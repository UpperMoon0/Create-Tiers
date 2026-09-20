import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PLUGIN_SOURCE = ROOT / "common/src/main/java/com/createtiers/integration/jade/CreateTiersJadePlugin.java"
COMMON_LANG = ROOT / "common/src/main/resources/assets/createtiers/lang/en_us.json"
NEO_LOCAL_LANG = ROOT / "neoforge-1.21.1/src/main/resources/assets/createtiers/lang/en_us.json"
NEO_BUILD = ROOT / "neoforge-1.21.1/build.gradle"
JADE_CONFIG_KEY = "config.jade.plugin_createtiers.tier_info"
FORGE_MODEL_GENERATOR = ROOT / "forge-1.20.1/src/main/java/com/createtiers/client/TieredModelGenerator.java"
NEO_MODEL_GENERATOR = ROOT / "neoforge-1.21.1/src/main/java/com/createtiers/client/TieredModelGenerator.java"
ENCASING_DISCOVERY = ROOT / "common/src/main/java/com/createtiers/registry/CreateEncasingVariants.java"
FORGE_SERVER_PACK = ROOT / "common/src/main/java/com/createtiers/data/DynamicServerPack.java"
NEO_SERVER_PACK = ROOT / "neoforge-1.21.1/src/main/java/com/createtiers/data/DynamicServerPack.java"
CREATIVE_TAB = ROOT / "common/src/main/java/com/createtiers/registry/CommonCreativeTab.java"


class ResourceContractTests(unittest.TestCase):
    def test_jade_provider_has_config_translation(self):
        plugin_source = PLUGIN_SOURCE.read_text(encoding="utf-8")
        self.assertIn('CreateTiers.asResource("tier_info")', plugin_source)

        data = json.loads(COMMON_LANG.read_text(encoding="utf-8"))
        self.assertEqual("Create Tiers", data["config.jade.plugin_createtiers"])
        self.assertEqual("Tier Information", data[JADE_CONFIG_KEY])

    def test_shared_language_resource_is_packaged_by_neoforge(self):
        self.assertFalse(
            NEO_LOCAL_LANG.exists(),
            "NeoForge must not shadow the shared language file with a duplicate copy",
        )
        build_script = NEO_BUILD.read_text(encoding="utf-8")
        self.assertNotIn(
            "exclude 'assets/createtiers/lang/**'",
            build_script,
            "NeoForge must package the shared language resources",
        )

    def test_shared_language_contains_creative_tab_title(self):
        data = json.loads(COMMON_LANG.read_text(encoding="utf-8"))
        self.assertEqual("Create Tiers", data["itemGroup.createtiers"])

    def test_native_relay_assets_inherit_create_models_on_both_targets(self):
        for path in (FORGE_MODEL_GENERATOR, NEO_MODEL_GENERATOR):
            source = path.read_text(encoding="utf-8")
            self.assertIn("generateNativeRelayAssets", source)
            self.assertIn("TieredNativeKineticBlock", source)
            self.assertIn('"blockstates/" + baseId.getPath() + ".json"', source)
            self.assertIn('"models/item/" + baseId.getPath() + ".json"', source)

    def test_creative_tab_exposes_registered_upgrades_without_encased_variant_clutter(self):
        source = CREATIVE_TAB.read_text(encoding="utf-8")
        self.assertIn("tierUpgradeEntries().forEach(output::accept)", source)
        self.assertIn("TierUpgradeRegistry.getAll()", source)
        self.assertIn("CalibratedItemData.calibratedCopy", source)

        self.assertNotIn("ModBlocks.ENCASED_SHAFT_ITEMS", source)
        self.assertNotIn("ModBlocks.ENCASED_COGWHEEL_ITEMS", source)
        self.assertNotIn("ModBlocks.ENCASED_LARGE_COGWHEEL_ITEMS", source)

    def test_standard_create_encasings_are_discovered_not_hardcoded_in_registration(self):
        discovery = ENCASING_DISCOVERY.read_text(encoding="utf-8")
        self.assertIn("EncasingRegistry.getVariants(base)", discovery)
        self.assertIn("for (Block block : BuiltInRegistries.BLOCK)", discovery)
        self.assertIn('"create".equals(id.getNamespace())', discovery)
        for path in (FORGE_MODEL_GENERATOR, NEO_MODEL_GENERATOR, FORGE_SERVER_PACK, NEO_SERVER_PACK):
            source = path.read_text(encoding="utf-8")
            self.assertIn("CreateEncasingVariants.shaftVariants()", source)
            self.assertIn("CreateEncasingVariants.cogwheelVariants()", source)
            self.assertIn("CreateEncasingVariants.largeCogwheelVariants()", source)


if __name__ == "__main__":
    unittest.main()
