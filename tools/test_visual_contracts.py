import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VISUAL_POLICY = ROOT / "common/src/main/java/com/createtiers/client/AttachedTierVisuals.java"
ROTATING_MIXIN = ROOT / "common/src/main/java/com/createtiers/mixin/RotatingInstanceTierColorMixin.java"
RENDERER_MIXIN = ROOT / "common/src/main/java/com/createtiers/mixin/KineticBlockEntityRendererTierColorMixin.java"


class VisualContractTests(unittest.TestCase):
    def test_mixed_material_belt_pulley_is_not_whole_model_tinted(self):
        policy = VISUAL_POLICY.read_text(encoding="utf-8")
        self.assertIn("blockEntity instanceof BeltBlockEntity", policy)
        self.assertIn("getWholeRotatingModelColor", policy)

        rotating = ROTATING_MIXIN.read_text(encoding="utf-8")
        renderer = RENDERER_MIXIN.read_text(encoding="utf-8")

        # Scrolling belt material still gets the attached-tier color via colorFromBE.
        self.assertIn(
            "Color color = AttachedTierVisuals.getRenderedColor(blockEntity);",
            rotating,
        )

        # Rotating mixed-material models use the guarded policy on both Flywheel and BER paths.
        self.assertGreaterEqual(rotating.count("getWholeRotatingModelColor"), 3)
        self.assertIn("getWholeRotatingModelColor(blockEntity)", renderer)


if __name__ == "__main__":
    unittest.main()
