package com.createtiers.api;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for managing tiers in Create Tiers.
 * Tiers can be registered via KubeJS or other mods.
 * The registry must be populated before the block registration event.
 */
public class TierRegistry {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("CreateTiers");
    
    private static final Map<ResourceLocation, Tier> TIERS = new ConcurrentHashMap<>();
    private static final Map<Integer, Tier> TIERS_BY_LEVEL = new ConcurrentHashMap<>();
    private static boolean frozen = false;
    
    /**
     * Register a new tier. Must be called before the registry is frozen.
     * @param id The unique identifier for this tier
     * @param tier The tier to register
     * @return The registered tier
     * @throws IllegalStateException if the registry is frozen
     */
    public static Tier register(ResourceLocation id, Tier tier) {
        if (frozen) {
            throw new IllegalStateException("Tier registry is frozen. Tiers must be registered during the mod initialization phase.");
        }
        
        if (TIERS.containsKey(id)) {
            LOGGER.warn("Tier {} is already registered, overwriting", id);
        }
        
        TIERS.put(id, tier);
        TIERS_BY_LEVEL.put(tier.getTier(), tier);
        LOGGER.info("Registered tier: {} (level {}, maxRPM: {}, maxSU: {})", 
                id, tier.getTier(), tier.getMaxRPM(), tier.getMaxSU());
        
        return tier;
    }
    
    /**
     * Get a tier by its resource location
     * @param id The tier's resource location
     * @return The tier, or null if not found
     */
    public static Tier get(ResourceLocation id) {
        return TIERS.get(id);
    }
    
    /**
     * Get a tier by its numeric level
     * @param level The tier level
     * @return The tier, or null if not found
     */
    public static Tier getByLevel(int level) {
        return TIERS_BY_LEVEL.get(level);
    }

    /**
     * Get the maximum RPM possible across all registered tiers.
     * @return The highest maxRPM value among all tiers, or 0 if no tiers registered
     */
    public static int getMaxPossibleRPM() {
        return TIERS.values().stream()
                .mapToInt(Tier::getMaxRPM)
                .max()
                .orElse(0);
    }
    
    
    /**
     * Get all registered tiers, sorted by level
     * @return A sorted collection of all tiers
     */
    public static Collection<Tier> getAllTiers() {
        List<Tier> tiers = new ArrayList<>(TIERS.values());
        tiers.sort(Comparator.comparingInt(Tier::getTier));
        return tiers;
    }
    
    /**
     * Get all registered tier IDs
     * @return A set of all tier resource locations
     */
    public static Set<ResourceLocation> getAllTierIds() {
        return Collections.unmodifiableSet(TIERS.keySet());
    }
    
    /**
     * Check if a tier exists
     * @param id The tier's resource location
     * @return true if the tier exists
     */
    public static boolean exists(ResourceLocation id) {
        return TIERS.containsKey(id);
    }
    
    /**
     * Check if a tier exists for a given level
     * @param level The tier level
     * @return true if a tier exists for this level
     */
    public static boolean existsByLevel(int level) {
        return TIERS_BY_LEVEL.containsKey(level);
    }
    
    /**
     * Get the number of registered tiers
     * @return The count of registered tiers
     */
    public static int size() {
        return TIERS.size();
    }
    
    /**
     * Freeze the registry. This is called automatically before block registration.
     * After freezing, no new tiers can be registered.
     */
    public static void freeze() {
        if (!frozen) {
            frozen = true;
            LOGGER.info("Tier registry frozen with {} tiers registered", TIERS.size());
        }
    }
    
    /**
     * Check if the registry is frozen
     * @return true if the registry is frozen
     */
    public static boolean isFrozen() {
        return frozen;
    }
    
    /**
     * Unfreeze the registry. This should only be used for testing purposes.
     */
    public static void unfreeze() {
        frozen = false;
        LOGGER.warn("Tier registry unfrozen - this should only be used for testing!");
    }
    
    /**
     * Clear all registered tiers. This should only be used for testing purposes.
     */
    public static void clear() {
        TIERS.clear();
        TIERS_BY_LEVEL.clear();
        frozen = false;
        LOGGER.warn("Tier registry cleared - this should only be used for testing!");
    }
}
