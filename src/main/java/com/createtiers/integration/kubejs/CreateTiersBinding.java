package com.createtiers.integration.kubejs;

import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * KubeJS binding class for Create Tiers.
 * This class provides the JavaScript API for registering tiers.
 */
public class CreateTiersBinding {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("CreateTiers/KubeJS");
    
    /**
     * Register a tier with full customization including dual colors.
     */
    public static void registerTier(String name, int level, int maxRPM, int maxSU, int shaftColor, int cogwheelColor, String displayName) {
        ResourceLocation id = new ResourceLocation("createtiers", name);
        
        Tier tier = Tier.builder()
                .tier(level)
                .name(name)
                .maxRPM(maxRPM)
                .maxSU(maxSU)
                .shaftColor(shaftColor)
                .cogwheelColor(cogwheelColor)
                .displayName(displayName)
                .build();
        
        TierRegistry.register(id, tier);
        
        LOGGER.info("Registered tier '{}' via KubeJS: level={}, maxRPM={}, maxSU={}, shaftColor=#{}, cogwheelColor=#{}",
                name, level, maxRPM, maxSU, String.format("%06X", shaftColor), String.format("%06X", cogwheelColor));
    }

    /**
     * Register a tier with a single color (applies to both shaft and cogwheel).
     */
    public static void registerTier(String name, int level, int maxRPM, int maxSU, int color, String displayName) {
        registerTier(name, level, maxRPM, maxSU, color, color, displayName);
    }
    
    /**
     * Register a tier with a resource location from another mod and dual colors.
     */
    public static void registerCustomTier(String namespace, String name, int level, int maxRPM, int maxSU, int shaftColor, int cogwheelColor) {
        ResourceLocation id = new ResourceLocation(namespace, name);
        
        Tier tier = Tier.builder()
                .tier(level)
                .name(name)
                .maxRPM(maxRPM)
                .maxSU(maxSU)
                .shaftColor(shaftColor)
                .cogwheelColor(cogwheelColor)
                .build();
        
        TierRegistry.register(id, tier);
        
        LOGGER.info("Registered custom tier '{}:{}' via KubeJS: level={}, maxRPM={}, maxSU={}, shaftColor=#{}, cogwheelColor=#{}",
                namespace, name, level, maxRPM, maxSU, String.format("%06X", shaftColor), String.format("%06X", cogwheelColor));
    }
    
    /**
     * Register a custom tier with a single color.
     */
    public static void registerCustomTier(String namespace, String name, int level, int maxRPM, int maxSU, int color) {
        registerCustomTier(namespace, name, level, maxRPM, maxSU, color, color);
    }
    
    public static Tier getTier(String name) {
        return TierRegistry.get(new ResourceLocation("createtiers", name));
    }
    
    public static Tier getTierByLevel(int level) {
        return TierRegistry.getByLevel(level);
    }
    
    public static Collection<Tier> getAllTiers() {
        return TierRegistry.getAllTiers();
    }
    
    public static boolean tierExists(String name) {
        return TierRegistry.exists(new ResourceLocation("createtiers", name));
    }
}
