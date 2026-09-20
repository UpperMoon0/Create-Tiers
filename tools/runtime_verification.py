"""Small runtime verification harness for Create Tiers."""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import subprocess
import sys
import time

TARGETS = {
    "forge-1.20.1": ":forge-1.20.1:runGameTestServer",
    "neoforge-1.21.1": ":neoforge-1.21.1:runGameTestServer",
}

REQUIRED_SCENARIOS = (
    "tiered-to-tiered-propagation",
    "high-tier-to-create-receiver",
    "overspeed-rejection",
    "lowest-tier-max-su",
    "calibration-apply-clear",
    "calibration-nbt-persistence",
    "calibration-network-rebuild",
    "calibration-recipe-item-roundtrip",
    "shaft-cannot-bypass-item-recipe",
    "rotation-speed-controller",
    "creative-motor",
    "tiered-shaft-belt",
    "tiered-shaft-steam-engine",
)


def target_config(target: str) -> dict[str, object]:
    try:
        task = TARGETS[target]
    except KeyError as exc:
        raise ValueError(f"unknown target: {target}") from exc
    return {
        "target": target,
        "gradle_task": task,
        "required_scenarios": list(REQUIRED_SCENARIOS),
    }


def matrix() -> dict[str, list[dict[str, object]]]:
    return {"include": [target_config(target) for target in TARGETS]}


def git_output(*args: str) -> str:
    return subprocess.check_output(["git", *args], text=True).strip()


def checkout_state() -> tuple[str, bool]:
    return git_output("rev-parse", "HEAD"), bool(git_output("status", "--porcelain"))


def gradle_command(task: str) -> list[str]:
    if os.name == "nt":
        return ["gradlew.bat", task, "--stacktrace", "--no-daemon"]
    return ["bash", "./gradlew", task, "--stacktrace", "--no-daemon"]


def run_target(target: str, expected_head: str | None = None) -> int:
    # Keep diagnostic streaming from turning a successful runtime test into a
    # Windows code-page failure when mods log Unicode characters.
    if hasattr(sys.stdout, "reconfigure"):
        sys.stdout.reconfigure(errors="replace")

    config = target_config(target)
    actual_head, dirty = checkout_state()
    if expected_head and actual_head != expected_head:
        raise SystemExit(
            f"checkout head mismatch: expected {expected_head}, found {actual_head}"
        )

    evidence = Path("build") / "runtime-evidence" / target
    evidence.mkdir(parents=True, exist_ok=True)
    process_log = evidence / "process.log"
    result_path = evidence / "result.json"
    command = gradle_command(str(config["gradle_task"]))
    started = time.monotonic()

    with process_log.open("w", encoding="utf-8", errors="replace") as log:
        log.write("$ " + " ".join(command) + "\n")
        log.flush()
        process = subprocess.Popen(
            command,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
            encoding="utf-8",
            errors="replace",
            bufsize=1,
        )
        assert process.stdout is not None
        for line in process.stdout:
            sys.stdout.write(line)
            log.write(line)
        return_code = process.wait()

    result = {
        "target": target,
        "task": config["gradle_task"],
        "required_scenarios": list(REQUIRED_SCENARIOS),
        "commit": actual_head,
        "dirty": dirty,
        "elapsed_seconds": round(time.monotonic() - started, 3),
        "passed": return_code == 0,
        "return_code": return_code,
    }
    result_path.write_text(json.dumps(result, indent=2, sort_keys=True) + "\n", encoding="utf-8")
    return return_code


def receipt_payload(target: str, head: str) -> dict[str, object]:
    config = target_config(target)
    return {
        "target": target,
        "head": head,
        "gradle_task": config["gradle_task"],
        "required_scenarios": list(REQUIRED_SCENARIOS),
    }


def write_receipt(directory: Path, target: str, head: str) -> Path:
    directory.mkdir(parents=True, exist_ok=True)
    path = directory / f"{target}.pass"
    path.write_text(
        json.dumps(receipt_payload(target, head), sort_keys=True) + "\n",
        encoding="utf-8",
    )
    return path


def verify_receipts(directory: Path, head: str) -> None:
    expected_names = {f"{target}.pass" for target in TARGETS}
    actual_names = {path.name for path in directory.glob("*.pass")}
    missing = sorted(expected_names - actual_names)
    extra = sorted(actual_names - expected_names)
    if missing or extra:
        raise ValueError(f"receipt set mismatch: missing={missing}, extra={extra}")

    for target in TARGETS:
        path = directory / f"{target}.pass"
        try:
            payload = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            raise ValueError(f"invalid receipt {path}") from exc
        expected = receipt_payload(target, head)
        if payload != expected:
            raise ValueError(
                f"stale or malformed receipt {path}: expected {expected}, got {payload}"
            )


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--matrix", action="store_true")
    parser.add_argument("--target")
    parser.add_argument("--head")
    parser.add_argument("--write-receipt", type=Path)
    parser.add_argument("--verify-receipts", type=Path)
    args = parser.parse_args()

    modes = sum(
        bool(value)
        for value in (args.matrix, args.target, args.write_receipt, args.verify_receipts)
    )
    if modes != 1:
        parser.error("choose exactly one operation")

    if args.matrix:
        print(json.dumps(matrix(), separators=(",", ":")))
        return 0
    if args.target:
        return run_target(args.target, args.head)
    if args.write_receipt:
        if not args.head:
            parser.error("--write-receipt requires --head")
        target = os.environ.get("CREATE_TIERS_TARGET")
        if not target:
            parser.error("--write-receipt requires CREATE_TIERS_TARGET")
        write_receipt(args.write_receipt, target, args.head)
        return 0
    if args.verify_receipts:
        if not args.head:
            parser.error("--verify-receipts requires --head")
        try:
            verify_receipts(args.verify_receipts, args.head)
        except ValueError as exc:
            print(str(exc), file=sys.stderr)
            return 1
        return 0
    raise AssertionError("unreachable")


if __name__ == "__main__":
    raise SystemExit(main())
