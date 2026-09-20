package com.createtiers.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TierUpgradeRegistryTest {

    private static ResourceLocation id(String value) {
        ResourceLocation result = ResourceLocation.tryParse(value);
        if (result == null) throw new AssertionError("Invalid test id: " + value);
        return result;
    }

    @BeforeEach
    void setUp() {
        TierUpgradeRegistry.clear();
        TierRegistry.clear();
        TierRegistry.register(id("createtiers:basic"), new Tier(1, "basic", 256, 1024));
        TierRegistry.register(id("createtiers:advanced"), new Tier(2, "advanced", 512, 4096));
    }

    @AfterEach
    void tearDown() {
        TierUpgradeRegistry.clear();
        TierRegistry.clear();
    }

    @Test
    void sameItemCanExposeMultipleRegisteredTiersWithIndependentRecipePolicy() {
        ResourceLocation item = id("create:large_water_wheel");
        TierUpgradeRegistry.register(item, id("createtiers:basic"), true);
        TierUpgradeRegistry.register(item, id("createtiers:advanced"), false);

        assertTrue(TierUpgradeRegistry.isRegistered(item, id("createtiers:basic")));
        assertTrue(TierUpgradeRegistry.isRegistered(item, id("createtiers:advanced")));
        assertTrue(TierUpgradeRegistry.get(item, id("createtiers:basic")).defaultRecipe());
        assertFalse(TierUpgradeRegistry.get(item, id("createtiers:advanced")).defaultRecipe());
    }

    @Test
    void invalidLaterEntryDoesNotPartiallyCommitBatch() {
        ResourceLocation item = id("create:large_water_wheel");
        assertThrows(IllegalArgumentException.class, () -> TierUpgradeRegistry.registerAll(List.of(
                new TierUpgradeRegistry.Registration(item, id("createtiers:basic"), true),
                new TierUpgradeRegistry.Registration(item, id("createtiers:missing"), false))));

        assertEquals(0, TierUpgradeRegistry.size());
    }

    @Test
    void duplicatePairAndFrozenMutationAreRejected() {
        ResourceLocation item = id("create:large_water_wheel");
        TierUpgradeRegistry.register(item, id("createtiers:basic"), true);
        assertThrows(IllegalArgumentException.class,
                () -> TierUpgradeRegistry.register(item, id("createtiers:basic"), false));

        TierUpgradeRegistry.freeze();
        assertThrows(IllegalStateException.class,
                () -> TierUpgradeRegistry.register(item, id("createtiers:advanced"), false));
        assertEquals(1, TierUpgradeRegistry.size());
    }
}
