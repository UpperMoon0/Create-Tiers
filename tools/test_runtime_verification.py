import json
from pathlib import Path
import tempfile
import unittest

import runtime_verification as verification


class RuntimeVerificationTest(unittest.TestCase):
    def write_evidence(
        self,
        root: Path,
        target: str,
        head: str,
        scenarios=None,
        passed: bool = True,
    ):
        path = root / target / "result.json"
        path.parent.mkdir(parents=True, exist_ok=True)
        observed = list(
            verification.REQUIRED_SCENARIOS if scenarios is None else scenarios
        )
        payload = {
            "target": target,
            "commit": head,
            "passed": passed,
            "return_code": 0 if passed else 1,
            "observed_scenarios": observed,
        }
        path.write_text(json.dumps(payload) + "\n", encoding="utf-8")

    def test_matrix_covers_every_supported_target_and_required_scenario(self):
        cells = verification.matrix()["include"]
        self.assertEqual(set(verification.TARGETS), {cell["target"] for cell in cells})
        for cell in cells:
            self.assertEqual(list(verification.REQUIRED_SCENARIOS), cell["required_scenarios"])
            self.assertEqual(verification.TARGETS[cell["target"]], cell["gradle_task"])

    def test_gradle_command_uses_explicit_shell_on_posix(self):
        original_name = verification.os.name
        try:
            verification.os.name = "posix"
            self.assertEqual(
                ["bash", "./gradlew", ":example", "--stacktrace", "--no-daemon"],
                verification.gradle_command(":example"),
            )
            verification.os.name = "nt"
            self.assertEqual(
                ["gradlew.bat", ":example", "--stacktrace", "--no-daemon"],
                verification.gradle_command(":example"),
            )
        finally:
            verification.os.name = original_name

    def test_unknown_target_fails_closed(self):
        with self.assertRaisesRegex(ValueError, "unknown target"):
            verification.target_config("forge-9.99")

    def test_scenario_evidence_requires_every_declared_runtime_contract(self):
        observed = list(verification.REQUIRED_SCENARIOS)
        self.assertEqual(observed, verification.validate_scenario_evidence(observed))

        with self.assertRaisesRegex(ValueError, "missing="):
            verification.validate_scenario_evidence(observed[:-1])

        with self.assertRaisesRegex(ValueError, "extra="):
            verification.validate_scenario_evidence(observed + ["undeclared-scenario"])

    def test_valid_exact_head_receipts_require_runtime_evidence(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            directory = root / "receipts"
            evidence = root / "evidence"
            for target in verification.TARGETS:
                self.write_evidence(evidence, target, "abc123")
                verification.write_receipt(
                    directory, target, "abc123", evidence_root=evidence
                )
            verification.verify_receipts(directory, "abc123")

    def test_receipt_refuses_missing_scenario_evidence(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            target = next(iter(verification.TARGETS))
            self.write_evidence(
                root / "evidence",
                target,
                "abc123",
                scenarios=list(verification.REQUIRED_SCENARIOS[:-1]),
            )
            with self.assertRaisesRegex(ValueError, "missing="):
                verification.write_receipt(
                    root / "receipts",
                    target,
                    "abc123",
                    evidence_root=root / "evidence",
                )

    def test_receipt_refuses_stale_runtime_evidence(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            target = next(iter(verification.TARGETS))
            self.write_evidence(root / "evidence", target, "old-head")
            with self.assertRaisesRegex(ValueError, "head mismatch"):
                verification.write_receipt(
                    root / "receipts",
                    target,
                    "new-head",
                    evidence_root=root / "evidence",
                )

    def test_wrong_commit_receipt_fails(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            directory = root / "receipts"
            evidence = root / "evidence"
            for target in verification.TARGETS:
                self.write_evidence(evidence, target, "old-head")
                verification.write_receipt(
                    directory, target, "old-head", evidence_root=evidence
                )
            with self.assertRaisesRegex(ValueError, "stale or malformed"):
                verification.verify_receipts(directory, "new-head")

    def test_missing_receipt_fails(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            directory = root / "receipts"
            evidence = root / "evidence"
            first = next(iter(verification.TARGETS))
            self.write_evidence(evidence, first, "abc123")
            verification.write_receipt(
                directory, first, "abc123", evidence_root=evidence
            )
            with self.assertRaisesRegex(ValueError, "missing="):
                verification.verify_receipts(directory, "abc123")

    def test_extra_receipt_fails(self):
        with tempfile.TemporaryDirectory() as temp:
            root = Path(temp)
            directory = root / "receipts"
            evidence = root / "evidence"
            for target in verification.TARGETS:
                self.write_evidence(evidence, target, "abc123")
                verification.write_receipt(
                    directory, target, "abc123", evidence_root=evidence
                )
            (directory / "stale-target.pass").write_text(
                json.dumps({"head": "abc123"}) + "\n", encoding="utf-8"
            )
            with self.assertRaisesRegex(ValueError, "extra="):
                verification.verify_receipts(directory, "abc123")


if __name__ == "__main__":
    unittest.main()
