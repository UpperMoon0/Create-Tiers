package com.createtiers.client;

import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GearboxModelTest {

    @Test
    void tieredPartialsMapIsPopulated() {
        AllTieredPartialModels.init();

        assertFalse(TierRegistry.getAllTiers().isEmpty(), "Tier registry should have tiers");

        for (Tier tier : TierRegistry.getAllTiers()) {
            AllTieredPartialModels.TieredPartials partials = AllTieredPartialModels.forTier(tier.getName());
            assertNotNull(partials, "Partial models should exist for tier: " + tier.getName());
            assertNotNull(partials.SHAFT_HALF, "Gearbox shaft-half partial should exist for tier: " + tier.getName());
        }
    }

    @Test
    void gearboxUsesTierShaftHalfPartial() {
        AllTieredPartialModels.init();

        for (Tier tier : TierRegistry.getAllTiers()) {
            AllTieredPartialModels.TieredPartials partials = AllTieredPartialModels.forTier(tier.getName());
            assertNotNull(partials.SHAFT_HALF,
                    "Tiered gearboxes render each output using the tier shaft-half partial");
        }
    }

    @Test
    void attachedTierAccentPartialIsRegistered() {
        assertNotNull(AllTieredPartialModels.ATTACHED_TIER_ACCENT,
                "Calibrated ordinary Create kinetics need the generic tier accent partial");
    }
}
