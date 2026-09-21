import pathlib
import re
import unittest


ROOT = pathlib.Path(__file__).resolve().parents[1]
README = (ROOT / "README.md").read_text(encoding="utf-8")
STARTUP_EXAMPLE = (ROOT / "kubejs/startup_scripts/example.js").read_text(encoding="utf-8")
SERVER_EXAMPLE = (ROOT / "kubejs/server_scripts/example_recipes.js").read_text(encoding="utf-8")
BINDING = (ROOT / "common/src/main/java/com/createtiers/integration/kubejs/CreateTiersBinding.java").read_text(encoding="utf-8")


class PackAuthorDocumentationContracts(unittest.TestCase):
    def test_supported_versions_and_dependencies_are_prominent(self):
        self.assertIn("Create 6.0.8", README)
        self.assertIn("Create 6.0.11", README)
        self.assertIn("KubeJS is the supported script configuration route", README)
        self.assertIn("Jade is optional", README)

    def test_startup_registration_order_is_explicit_and_example_matches(self):
        self.assertIn("must run before", README)
        self.assertLess(
            STARTUP_EXAMPLE.index("CreateTiers.registerTiers("),
            STARTUP_EXAMPLE.index("CreateTiers.registerTierUpgrades("),
        )

    def test_deployment_and_restart_contract_is_documented(self):
        self.assertIn("kubejs/startup_scripts", README)
        self.assertIn("full game/server restart", README)
        self.assertIn("modpack client and dedicated server", README)
        self.assertIn("kubejs/server_scripts", README)

    def test_direct_targets_are_distinguished_from_compatibility_states(self):
        self.assertIn("Direct upgrade targets vs compatibility states", README)
        self.assertIn("belt segments do not have a normal block item", README)
        self.assertIn("Steam Engine itself is **not** a valid upgrade target", README)
        self.assertIn("that block entity is a Create `KineticBlockEntity`", README)

    def test_recipe_progression_and_data_semantics_are_explicit(self):
        self.assertIn("does **not** enforce a Basic -> Advanced -> Elite chain", README)
        self.assertIn("defaultRecipe: false", README)
        self.assertIn("built-in Create Tiers fallback recipe", README)
        self.assertIn("starts from a **new `ItemStack`", README)
        self.assertIn("does **not** automatically copy arbitrary NBT/components", README)
        self.assertIn("fresh output stack", SERVER_EXAMPLE)

    def test_documented_api_tracks_actual_public_binding_methods(self):
        actual = re.findall(r"public static [^{;]+?\s+(\w+)\s*\(", BINDING, flags=re.DOTALL)
        expected_counts = {
            "registerTiers": 1,
            "registerTier": 1,
            "registerTierStyled": 2,
            "registerCustomTier": 1,
            "registerCustomTierStyled": 2,
            "registerTierUpgrade": 2,
            "registerTierUpgrades": 1,
            "tieredItem": 1,
            "getTier": 1,
            "getAllTiers": 1,
            "tierExists": 1,
        }
        self.assertEqual(set(expected_counts), set(actual))
        for method, count in expected_counts.items():
            with self.subTest(method=method):
                self.assertEqual(count, actual.count(method))
                self.assertIn(method + "(", README)
    def test_kubejs_api_reference_covers_public_binding_surface(self):
        signatures = [
            "registerTier(name, maxRPM, maxSU)",
            "registerTierStyled(name, maxRPM, maxSU, color, displayName)",
            "registerTierStyled(name, maxRPM, maxSU, shaftColor, cogwheelColor, displayName)",
            "registerCustomTier(namespace, name, maxRPM, maxSU)",
            "registerCustomTierStyled(namespace, name, maxRPM, maxSU, color, displayName)",
            "registerCustomTierStyled(namespace, name, maxRPM, maxSU, shaftColor, cogwheelColor, displayName)",
            "registerTiers([{ name, maxRPM, maxSU, shaftColor?, cogwheelColor?, displayName? }, ...])",
            "registerTierUpgrade(item, tier)",
            "registerTierUpgrade(item, tier, defaultRecipe)",
            "registerTierUpgrades([{ item, tier, defaultRecipe? }, ...])",
            "tieredItem(item, tier)",
            "getTier(name)",
            "getAllTiers()",
            "tierExists(name)",
        ]
        for signature in signatures:
            with self.subTest(signature=signature):
                self.assertIn(signature, README)

        self.assertIn("positive whole 32-bit integers", README)
        self.assertIn("0x000000", README)
        self.assertIn("0xFFFFFF", README)
        self.assertIn("valid lowercase Minecraft resource paths", README)


if __name__ == "__main__":
    unittest.main()
