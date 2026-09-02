package com.createtiers.api;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing tiers in Create Tiers.
 * Tiers can be registered via KubeJS startup scripts or other mods.
 * The registry must be populated before the block registration event.
 */
public class TierRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger("CreateTiers");

    private static final Map<ResourceLocation, Tier> TIERS = new ConcurrentHashMap<>();
    private static final Map<Integer, Tier> TIERS_BY_LEVEL = new ConcurrentHashMap<>();
    private static boolean frozen = false;

    /**
     * Register a new tier. Must be called before the registry is frozen.
     * IDs, numeric levels, and generated tier names are all unique.
     *
     * @param id The unique identifier for this tier
     * @param tier The tier to register
     * @return The registered tier
     * @throws IllegalStateException if the registry is frozen
     * @throws IllegalArgumentException if the tier is invalid or conflicts with an existing tier
     */
    public static synchronized Tier register(ResourceLocation id, Tier tier) {
        Objects.requireNonNull(id, "Tier id cannot be null");
        Objects.requireNonNull(tier, "Tier cannot be null");

        if (frozen) {
            throw new IllegalStateException("Tier registry is frozen. Tiers must be registered during mod initialization/startup scripts.");
        }

        validateTier(tier);

        if (TIERS.containsKey(id)) {
            throw new IllegalArgumentException("Tier id '" + id + "' is already registered");
        }

        Tier levelConflict = TIERS_BY_LEVEL.get(tier.getTier());
        if (levelConflict != null) {
            throw new IllegalArgumentException("Tier level " + tier.getTier() + " is already used by '" + levelConflict.getName() + "'");
        }

        for (Map.Entry<ResourceLocation, Tier> entry : TIERS.entrySet()) {
            if (entry.getValue().getName().equals(tier.getName())) {
                throw new IllegalArgumentException(
                        "Tier generated name '" + tier.getName() + "' is already used by " + entry.getKey()
                                + ". Generated component names must be unique across namespaces.");
            }
        }

        TIERS.put(id, tier);
        TIERS_BY_LEVEL.put(tier.getTier(), tier);
        LOGGER.info("Registered tier: {} (level {}, maxRPM: {}, maxSU: {})",
                id, tier.getTier(), tier.getMaxRPM(), tier.getMaxSU());

        return tier;
    }

    private static void validateTier(Tier tier) {
        if (tier.getTier() <= 0) {
            throw new IllegalArgumentException("Tier level must be greater than 0: " + tier.getTier());
        }
        if (tier.getName() == null || tier.getName().isBlank()) {
            throw new IllegalArgumentException("Tier name cannot be blank");
        }
        if (tier.getMaxRPM() <= 0) {
            throw new IllegalArgumentException("Tier maxRPM must be greater than 0 for '" + tier.getName() + "'");
        }
        if (tier.getMaxSU() <= 0) {
            throw new IllegalArgumentException("Tier maxSU must be greater than 0 for '" + tier.getName() + "'");
        }
        validateColor("shaftColor", tier.getShaftColor(), tier.getName());
        validateColor("cogwheelColor", tier.getCogwheelColor(), tier.getName());
    }

    private static void validateColor(String field, int color, String tierName) {
        if (color < 0 || color > 0xFFFFFF) {
            throw new IllegalArgumentException(field + " for tier '" + tierName + "' must be a 24-bit RGB value (0x000000-0xFFFFFF)");
        }
    }

    public static Tier get(ResourceLocation id) {
        return TIERS.get(id);
    }

    public static Tier getByLevel(int level) {
        return TIERS_BY_LEVEL.get(level);
    }

    public static int getMaxPossibleRPM() {
        return TIERS.values().stream()
                .mapToInt(Tier::getMaxRPM)
                .max()
                .orElse(0);
    }

    public static Collection<Tier> getAllTiers() {
        List<Tier> tiers = new ArrayList<>(TIERS.values());
        tiers.sort(Comparator.comparingInt(Tier::getTier));
        return tiers;
    }

    public static Set<ResourceLocation> getAllTierIds() {
        return Collections.unmodifiableSet(TIERS.keySet());
    }

    public static boolean exists(ResourceLocation id) {
        return TIERS.containsKey(id);
    }

    public static boolean existsByLevel(int level) {
        return TIERS_BY_LEVEL.containsKey(level);
    }

    public static int size() {
        return TIERS.size();
    }

    public static synchronized void freeze() {
        if (!frozen) {
            frozen = true;
            LOGGER.info("Tier registry frozen with {} tiers registered", TIERS.size());
        }
    }

    public static boolean isFrozen() {
        return frozen;
    }

    /** Testing only. */
    public static synchronized void unfreeze() {
        frozen = false;
        LOGGER.warn("Tier registry unfrozen - this should only be used for testing!");
    }

    /** Testing only. */
    public static synchronized void clear() {
        TIERS.clear();
        TIERS_BY_LEVEL.clear();
        frozen = false;
        LOGGER.warn("Tier registry cleared - this should only be used for testing!");
    }
}
