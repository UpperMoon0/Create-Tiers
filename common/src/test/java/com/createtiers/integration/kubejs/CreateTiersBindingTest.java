package com.createtiers.integration.kubejs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import com.createtiers.api.TierUpgradeRegistry;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateTiersBindingTest {

    @BeforeEach
    void setUp() {
        TierUpgradeRegistry.clear();
        TierRegistry.clear();
    }

    @AfterEach
    void tearDown() {
        TierUpgradeRegistry.clear();
        TierRegistry.clear();
    }

    @Test
    void malformedLaterBatchEntryDoesNotPartiallyRegisterEarlierEntries() {
        List<Map<String, Object>> tiers = List.of(
                Map.of("name", "basic", "maxRPM", 256, "maxSU", 1024),
                Map.of("name", "advanced", "maxRPM", 512, "maxSU", "bad"));

        assertThrows(IllegalArgumentException.class, () -> CreateTiersBinding.registerTiers(tiers));
        assertEquals(0, TierRegistry.size());
    }

    @Test
    void duplicateNamesInsideBatchAreRejectedAtomically() {
        List<Map<String, Object>> tiers = List.of(
                Map.of("name", "basic", "maxRPM", 256, "maxSU", 1024),
                Map.of("name", "basic", "maxRPM", 512, "maxSU", 2048));

        assertThrows(IllegalArgumentException.class, () -> CreateTiersBinding.registerTiers(tiers));
        assertEquals(0, TierRegistry.size());
    }

    @Test
    void crossedCapabilitiesInsideBatchAreRejectedAtomically() {
        List<Map<String, Object>> tiers = List.of(
                Map.of("name", "torque", "maxRPM", 256, "maxSU", 8192),
                Map.of("name", "speed", "maxRPM", 512, "maxSU", 4096));

        assertThrows(IllegalArgumentException.class, () -> CreateTiersBinding.registerTiers(tiers));
        assertEquals(0, TierRegistry.size());
    }

    @Test
    void fractionalNumericFieldsAreRejectedInsteadOfTruncated() {
        List<Map<String, Object>> tiers = List.of(
                Map.of("name", "basic", "maxRPM", 256.5d, "maxSU", 1024));

        assertThrows(IllegalArgumentException.class, () -> CreateTiersBinding.registerTiers(tiers));
        assertEquals(0, TierRegistry.size());
    }

    @Test
    void overflowingNumericFieldsAreRejectedInsteadOfWrapped() {
        List<Map<String, Object>> tiers = List.of(
                Map.of("name", "basic", "maxRPM", 2147483648L, "maxSU", 1024));

        assertThrows(IllegalArgumentException.class, () -> CreateTiersBinding.registerTiers(tiers));
        assertEquals(0, TierRegistry.size());
    }

    @Test
    void nullAndWrongTypedBatchFieldsFailWithoutMutation() {
        assertThrows(IllegalArgumentException.class, () -> CreateTiersBinding.registerTiers(null));

        Map<String, Object> wrongType = new HashMap<>();
        wrongType.put("name", "basic");
        wrongType.put("maxRPM", "256");
        wrongType.put("maxSU", 1024);
        assertThrows(IllegalArgumentException.class, () -> CreateTiersBinding.registerTiers(List.of(wrongType)));
        assertEquals(0, TierRegistry.size());
    }

    @Test
    void batchDefaultsColorsAndDisplayNameWithoutChangingExplicitValues() {
        CreateTiersBinding.registerTiers(List.of(
                Map.of("name", "basic", "maxRPM", 256, "maxSU", 1024, "shaftColor", 0x123456),
                Map.of("name", "advanced", "maxRPM", 512, "maxSU", 4096,
                        "shaftColor", 0xABCDEF, "cogwheelColor", 0x654321, "displayName", "Advanced Tier")));

        Tier basic = CreateTiersBinding.getTier("basic");
        Tier advanced = CreateTiersBinding.getTier("advanced");
        assertEquals(0x123456, basic.getShaftColor());
        assertEquals(0x123456, basic.getCogwheelColor());
        assertEquals("basic", basic.getDisplayName());
        assertEquals(0xABCDEF, advanced.getShaftColor());
        assertEquals(0x654321, advanced.getCogwheelColor());
        assertEquals("Advanced Tier", advanced.getDisplayName());
        assertEquals(2, TierRegistry.size());
    }

    @Test
    void directRegistrationOverloadsApplyDocumentedDefaults() {
        CreateTiersBinding.registerTier("basic", 256, 1024);
        CreateTiersBinding.registerTierStyled("advanced", 512, 4096, 0x334455, "advanced");

        Tier basic = CreateTiersBinding.getTier("basic");
        Tier advanced = CreateTiersBinding.getTier("advanced");
        assertEquals(0xFFFFFF, basic.getShaftColor());
        assertEquals(0xFFFFFF, basic.getCogwheelColor());
        assertEquals(0x334455, advanced.getShaftColor());
        assertEquals(0x334455, advanced.getCogwheelColor());
    }

    @Test
    void removedLevelSignaturesCannotBeSilentlyReinterpreted() {
        assertThrows(NoSuchMethodException.class, () -> CreateTiersBinding.class.getMethod(
                "registerTier", String.class, int.class, int.class, int.class));
        assertThrows(NoSuchMethodException.class, () -> CreateTiersBinding.class.getMethod(
                "registerCustomTier", String.class, String.class,
                int.class, int.class, int.class, int.class));
    }

    @Test
    void tierUpgradeRegistrationIsIndependentFromRecipeChoice() {
        CreateTiersBinding.registerTier("advanced", 512, 4096);
        CreateTiersBinding.registerTierUpgrade("create:large_water_wheel", "advanced", false);

        ResourceLocation item = ResourceLocation.tryParse("create:large_water_wheel");
        ResourceLocation tier = ResourceLocation.tryParse("createtiers:advanced");
        assertTrue(TierUpgradeRegistry.isRegistered(item, tier));
        assertFalse(TierUpgradeRegistry.get(item, tier).defaultRecipe());
    }

    @Test
    void tierUpgradeBatchDefaultsRecipeAndIsAtomic() {
        CreateTiersBinding.registerTier("basic", 256, 1024);
        CreateTiersBinding.registerTier("advanced", 512, 4096);

        assertThrows(IllegalArgumentException.class, () -> CreateTiersBinding.registerTierUpgrades(List.of(
                Map.of("item", "create:shaft", "tier", "basic"),
                Map.of("item", "create:shaft", "tier", "missing", "defaultRecipe", false))));
        assertEquals(0, TierUpgradeRegistry.size());

        CreateTiersBinding.registerTierUpgrades(List.of(
                Map.of("item", "create:shaft", "tier", "basic"),
                Map.of("item", "create:large_water_wheel", "tier", "advanced", "defaultRecipe", false)));

        assertTrue(TierUpgradeRegistry.get(
                ResourceLocation.tryParse("create:shaft"),
                ResourceLocation.tryParse("createtiers:basic")).defaultRecipe());
        assertFalse(TierUpgradeRegistry.get(
                ResourceLocation.tryParse("create:large_water_wheel"),
                ResourceLocation.tryParse("createtiers:advanced")).defaultRecipe());
    }
}
