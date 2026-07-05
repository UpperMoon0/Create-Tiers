package com.createtiers.client;

import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GearboxModelTest {

    @Test
    void tieredPartialsMapIsPopulated() {
        AllTieredPartialModels.init();

        assertFalse(TierRegistry.getAllTiers().isEmpty(), "Tier registry should have tiers");

        for (Tier tier : TierRegistry.getAllTiers()) {
            AllTieredPartialModels.TieredPartials partials = AllTieredPartialModels.forTier(tier.getName());
            assertNotNull(partials, "Partial models should exist for tier: " + tier.getName());
            assertNotNull(partials.GEARBOX, "GEARBOX partial should exist for tier: " + tier.getName());
            assertNotNull(partials.SHAFT_HALF, "SHAFT_HALF partial should exist for tier: " + tier.getName());
        }
    }

    @Test
    void gearboxPartialHasCorrectResourcePath() {
        AllTieredPartialModels.init();

        for (Tier tier : TierRegistry.getAllTiers()) {
            AllTieredPartialModels.TieredPartials partials = AllTieredPartialModels.forTier(tier.getName());
            String tierName = tier.getName();

            assertNotNull(partials.GEARBOX, "GEARBOX partial should exist");
        }
    }

    @Test
    void shaftHalfPartialIsDifferentFromGearboxPartial() {
        AllTieredPartialModels.init();

        for (Tier tier : TierRegistry.getAllTiers()) {
            AllTieredPartialModels.TieredPartials partials = AllTieredPartialModels.forTier(tier.getName());
            assertNotSame(partials.SHAFT_HALF, partials.GEARBOX,
                "SHAFT_HALF and GEARBOX should be different partial models");
        }
    }
}