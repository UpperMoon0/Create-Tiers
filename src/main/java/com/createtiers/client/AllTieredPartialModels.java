package com.createtiers.client;

import com.createtiers.CreateTiers;
import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds tiered partial models that use grayscale textures for proper color tinting.
 * These are used by both Flywheel visuals and the BER renderer.
 */
public class AllTieredPartialModels {

    // Maps tier name to its partial models
    public static final Map<String, TieredPartials> TIERS = new HashMap<>();

    public static class TieredPartials {
        public final PartialModel SHAFT;
        public final PartialModel SHAFT_HALF;
        public final PartialModel COGWHEEL_SHAFTLESS;
        public final PartialModel LARGE_COGWHEEL_SHAFTLESS;
        public final PartialModel COGWHEEL_SHAFT;

        public TieredPartials(String tierName) {
            String prefix = "block/" + tierName + "/";
            SHAFT = PartialModel.of(CreateTiers.asResource(prefix + "shaft"));
            SHAFT_HALF = PartialModel.of(CreateTiers.asResource(prefix + "shaft_half"));
            COGWHEEL_SHAFTLESS = PartialModel.of(CreateTiers.asResource(prefix + "cogwheel_shaftless"));
            LARGE_COGWHEEL_SHAFTLESS = PartialModel.of(CreateTiers.asResource(prefix + "large_cogwheel_shaftless"));
            COGWHEEL_SHAFT = PartialModel.of(CreateTiers.asResource(prefix + "cogwheel_shaft"));
        }
    }

    /**
     * Initialize all tiered partial models. Called during client setup.
     */
    public static void init() {
        CreateTiers.LOGGER.info("Initializing tiered partial models...");
        for (Tier tier : TierRegistry.getAllTiers()) {
            String tierName = tier.getName();
            TIERS.put(tierName, new TieredPartials(tierName));
            CreateTiers.LOGGER.debug("Registered partial models for tier: {}", tierName);
        }
        CreateTiers.LOGGER.info("Initialized {} tier partial model sets", TIERS.size());
    }

    /**
     * Get partial models for a specific tier.
     */
    public static TieredPartials forTier(String tierName) {
        return TIERS.get(tierName);
    }

    /**
     * Get partial models for a specific tier.
     */
    public static TieredPartials forTier(Tier tier) {
        return forTier(tier.getName());
    }
}
