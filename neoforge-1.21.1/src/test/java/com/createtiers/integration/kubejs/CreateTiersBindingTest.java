package com.createtiers.integration.kubejs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

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
}
