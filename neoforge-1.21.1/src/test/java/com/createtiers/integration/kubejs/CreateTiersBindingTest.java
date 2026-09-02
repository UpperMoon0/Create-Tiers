package com.createtiers.integration.kubejs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreateTiersBindingTest {

    @BeforeEach
    void setUp() {
        TierRegistry.clear();
    }

    @AfterEach
    void tearDown() {
        TierRegistry.clear();
    }

    @Test
    void malformedLaterBatchEntryDoesNotPartiallyRegisterEarlierEntries() {
        List<Map<String, Object>> tiers = List.of(
                Map.of("name", "basic", "level", 1, "maxRPM", 256, "maxSU", 1024),
                Map.of("name", "advanced", "level", 1, "maxRPM", 512, "maxSU", 2048));

        assertThrows(IllegalArgumentException.class, () -> CreateTiersBinding.registerTiers(tiers));
        assertEquals(0, TierRegistry.size());
    }

    @Test
    void duplicateNamesInsideBatchAreRejectedAtomically() {
        List<Map<String, Object>> tiers = List.of(
                Map.of("name", "basic", "level", 1, "maxRPM", 256, "maxSU", 1024),
                Map.of("name", "basic", "level", 2, "maxRPM", 512, "maxSU", 2048));

        assertThrows(IllegalArgumentException.class, () -> CreateTiersBinding.registerTiers(tiers));
        assertEquals(0, TierRegistry.size());
    }

    @Test
    void fractionalNumericFieldsAreRejectedInsteadOfTruncated() {
        List<Map<String, Object>> tiers = List.of(
                Map.of("name", "basic", "level", 1.5d, "maxRPM", 256, "maxSU", 1024));

        assertThrows(IllegalArgumentException.class, () -> CreateTiersBinding.registerTiers(tiers));
        assertEquals(0, TierRegistry.size());
    }

    @Test
    void overflowingNumericFieldsAreRejectedInsteadOfWrapped() {
        List<Map<String, Object>> tiers = List.of(
                Map.of("name", "basic", "level", 1, "maxRPM", 2147483648L, "maxSU", 1024));

        assertThrows(IllegalArgumentException.class, () -> CreateTiersBinding.registerTiers(tiers));
        assertEquals(0, TierRegistry.size());
    }

    @Test
    void nullAndWrongTypedBatchFieldsFailWithoutMutation() {
        assertThrows(IllegalArgumentException.class, () -> CreateTiersBinding.registerTiers(null));

        Map<String, Object> wrongType = new HashMap<>();
        wrongType.put("name", "basic");
        wrongType.put("level", "1");
        wrongType.put("maxRPM", 256);
        wrongType.put("maxSU", 1024);
        assertThrows(IllegalArgumentException.class, () -> CreateTiersBinding.registerTiers(List.of(wrongType)));
        assertEquals(0, TierRegistry.size());
    }

    @Test
    void batchDefaultsColorsAndDisplayNameWithoutChangingExplicitValues() {
        CreateTiersBinding.registerTiers(List.of(
                Map.of("name", "basic", "level", 1, "maxRPM", 256, "maxSU", 1024, "shaftColor", 0x123456),
                Map.of("name", "advanced", "level", 2, "maxRPM", 512, "maxSU", 4096,
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
        CreateTiersBinding.registerTier("basic", 1, 256, 1024);
        CreateTiersBinding.registerTier("advanced", 2, 512, 4096, 0x334455);

        Tier basic = CreateTiersBinding.getTier("basic");
        Tier advanced = CreateTiersBinding.getTier("advanced");
        assertEquals(0xFFFFFF, basic.getShaftColor());
        assertEquals(0xFFFFFF, basic.getCogwheelColor());
        assertEquals(0x334455, advanced.getShaftColor());
        assertEquals(0x334455, advanced.getCogwheelColor());
    }
}
