import json
from pathlib import Path
import tempfile
import unittest

import runtime_verification as verification


class RuntimeVerificationTest(unittest.TestCase):
    def test_matrix_covers_every_supported_target_and_required_scenario(self):
        cells = verification.matrix()["include"]
        self.assertEqual(set(verification.TARGETS), {cell["target"] for cell in cells})
        for cell in cells:
            self.assertEqual(list(verification.REQUIRED_SCENARIOS), cell["required_scenarios"])
            self.assertEqual(verification.TARGETS[cell["target"]], cell["gradle_task"])

    def test_unknown_target_fails_closed(self):
        with self.assertRaisesRegex(ValueError, "unknown target"):
            verification.target_config("forge-9.99")

    def test_valid_exact_head_receipts_pass(self):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            for target in verification.TARGETS:
                verification.write_receipt(directory, target, "abc123")
            verification.verify_receipts(directory, "abc123")

    def test_wrong_commit_receipt_fails(self):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            for target in verification.TARGETS:
                verification.write_receipt(directory, target, "old-head")
            with self.assertRaisesRegex(ValueError, "stale or malformed"):
                verification.verify_receipts(directory, "new-head")

    def test_missing_receipt_fails(self):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            first = next(iter(verification.TARGETS))
            verification.write_receipt(directory, first, "abc123")
            with self.assertRaisesRegex(ValueError, "missing="):
                verification.verify_receipts(directory, "abc123")

    def test_extra_receipt_fails(self):
        with tempfile.TemporaryDirectory() as temp:
            directory = Path(temp)
            for target in verification.TARGETS:
                verification.write_receipt(directory, target, "abc123")
            (directory / "stale-target.pass").write_text(
                json.dumps({"head": "abc123"}) + "\n", encoding="utf-8"
            )
            with self.assertRaisesRegex(ValueError, "extra="):
                verification.verify_receipts(directory, "abc123")


if __name__ == "__main__":
    unittest.main()
