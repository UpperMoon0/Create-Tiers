package com.createtiers.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TierRegistryTest {

    @BeforeEach
    void setUp() {
        TierRegistry.clear();
    }

    @AfterEach
    void tearDown() {
        TierRegistry.clear();
    }

    @Test
    void duplicateIdIsRejectedWithoutMutatingLevelLookup() {
        Tier first = new Tier(1, "basic", 256, 1024);
        Tier second = new Tier(2, "advanced", 512, 2048);
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("createtiers", "basic");

        TierRegistry.register(id, first);
        assertThrows(IllegalArgumentException.class, () -> TierRegistry.register(id, second));

        assertEquals(first, TierRegistry.get(id));
        assertEquals(first, TierRegistry.getByLevel(1));
        assertEquals(1, TierRegistry.size());
    }

    @Test
    void duplicateLevelIsRejected() {
        TierRegistry.register(ResourceLocation.fromNamespaceAndPath("createtiers", "basic"),
                new Tier(1, "basic", 256, 1024));

        assertThrows(IllegalArgumentException.class, () -> TierRegistry.register(
                ResourceLocation.fromNamespaceAndPath("othermod", "advanced"),
                new Tier(1, "advanced", 512, 2048)));
        assertEquals(1, TierRegistry.size());
    }

    @Test
    void generatedNamesMustBeUniqueAcrossNamespaces() {
        TierRegistry.register(ResourceLocation.fromNamespaceAndPath("pack_a", "steel"),
                new Tier(1, "steel", 256, 1024));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> TierRegistry.register(
                ResourceLocation.fromNamespaceAndPath("pack_b", "steel"),
                new Tier(2, "steel", 512, 2048)));
        assertTrue(error.getMessage().contains("Generated component names must be unique"));
    }

    @Test
    void invalidLimitsLevelsAndColorsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> TierRegistry.register(
                ResourceLocation.fromNamespaceAndPath("createtiers", "bad_level"),
                new Tier(0, "bad_level", 256, 1024)));
        assertThrows(IllegalArgumentException.class, () -> TierRegistry.register(
                ResourceLocation.fromNamespaceAndPath("createtiers", "bad_rpm"),
                new Tier(1, "bad_rpm", 0, 1024)));
        assertThrows(IllegalArgumentException.class, () -> TierRegistry.register(
                ResourceLocation.fromNamespaceAndPath("createtiers", "bad_su"),
                new Tier(1, "bad_su", 256, 0)));
        assertThrows(IllegalArgumentException.class, () -> TierRegistry.register(
                ResourceLocation.fromNamespaceAndPath("createtiers", "bad_shaft_color"),
                new Tier(1, "bad_shaft_color", 256, 1024, -1, 0xFFFFFF, null)));
        assertThrows(IllegalArgumentException.class, () -> TierRegistry.register(
                ResourceLocation.fromNamespaceAndPath("createtiers", "bad_cog_color"),
                new Tier(1, "bad_cog_color", 256, 1024, 0xFFFFFF, 0x1000000, null)));
        assertEquals(0, TierRegistry.size());
    }

    @Test
    void invalidGeneratedResourceNameIsRejectedAtRegistration() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> TierRegistry.register(
                ResourceLocation.fromNamespaceAndPath("createtiers", "safe_lookup_id"),
                new Tier(1, "Bad Name", 256, 1024)));

        assertTrue(error.getMessage().contains("invalid in Minecraft resource paths"));
        assertEquals(0, TierRegistry.size());
    }

    @Test
    void batchRegistrationIsAtomicWhenLaterEntryConflicts() {
        ResourceLocation basicId = ResourceLocation.fromNamespaceAndPath("createtiers", "basic");
        ResourceLocation advancedId = ResourceLocation.fromNamespaceAndPath("createtiers", "advanced");
        Map<ResourceLocation, Tier> registrations = new LinkedHashMap<>();
        registrations.put(basicId, new Tier(1, "basic", 256, 1024));
        registrations.put(advancedId, new Tier(1, "advanced", 512, 2048));

        assertThrows(IllegalArgumentException.class, () -> TierRegistry.registerAll(registrations));

        assertEquals(0, TierRegistry.size());
        assertFalse(TierRegistry.exists(basicId));
        assertFalse(TierRegistry.exists(advancedId));
    }

    @Test
    void validBatchCommitsAllTiersAndReadsBackInLevelOrder() {
        ResourceLocation highId = ResourceLocation.fromNamespaceAndPath("createtiers", "high");
        ResourceLocation lowId = ResourceLocation.fromNamespaceAndPath("createtiers", "low");
        Tier high = new Tier(2, "high", 512, 4096);
        Tier low = new Tier(1, "low", 256, 1024);
        Map<ResourceLocation, Tier> registrations = new LinkedHashMap<>();
        registrations.put(highId, high);
        registrations.put(lowId, low);

        assertEquals(List.of(high, low), TierRegistry.registerAll(registrations));
        assertEquals(List.of(low, high), List.copyOf(TierRegistry.getAllTiers()));
        assertEquals(high, TierRegistry.get(highId));
        assertEquals(low, TierRegistry.getByLevel(1));
    }

    @Test
    void frozenRegistryRejectsSingleAndBatchMutationsWithoutChangingContents() {
        ResourceLocation basicId = ResourceLocation.fromNamespaceAndPath("createtiers", "basic");
        Tier basic = new Tier(1, "basic", 256, 1024);
        TierRegistry.register(basicId, basic);
        TierRegistry.freeze();

        assertThrows(IllegalStateException.class, () -> TierRegistry.register(
                ResourceLocation.fromNamespaceAndPath("createtiers", "advanced"),
                new Tier(2, "advanced", 512, 2048)));
        assertThrows(IllegalStateException.class, () -> TierRegistry.registerAll(Map.of(
                ResourceLocation.fromNamespaceAndPath("createtiers", "elite"),
                new Tier(3, "elite", 1024, 4096))));

        assertTrue(TierRegistry.isFrozen());
        assertEquals(1, TierRegistry.size());
        assertEquals(basic, TierRegistry.get(basicId));
    }
}
