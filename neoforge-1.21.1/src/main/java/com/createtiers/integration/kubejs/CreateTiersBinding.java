package com.createtiers.integration.kubejs;

import com.createtiers.Compat;
import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public class CreateTiersBinding {

    private static final Logger LOGGER = LoggerFactory.getLogger("CreateTiers/KubeJS");

    public static void registerTiers(List<Map<String, Object>> tiers) {
        for (Map<String, Object> tierData : tiers) {
            String name = (String) tierData.get("name");
            int level = ((Number) tierData.get("level")).intValue();
            int maxRPM = ((Number) tierData.get("maxRPM")).intValue();
            int maxSU = ((Number) tierData.get("maxSU")).intValue();

            int shaftColor = tierData.containsKey("shaftColor") ? ((Number) tierData.get("shaftColor")).intValue() : 0xFFFFFF;
            int cogwheelColor = tierData.containsKey("cogwheelColor") ? ((Number) tierData.get("cogwheelColor")).intValue() : shaftColor;

            String displayName = tierData.containsKey("displayName") ? (String) tierData.get("displayName") : name;

            registerTier(name, level, maxRPM, maxSU, shaftColor, cogwheelColor, displayName);
        }
        LOGGER.info("Registered {} tiers via registerTiers batch call", tiers.size());
    }

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

    public static void registerTier(String name, int level, int maxRPM, int maxSU, int color, String displayName) {
        registerTier(name, level, maxRPM, maxSU, color, color, displayName);
    }

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
