package com.createtiers.client;

import com.createtiers.CreateTiers;
import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;

import java.util.HashMap;
import java.util.Map;

public class AllTieredPartialModels {

    /** Subtle machine accent used when an ordinary Create kinetic block is tier-upgraded. */
    public static final PartialModel ATTACHED_TIER_ACCENT =
            PartialModel.of(CreateTiers.asResource("block/tier_accent"));

    /** Create belt pulley wooden body with the shaft removed; tiered shaft is rendered separately. */
    public static final PartialModel BELT_PULLEY_BODY =
            PartialModel.of(CreateTiers.asResource("block/belt_pulley_body"));

    public static final Map<String, TieredPartials> TIERS = new HashMap<>();

    public static class TieredPartials {
        public final PartialModel SHAFT;
        public final PartialModel SHAFT_HALF;
        public final PartialModel POWERED_SHAFT;
        public final PartialModel COGWHEEL_SHAFTLESS;
        public final PartialModel LARGE_COGWHEEL_SHAFTLESS;
        public final PartialModel COGWHEEL_SHAFT;

        public final PartialModel ANDESITE_ENCASED_SHAFT;
        public final PartialModel BRASS_ENCASED_SHAFT;

        public TieredPartials(String tierName) {
            String prefix = "block/" + tierName + "/";
            SHAFT = PartialModel.of(CreateTiers.asResource(prefix + "shaft"));
            SHAFT_HALF = PartialModel.of(CreateTiers.asResource(prefix + "shaft_half"));
            POWERED_SHAFT = PartialModel.of(CreateTiers.asResource(prefix + "powered_shaft"));
            COGWHEEL_SHAFTLESS = PartialModel.of(CreateTiers.asResource(prefix + "cogwheel_shaftless"));
            LARGE_COGWHEEL_SHAFTLESS = PartialModel.of(CreateTiers.asResource(prefix + "large_cogwheel_shaftless"));
            COGWHEEL_SHAFT = PartialModel.of(CreateTiers.asResource(prefix + "cogwheel_shaft"));

            ANDESITE_ENCASED_SHAFT = SHAFT;
            BRASS_ENCASED_SHAFT = SHAFT;
        }
    }

    public static void init() {
        CreateTiers.LOGGER.info("Initializing tiered partial models...");
        for (Tier tier : TierRegistry.getAllTiers()) {
            String tierName = tier.getName();
            TIERS.put(tierName, new TieredPartials(tierName));
            CreateTiers.LOGGER.debug("Registered partial models for tier: {}", tierName);
        }
        CreateTiers.LOGGER.info("Initialized {} tier partial model sets", TIERS.size());
    }

    public static TieredPartials forTier(String tierName) {
        return TIERS.get(tierName);
    }

    public static TieredPartials forTier(Tier tier) {
        return forTier(tier.getName());
    }
}
