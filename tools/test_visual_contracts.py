import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
VISUAL_POLICY = ROOT / "common/src/main/java/com/createtiers/client/AttachedTierVisuals.java"
ROTATING_MIXIN = ROOT / "common/src/main/java/com/createtiers/mixin/RotatingInstanceTierColorMixin.java"
RENDERER_MIXIN = ROOT / "common/src/main/java/com/createtiers/mixin/KineticBlockEntityRendererTierColorMixin.java"
ACCENT_MIXIN = ROOT / "common/src/main/java/com/createtiers/mixin/SafeBlockEntityRendererTierAccentMixin.java"
BELT_VISUAL_MIXIN = ROOT / "common/src/main/java/com/createtiers/mixin/BeltVisualMixin.java"
BELT_PULLEY_BODY = ROOT / "common/src/main/resources/assets/createtiers/models/block/belt_pulley_body.json"


class VisualContractTests(unittest.TestCase):
    def test_mixed_material_belt_pulley_is_not_whole_model_tinted(self):
        policy = VISUAL_POLICY.read_text(encoding="utf-8")
        self.assertIn("blockEntity instanceof BeltBlockEntity", policy)
        self.assertIn("getWholeRotatingModelColor", policy)

        rotating = ROTATING_MIXIN.read_text(encoding="utf-8")
        renderer = RENDERER_MIXIN.read_text(encoding="utf-8")

        # Scrolling belt material can still use the attached-tier color via colorFromBE.
        self.assertIn(
            "Color color = AttachedTierVisuals.getRenderedColor(blockEntity);",
            rotating,
        )

        # Generic whole rotating-model tinting must skip mixed-material belt pulleys.
        self.assertGreaterEqual(rotating.count("getWholeRotatingModelColor"), 3)
        self.assertIn("getWholeRotatingModelColor(blockEntity)", renderer)

    def test_belt_does_not_get_generic_square_accent(self):
        source = ACCENT_MIXIN.read_text(encoding="utf-8")
        self.assertIn("kinetic instanceof BeltBlockEntity", source)

    def test_tiered_belt_pulley_splits_wood_body_from_colored_shaft(self):
        model = json.loads(BELT_PULLEY_BODY.read_text(encoding="utf-8"))
        names = [element.get("name") for element in model["elements"]]
        self.assertNotIn("Axis", names)
        self.assertTrue(names)
        self.assertTrue(all(name == "Pulley" for name in names))
        self.assertEqual("block/dark_oak_log", model["textures"]["2"])
        self.assertEqual("block/dark_oak_log_top", model["textures"]["3"])

        source = BELT_VISUAL_MIXIN.read_text(encoding="utf-8")
        self.assertIn("BELT_PULLEY_BODY", source)
        self.assertIn("Models.partial(partials.SHAFT)", source)
        self.assertIn("setColor(color)", source)
        self.assertIn("pulley.delete()", source)


if __name__ == "__main__":
    unittest.main()
