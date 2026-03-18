package com.createtiers.integration.kubejs;

import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * KubeJS binding class for Create Tiers.
 * This class provides the JavaScript API for registering tiers.
 */
public class CreateTiersBinding {
    
    private static final Logger LOGGER = LoggerFactory.getLogger("CreateTiers/KubeJS");
    
    /**
     * Register multiple tiers at once for cleaner code.
     * Example usage in KubeJS:
     * <pre>
     * CreateTiers.registerTiers([
     *     { name: 'crude', level: 1, maxRPM: 128, maxSU: 512, shaftColor: 0x6B4E2C, cogwheelColor: 0x6B4E2C, displayName: 'Crude' },
     *     { name: 'basic', level: 2, maxRPM: 256, maxSU: 2048, shaftColor: 0x936C3D, cogwheelColor: 0xBCBCBC, displayName: 'Basic' }
     * ]);
     * </pre>
     */
    @SuppressWarnings("unchecked")
    public static void registerTiers(List<Map<String, Object>> tiers) {
        for (Map<String, Object> tierData : tiers) {
            String name = (String) tierData.get("name");
            int level = ((Number) tierData.get("level")).intValue();
            int maxRPM = ((Number) tierData.get("maxRPM")).intValue();
            int maxSU = ((Number) tierData.get("maxSU")).intValue();
            
            // Colors are optional - default to 0xFFFFFF if not provided
            int shaftColor = tierData.containsKey("shaftColor") ? ((Number) tierData.get("shaftColor")).intValue() : 0xFFFFFF;
            int cogwheelColor = tierData.containsKey("cogwheelColor") ? ((Number) tierData.get("cogwheelColor")).intValue() : shaftColor;
            
            // DisplayName is optional - default to name if not provided
            String displayName = tierData.containsKey("displayName") ? (String) tierData.get("displayName") : name;
            
            registerTier(name, level, maxRPM, maxSU, shaftColor, cogwheelColor, displayName);
        }
        LOGGER.info("Registered {} tiers via registerTiers batch call", tiers.size());
    }
    
    /**
     * Register a tier with full customization including dual colors.
     */
    public static void registerTier(String name, int level, int maxRPM, int maxSU, int shaftColor, int cogwheelColor, String displayName) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("createtiers", name);
        
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
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, name);
        
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
        return TierRegistry.get(ResourceLocation.fromNamespaceAndPath("createtiers", name));
    }
    
    public static Tier getTierByLevel(int level) {
        return TierRegistry.getByLevel(level);
    }
    
    public static Collection<Tier> getAllTiers() {
        return TierRegistry.getAllTiers();
    }
    
    public static boolean tierExists(String name) {
        return TierRegistry.exists(ResourceLocation.fromNamespaceAndPath("createtiers", name));
    }
}
