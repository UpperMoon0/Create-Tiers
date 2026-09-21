package com.createtiers.client;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TierUpgradeItemTintPolicyTest {

    private static ResourceLocation id(String value) {
        return ResourceLocation.tryParse(value);
    }

    @Test
    void fullTintUsesMechanicalChannelForAnySprite() {
        assertEquals(0, TierUpgradeItemTintPolicy.tintIndex(
                TierUpgradeItemTintPolicy.Mode.FULL,
                id("minecraft:block/spruce_planks")));
        assertEquals(0, TierUpgradeItemTintPolicy.tintIndex(
                TierUpgradeItemTintPolicy.Mode.FULL,
                id("create:block/brass_casing")));
    }

    @Test
    void shaftOnlyTintsAxisButPreservesCasing() {
        assertEquals(0, TierUpgradeItemTintPolicy.tintIndex(
                TierUpgradeItemTintPolicy.Mode.SHAFT_ONLY,
                id("create:block/axis")));
        assertEquals(0, TierUpgradeItemTintPolicy.tintIndex(
                TierUpgradeItemTintPolicy.Mode.SHAFT_ONLY,
                id("create:block/axis_top")));
        assertEquals(-1, TierUpgradeItemTintPolicy.tintIndex(
                TierUpgradeItemTintPolicy.Mode.SHAFT_ONLY,
                id("create:block/encased_chain_drive")));
    }

    @Test
    void cogwheelSeparatesShaftAndWheelChannels() {
        assertEquals(0, TierUpgradeItemTintPolicy.tintIndex(
                TierUpgradeItemTintPolicy.Mode.COGWHEEL,
                id("create:block/cogwheel_axis")));
        assertEquals(1, TierUpgradeItemTintPolicy.tintIndex(
                TierUpgradeItemTintPolicy.Mode.COGWHEEL,
                id("create:block/cogwheel")));
        assertEquals(1, TierUpgradeItemTintPolicy.tintIndex(
                TierUpgradeItemTintPolicy.Mode.COGWHEEL,
                id("create:block/large_cogwheel")));
        assertEquals(-1, TierUpgradeItemTintPolicy.tintIndex(
                TierUpgradeItemTintPolicy.Mode.COGWHEEL,
                id("create:block/andesite_encased_cogwheel_side")));
    }
}
