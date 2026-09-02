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

/** KubeJS startup-script API for registering Create Tiers tiers. */
public class CreateTiersBinding {

    private static final Logger LOGGER = LoggerFactory.getLogger("CreateTiers/KubeJS");

    public static void registerTiers(List<Map<String, Object>> tiers) {
        if (tiers == null) {
            throw new IllegalArgumentException("CreateTiers.registerTiers requires a tier list");
        }

        for (int index = 0; index < tiers.size(); index++) {
            Map<String, Object> tierData = tiers.get(index);
            if (tierData == null) {
                throw new IllegalArgumentException("Create Tiers tier entry #" + index + " cannot be null");
            }

            String name = requireString(tierData, "name", index);
            int level = requireNumber(tierData, "level", index);
            int maxRPM = requireNumber(tierData, "maxRPM", index);
            int maxSU = requireNumber(tierData, "maxSU", index);
            int shaftColor = optionalNumber(tierData, "shaftColor", 0xFFFFFF, index);
            int cogwheelColor = optionalNumber(tierData, "cogwheelColor", shaftColor, index);
            String displayName = optionalString(tierData, "displayName", name, index);

            registerTier(name, level, maxRPM, maxSU, shaftColor, cogwheelColor, displayName);
        }
        LOGGER.info("Registered {} tiers via registerTiers batch call", tiers.size());
    }

    public static void registerTier(String name, int level, int maxRPM, int maxSU) {
        registerTier(name, level, maxRPM, maxSU, 0xFFFFFF, 0xFFFFFF, name);
    }

    public static void registerTier(String name, int level, int maxRPM, int maxSU, int color) {
        registerTier(name, level, maxRPM, maxSU, color, color, name);
    }

    public static void registerTier(String name, int level, int maxRPM, int maxSU, int color, String displayName) {
        registerTier(name, level, maxRPM, maxSU, color, color, displayName);
    }

    public static void registerTier(String name, int level, int maxRPM, int maxSU, int shaftColor, int cogwheelColor) {
        registerTier(name, level, maxRPM, maxSU, shaftColor, cogwheelColor, name);
    }

    public static void registerTier(String name, int level, int maxRPM, int maxSU, int shaftColor, int cogwheelColor,
            String displayName) {
        requireDirectName(name, "name");
        ResourceLocation id = rl("createtiers", name);

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
        LOGGER.info("Registered tier '{}' via KubeJS: level={}, maxRPM={}, maxSU={}", name, level, maxRPM, maxSU);
    }

    /**
     * Registers a custom lookup id. Generated component registry names still use {@code name},
     * so names must remain globally unique across all tier namespaces.
     */
    public static void registerCustomTier(String namespace, String name, int level, int maxRPM, int maxSU,
            int shaftColor, int cogwheelColor) {
        requireDirectName(namespace, "namespace");
        requireDirectName(name, "name");
        ResourceLocation id = rl(namespace, name);

        Tier tier = Tier.builder()
                .tier(level)
                .name(name)
                .maxRPM(maxRPM)
                .maxSU(maxSU)
                .shaftColor(shaftColor)
                .cogwheelColor(cogwheelColor)
                .displayName(name)
                .build();

        TierRegistry.register(id, tier);
        LOGGER.info("Registered custom tier '{}:{}' via KubeJS: level={}, maxRPM={}, maxSU={}",
                namespace, name, level, maxRPM, maxSU);
    }

    public static void registerCustomTier(String namespace, String name, int level, int maxRPM, int maxSU, int color) {
        registerCustomTier(namespace, name, level, maxRPM, maxSU, color, color);
    }

    public static Tier getTier(String name) {
        return TierRegistry.get(rl("createtiers", name));
    }

    public static Tier getTierByLevel(int level) {
        return TierRegistry.getByLevel(level);
    }

    public static Collection<Tier> getAllTiers() {
        return TierRegistry.getAllTiers();
    }

    public static boolean tierExists(String name) {
        return TierRegistry.exists(rl("createtiers", name));
    }

    private static String requireString(Map<String, Object> data, String key, int index) {
        Object value = data.get(key);
        if (!(value instanceof String string) || string.isBlank()) {
            throw fieldError(index, key, "must be a non-empty string");
        }
        return string;
    }

    private static String optionalString(Map<String, Object> data, String key, String fallback, int index) {
        if (!data.containsKey(key)) {
            return fallback;
        }
        return requireString(data, key, index);
    }

    private static int requireNumber(Map<String, Object> data, String key, int index) {
        Object value = data.get(key);
        if (!(value instanceof Number number)) {
            throw fieldError(index, key, "must be a number");
        }
        return number.intValue();
    }

    private static int optionalNumber(Map<String, Object> data, String key, int fallback, int index) {
        if (!data.containsKey(key)) {
            return fallback;
        }
        return requireNumber(data, key, index);
    }

    private static IllegalArgumentException fieldError(int index, String field, String reason) {
        return new IllegalArgumentException("Create Tiers tier entry #" + index + " field '" + field + "' " + reason);
    }

    private static void requireDirectName(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Create Tiers " + field + " cannot be blank");
        }
    }

    private static ResourceLocation rl(String namespace, String path) {
        try {
            return Compat.rl(namespace, path);
        } catch (IllegalStateException e) {
            return new ResourceLocation(namespace, path);
        }
    }
}
