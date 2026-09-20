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

    private static ResourceLocation id(String namespace, String path) {
        ResourceLocation value = ResourceLocation.tryParse(namespace + ":" + path);
        if (value == null) {
            throw new AssertionError("Invalid test resource location: " + namespace + ":" + path);
        }
        return value;
    }

    @BeforeEach
    void setUp() {
        TierRegistry.clear();
    }

    @AfterEach
    void tearDown() {
        TierRegistry.clear();
    }

    @Test
    void duplicateIdIsRejectedWithoutMutation() {
        Tier first = new Tier("basic", 256, 1024);
        Tier second = new Tier("advanced", 512, 2048);
        ResourceLocation id = id("createtiers", "basic");

        TierRegistry.register(id, first);
        assertThrows(IllegalArgumentException.class, () -> TierRegistry.register(id, second));

        assertEquals(first, TierRegistry.get(id));
        assertEquals(1, TierRegistry.size());
    }

    @Test
    void generatedNamesMustBeUniqueAcrossNamespaces() {
        TierRegistry.register(id("pack_a", "steel"),
                new Tier("steel", 256, 1024));

        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> TierRegistry.register(
                id("pack_b", "steel"),
                new Tier("steel", 512, 2048)));
        assertTrue(error.getMessage().contains("Generated component names must be unique"));
    }

    @Test
    void invalidLimitsAndColorsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> TierRegistry.register(
                id("createtiers", "bad_rpm"),
                new Tier("bad_rpm", 0, 1024)));
        assertThrows(IllegalArgumentException.class, () -> TierRegistry.register(
                id("createtiers", "bad_su"),
                new Tier("bad_su", 256, 0)));
        assertThrows(IllegalArgumentException.class, () -> TierRegistry.register(
                id("createtiers", "bad_shaft_color"),
                new Tier("bad_shaft_color", 256, 1024, -1, 0xFFFFFF, null)));
        assertThrows(IllegalArgumentException.class, () -> TierRegistry.register(
                id("createtiers", "bad_cog_color"),
                new Tier("bad_cog_color", 256, 1024, 0xFFFFFF, 0x1000000, null)));
        assertEquals(0, TierRegistry.size());
    }

    @Test
    void invalidGeneratedResourceNameIsRejectedAtRegistration() {
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> TierRegistry.register(
                id("createtiers", "safe_lookup_id"),
                new Tier("Bad Name", 256, 1024)));

        assertTrue(error.getMessage().contains("invalid in Minecraft resource paths"));
        assertEquals(0, TierRegistry.size());
    }

    @Test
    void batchRegistrationIsAtomicWhenLaterEntryConflicts() {
        ResourceLocation basicId = id("pack_a", "basic");
        ResourceLocation duplicateNameId = id("pack_b", "basic");
        Map<ResourceLocation, Tier> registrations = new LinkedHashMap<>();
        registrations.put(basicId, new Tier("basic", 256, 1024));
        registrations.put(duplicateNameId, new Tier("basic", 512, 2048));

        assertThrows(IllegalArgumentException.class, () -> TierRegistry.registerAll(registrations));

        assertEquals(0, TierRegistry.size());
        assertFalse(TierRegistry.exists(basicId));
        assertFalse(TierRegistry.exists(duplicateNameId));
    }

    @Test
    void validBatchCommitsInInputOrderAndReadsBackInCapabilityOrder() {
        ResourceLocation highId = id("createtiers", "high");
        ResourceLocation lowId = id("createtiers", "low");
        Tier high = new Tier("high", 512, 4096);
        Tier low = new Tier("low", 256, 1024);
        Map<ResourceLocation, Tier> registrations = new LinkedHashMap<>();
        registrations.put(highId, high);
        registrations.put(lowId, low);

        assertEquals(List.of(high, low), TierRegistry.registerAll(registrations));
        assertEquals(List.of(low, high), List.copyOf(TierRegistry.getAllTiers()));
        assertEquals(high, TierRegistry.get(highId));
    }

    @Test
    void crossedCapabilitiesAreRejectedRegardlessOfRegistrationOrder() {
        Tier highSuLowRpm = new Tier("torque", 256, 8192);
        Tier highRpmLowSu = new Tier("speed", 512, 4096);

        TierRegistry.register(id("createtiers", "torque"), highSuLowRpm);
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> TierRegistry.register(
                id("createtiers", "speed"), highRpmLowSu));
        assertTrue(error.getMessage().contains("incomparable"));
        assertEquals(1, TierRegistry.size());

        TierRegistry.clear();
        TierRegistry.register(id("createtiers", "speed"), highRpmLowSu);
        assertThrows(IllegalArgumentException.class, () -> TierRegistry.register(
                id("createtiers", "torque"), highSuLowRpm));
        assertEquals(1, TierRegistry.size());
    }

    @Test
    void crossedCapabilitiesRejectWholeBatchAtomically() {
        Map<ResourceLocation, Tier> registrations = new LinkedHashMap<>();
        registrations.put(id("createtiers", "basic"), new Tier("basic", 256, 1024));
        registrations.put(id("createtiers", "torque"), new Tier("torque", 512, 8192));
        registrations.put(id("createtiers", "speed"), new Tier("speed", 1024, 4096));

        assertThrows(IllegalArgumentException.class, () -> TierRegistry.registerAll(registrations));
        assertEquals(0, TierRegistry.size());
    }

    @Test
    void equalCapabilityTiersRemainValidAndDeterministic() {
        Tier beta = new Tier("beta", 512, 4096);
        Tier alpha = new Tier("alpha", 512, 4096);

        TierRegistry.register(id("createtiers", "beta"), beta);
        TierRegistry.register(id("createtiers", "alpha"), alpha);

        assertEquals(List.of(alpha, beta), List.copyOf(TierRegistry.getAllTiers()));
    }

    @Test
    void oneCapabilityMayStayEqualWhileTheOtherIncreases() {
        Tier basic = new Tier("basic", 256, 1024);
        Tier moreSu = new Tier("more_su", 256, 4096);
        Tier moreRpm = new Tier("more_rpm", 512, 4096);

        TierRegistry.register(id("createtiers", "basic"), basic);
        TierRegistry.register(id("createtiers", "more_su"), moreSu);
        TierRegistry.register(id("createtiers", "more_rpm"), moreRpm);

        assertEquals(List.of(basic, moreSu, moreRpm), List.copyOf(TierRegistry.getAllTiers()));
    }

    @Test
    void frozenRegistryRejectsSingleAndBatchMutationsWithoutChangingContents() {
        ResourceLocation basicId = id("createtiers", "basic");
        Tier basic = new Tier("basic", 256, 1024);
        TierRegistry.register(basicId, basic);
        TierRegistry.freeze();

        assertThrows(IllegalStateException.class, () -> TierRegistry.register(
                id("createtiers", "advanced"),
                new Tier("advanced", 512, 2048)));
        assertThrows(IllegalStateException.class, () -> TierRegistry.registerAll(Map.of(
                id("createtiers", "elite"),
                new Tier("elite", 1024, 4096))));

        assertTrue(TierRegistry.isFrozen());
        assertEquals(1, TierRegistry.size());
        assertEquals(basic, TierRegistry.get(basicId));
    }
}
