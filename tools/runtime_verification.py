"""Runtime verification harness for Create Tiers."""
from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import re
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
    "attached-tier-apply-clear",
    "attached-tier-nbt-persistence",
    "attached-tier-network-rebuild",
    "tier-upgrade-item-roundtrip",
    "shaft-cannot-bypass-item-recipe",
    "rotation-speed-controller",
    "native-speed-controller-large-cog",
    "creative-motor",
    "native-relay-default-family",
    "tiered-shaft-belt",
    "tiered-shaft-steam-engine",
    "registered-shaft-belt-tier-preservation",
    "registered-shaft-steam-tier-preservation",
    "registered-kinetic-encasing-tier-preservation",
    "native-axe-or-pickaxe-parity",
    "tier-upgrade-creative-tab-entry",
)

SCENARIO_MARKER = "CREATE_TIERS_SCENARIO_PASS:"
SCENARIO_PATTERN = re.compile(r"CREATE_TIERS_SCENARIO_PASS:([a-z0-9-]+)")


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


def validate_scenario_evidence(observed: list[str] | set[str] | tuple[str, ...]) -> list[str]:
    observed_set = set(observed)
    required_set = set(REQUIRED_SCENARIOS)
    missing = sorted(required_set - observed_set)
    extra = sorted(observed_set - required_set)
    if missing or extra:
        raise ValueError(
            f"runtime scenario evidence mismatch: missing={missing}, extra={extra}"
        )
    return [scenario for scenario in REQUIRED_SCENARIOS if scenario in observed_set]


def run_target(target: str, expected_head: str | None = None) -> int:
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
    observed_scenarios: set[str] = set()

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
            match = SCENARIO_PATTERN.search(line)
            if match:
                observed_scenarios.add(match.group(1))
        process_return_code = process.wait()

    verification_error: str | None = None
    effective_return_code = process_return_code
    observed_ordered = [
        scenario for scenario in REQUIRED_SCENARIOS if scenario in observed_scenarios
    ]
    missing_scenarios = sorted(set(REQUIRED_SCENARIOS) - observed_scenarios)
    extra_scenarios = sorted(observed_scenarios - set(REQUIRED_SCENARIOS))

    if process_return_code == 0:
        try:
            observed_ordered = validate_scenario_evidence(observed_scenarios)
        except ValueError as exc:
            verification_error = str(exc)
            effective_return_code = 2
            print(verification_error, file=sys.stderr)

    result = {
        "target": target,
        "task": config["gradle_task"],
        "required_scenarios": list(REQUIRED_SCENARIOS),
        "observed_scenarios": observed_ordered,
        "missing_scenarios": missing_scenarios,
        "extra_scenarios": extra_scenarios,
        "commit": actual_head,
        "dirty": dirty,
        "elapsed_seconds": round(time.monotonic() - started, 3),
        "passed": effective_return_code == 0,
        "process_return_code": process_return_code,
        "return_code": effective_return_code,
        "verification_error": verification_error,
    }
    result_path.write_text(
        json.dumps(result, indent=2, sort_keys=True) + "\n",
        encoding="utf-8",
    )
    return effective_return_code


def receipt_payload(target: str, head: str, executed_scenarios: list[str] | None = None) -> dict[str, object]:
    config = target_config(target)
    executed = list(REQUIRED_SCENARIOS) if executed_scenarios is None else list(executed_scenarios)
    return {
        "target": target,
        "head": head,
        "gradle_task": config["gradle_task"],
        "required_scenarios": list(REQUIRED_SCENARIOS),
        "executed_scenarios": executed,
    }


def load_runtime_evidence(
    target: str,
    head: str,
    evidence_root: Path = Path("build/runtime-evidence"),
) -> list[str]:
    result_path = evidence_root / target / "result.json"
    try:
        payload = json.loads(result_path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ValueError(f"missing or invalid runtime evidence {result_path}") from exc

    if payload.get("target") != target:
        raise ValueError(
            f"runtime evidence target mismatch: expected {target}, got {payload.get('target')}"
        )
    if payload.get("commit") != head:
        raise ValueError(
            f"runtime evidence head mismatch: expected {head}, got {payload.get('commit')}"
        )
    if payload.get("passed") is not True or payload.get("return_code") != 0:
        raise ValueError(f"runtime evidence is not a passing run: {result_path}")

    observed = payload.get("observed_scenarios")
    if not isinstance(observed, list) or not all(isinstance(item, str) for item in observed):
        raise ValueError(f"runtime evidence has invalid observed_scenarios: {result_path}")
    return validate_scenario_evidence(observed)


def write_receipt(
    directory: Path,
    target: str,
    head: str,
    evidence_root: Path = Path("build/runtime-evidence"),
) -> Path:
    executed = load_runtime_evidence(target, head, evidence_root)
    directory.mkdir(parents=True, exist_ok=True)
    path = directory / f"{target}.pass"
    path.write_text(
        json.dumps(receipt_payload(target, head, executed), sort_keys=True) + "\n",
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
        expected = receipt_payload(target, head, list(REQUIRED_SCENARIOS))
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
        try:
            write_receipt(args.write_receipt, target, args.head)
        except ValueError as exc:
            print(str(exc), file=sys.stderr)
            return 1
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
