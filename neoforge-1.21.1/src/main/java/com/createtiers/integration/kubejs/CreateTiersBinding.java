package com.createtiers.integration.kubejs;

import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import com.createtiers.api.TierUpgradeRegistry;
import com.createtiers.foundation.item.CalibratedItemData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** KubeJS startup-script API for registering Create Tiers tiers. */
public class CreateTiersBinding {

    private static final Logger LOGGER = LoggerFactory.getLogger("CreateTiers/KubeJS");

    public static void registerTiers(List<Map<String, Object>> tiers) {
        if (tiers == null) {
            throw new IllegalArgumentException("CreateTiers.registerTiers requires a tier list");
        }

        Map<ResourceLocation, Tier> registrations = new LinkedHashMap<>();
        for (int index = 0; index < tiers.size(); index++) {
            Map<String, Object> tierData = tiers.get(index);
            if (tierData == null) {
                throw new IllegalArgumentException("Create Tiers tier entry #" + index + " cannot be null");
            }

            String name = requireString(tierData, "name", index);
            int maxRPM = requireNumber(tierData, "maxRPM", index);
            int maxSU = requireNumber(tierData, "maxSU", index);
            int shaftColor = optionalNumber(tierData, "shaftColor", 0xFFFFFF, index);
            int cogwheelColor = optionalNumber(tierData, "cogwheelColor", shaftColor, index);
            String displayName = optionalString(tierData, "displayName", name, index);

            ResourceLocation id = ResourceLocation.fromNamespaceAndPath("createtiers", name);
            Tier tier = createTier(name, maxRPM, maxSU, shaftColor, cogwheelColor, displayName);
            if (registrations.putIfAbsent(id, tier) != null) {
                throw fieldError(index, "name", "duplicates another tier in this batch");
            }
        }

        TierRegistry.registerAll(registrations);
        LOGGER.info("Registered {} tiers via registerTiers batch call", tiers.size());
    }

    public static void registerTier(String name, int maxRPM, int maxSU) {
        registerTierStyled(name, maxRPM, maxSU, 0xFFFFFF, 0xFFFFFF, name);
    }

    /**
     * Styled direct registration uses a distinct method name so removed level-based
     * overloads cannot be silently reinterpreted as color arguments.
     */
    public static void registerTierStyled(String name, int maxRPM, int maxSU, int color, String displayName) {
        registerTierStyled(name, maxRPM, maxSU, color, color, displayName);
    }

    public static void registerTierStyled(String name, int maxRPM, int maxSU, int shaftColor, int cogwheelColor,
            String displayName) {
        requireDirectName(name, "name");
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath("createtiers", name);
        Tier tier = createTier(name, maxRPM, maxSU, shaftColor, cogwheelColor, displayName);

        TierRegistry.register(id, tier);
        LOGGER.info("Registered tier '{}' via KubeJS: maxRPM={}, maxSU={}", name, maxRPM, maxSU);
    }

    /**
     * Registers a custom lookup id. Generated component registry names still use {@code name},
     * so names must remain globally unique across all tier namespaces.
     */
    public static void registerCustomTier(String namespace, String name, int maxRPM, int maxSU) {
        registerCustomTierStyled(namespace, name, maxRPM, maxSU, 0xFFFFFF, 0xFFFFFF, name);
    }

    public static void registerCustomTierStyled(String namespace, String name, int maxRPM, int maxSU, int color,
            String displayName) {
        registerCustomTierStyled(namespace, name, maxRPM, maxSU, color, color, displayName);
    }

    public static void registerCustomTierStyled(String namespace, String name, int maxRPM, int maxSU,
            int shaftColor, int cogwheelColor, String displayName) {
        requireDirectName(namespace, "namespace");
        requireDirectName(name, "name");
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(namespace, name);
        Tier tier = createTier(name, maxRPM, maxSU, shaftColor, cogwheelColor, displayName);

        TierRegistry.register(id, tier);
        LOGGER.info("Registered custom tier '{}:{}' via KubeJS: maxRPM={}, maxSU={}",
                namespace, name, maxRPM, maxSU);
    }

    public static void registerTierUpgrade(String item, String tier) {
        registerTierUpgrade(item, tier, true);
    }

    public static void registerTierUpgrade(String item, String tier, boolean defaultRecipe) {
        ResourceLocation itemId = parseResourceLocation(item, "item", false);
        ResourceLocation tierId = parseResourceLocation(tier, "tier", true);
        TierUpgradeRegistry.register(itemId, tierId, defaultRecipe);
        LOGGER.info("Registered tier upgrade '{} -> {}' (defaultRecipe={})", itemId, tierId, defaultRecipe);
    }

    public static void registerTierUpgrades(List<Map<String, Object>> upgrades) {
        if (upgrades == null) {
            throw new IllegalArgumentException("CreateTiers.registerTierUpgrades requires an upgrade list");
        }

        List<TierUpgradeRegistry.Registration> registrations = new java.util.ArrayList<>(upgrades.size());
        for (int index = 0; index < upgrades.size(); index++) {
            Map<String, Object> data = upgrades.get(index);
            if (data == null) {
                throw new IllegalArgumentException("Create Tiers tier upgrade entry #" + index + " cannot be null");
            }
            registrations.add(new TierUpgradeRegistry.Registration(
                    parseResourceLocation(requireUpgradeString(data, "item", index), "item", false),
                    parseResourceLocation(requireUpgradeString(data, "tier", index), "tier", true),
                    optionalBoolean(data, "defaultRecipe", true, index)));
        }

        TierUpgradeRegistry.registerAll(registrations);
        LOGGER.info("Registered {} tier upgrades via registerTierUpgrades batch call", registrations.size());
    }

    /**
     * Build a registered tiered item for use as the output of any KubeJS recipe type.
     */
    public static ItemStack tieredItem(String item, String tier) {
        ResourceLocation itemId = parseResourceLocation(item, "item", false);
        ResourceLocation tierId = parseResourceLocation(tier, "tier", true);

        if (!TierUpgradeRegistry.isRegistered(itemId, tierId)) {
            throw new IllegalArgumentException(
                    "No tier upgrade is registered for item '" + itemId + "' and tier '" + tierId + "'");
        }

        Item input = BuiltInRegistries.ITEM.get(itemId);
        if (!itemId.equals(BuiltInRegistries.ITEM.getKey(input))) {
            throw new IllegalArgumentException("Unknown tier upgrade item: " + itemId);
        }
        Tier resolvedTier = TierRegistry.get(tierId);
        if (resolvedTier == null) {
            throw new IllegalArgumentException("Unknown Create Tiers tier: " + tierId);
        }
        return CalibratedItemData.calibratedCopy(new ItemStack(input), resolvedTier);
    }

    public static Tier getTier(String name) {
        return TierRegistry.get(ResourceLocation.fromNamespaceAndPath("createtiers", name));
    }

    public static Collection<Tier> getAllTiers() {
        return TierRegistry.getAllTiers();
    }

    public static boolean tierExists(String name) {
        return TierRegistry.exists(ResourceLocation.fromNamespaceAndPath("createtiers", name));
    }

    private static Tier createTier(String name, int maxRPM, int maxSU, int shaftColor, int cogwheelColor,
            String displayName) {
        return Tier.builder()
                .name(name)
                .maxRPM(maxRPM)
                .maxSU(maxSU)
                .shaftColor(shaftColor)
                .cogwheelColor(cogwheelColor)
                .displayName(displayName)
                .build();
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
        try {
            return new BigDecimal(number.toString()).intValueExact();
        } catch (NumberFormatException | ArithmeticException e) {
            throw fieldError(index, key, "must be a whole 32-bit integer");
        }
    }

    private static int optionalNumber(Map<String, Object> data, String key, int fallback, int index) {
        if (!data.containsKey(key)) {
            return fallback;
        }
        return requireNumber(data, key, index);
    }

    private static String requireUpgradeString(Map<String, Object> data, String key, int index) {
        Object value = data.get(key);
        if (!(value instanceof String string) || string.isBlank()) {
            throw upgradeFieldError(index, key, "must be a non-empty string");
        }
        return string;
    }

    private static boolean optionalBoolean(Map<String, Object> data, String key, boolean fallback, int index) {
        if (!data.containsKey(key)) {
            return fallback;
        }
        Object value = data.get(key);
        if (!(value instanceof Boolean bool)) {
            throw upgradeFieldError(index, key, "must be a boolean");
        }
        return bool;
    }

    private static ResourceLocation parseResourceLocation(String value, String field, boolean defaultTierNamespace) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Create Tiers " + field + " cannot be blank");
        }
        String normalized = defaultTierNamespace && value.indexOf(':') < 0 ? "createtiers:" + value : value;
        ResourceLocation id = ResourceLocation.tryParse(normalized);
        if (id == null) {
            throw new IllegalArgumentException("Invalid Create Tiers " + field + " id: " + value);
        }
        return id;
    }

    private static IllegalArgumentException upgradeFieldError(int index, String field, String reason) {
        return new IllegalArgumentException(
                "Create Tiers tier upgrade entry #" + index + " field '" + field + "' " + reason);
    }

    private static IllegalArgumentException fieldError(int index, String field, String reason) {
        return new IllegalArgumentException("Create Tiers tier entry #" + index + " field '" + field + "' " + reason);
    }

    private static void requireDirectName(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Create Tiers " + field + " cannot be blank");
        }
    }
}
