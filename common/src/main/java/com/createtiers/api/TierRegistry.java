package com.createtiers.api;

import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
    private static volatile boolean frozen = false;

    private static final Comparator<Map.Entry<ResourceLocation, Tier>> CAPABILITY_ORDER =
            Comparator.<Map.Entry<ResourceLocation, Tier>>comparingInt(entry -> entry.getValue().getMaxRPM())
                    .thenComparingInt(entry -> entry.getValue().getMaxSU())
                    .thenComparing(entry -> entry.getKey().toString());

    /**
     * Register a new tier. Must be called before the registry is frozen.
     * IDs and generated tier names are unique. Progression is derived from Max RPM
     * and Max SU rather than a separate numeric level.
     *
     * @param id The unique identifier for this tier
     * @param tier The tier to register
     * @return The registered tier
     * @throws IllegalStateException if the registry is frozen
     * @throws IllegalArgumentException if the tier is invalid or conflicts with an existing tier
     */
    public static synchronized Tier register(ResourceLocation id, Tier tier) {
        validateMutable();
        validateRegistration(id, tier, TIERS.keySet(), registeredNames());
        validateCapabilityOrder(Map.of(id, tier));
        put(id, tier);
        return tier;
    }

    /**
     * Atomically register a batch of tiers. The entire batch is validated against both the
     * existing registry and itself before any entry is committed.
     *
     * @param registrations ordered tier registrations
     * @return registered tiers in iteration order
     */
    public static synchronized List<Tier> registerAll(Map<ResourceLocation, Tier> registrations) {
        Objects.requireNonNull(registrations, "Tier registrations cannot be null");
        validateMutable();

        Set<ResourceLocation> ids = new HashSet<>(TIERS.keySet());
        Set<String> names = registeredNames();

        for (Map.Entry<ResourceLocation, Tier> entry : registrations.entrySet()) {
            validateRegistration(entry.getKey(), entry.getValue(), ids, names);
            ids.add(entry.getKey());
            names.add(entry.getValue().getName());
        }

        validateCapabilityOrder(registrations);

        List<Tier> registered = new ArrayList<>(registrations.size());
        for (Map.Entry<ResourceLocation, Tier> entry : registrations.entrySet()) {
            put(entry.getKey(), entry.getValue());
            registered.add(entry.getValue());
        }
        return registered;
    }

    private static void validateMutable() {
        if (frozen) {
            throw new IllegalStateException(
                    "Tier registry is frozen. Tiers must be registered during mod initialization/startup scripts.");
        }
    }

    private static void validateRegistration(ResourceLocation id, Tier tier, Set<ResourceLocation> ids,
            Set<String> names) {
        Objects.requireNonNull(id, "Tier id cannot be null");
        Objects.requireNonNull(tier, "Tier cannot be null");
        validateTier(tier);

        if (ids.contains(id)) {
            throw new IllegalArgumentException("Tier id '" + id + "' is already registered");
        }
        if (names.contains(tier.getName())) {
            throw new IllegalArgumentException(
                    "Tier generated name '" + tier.getName()
                            + "' is already registered. Generated component names must be unique across namespaces.");
        }
    }

    /**
     * Tier order must be derivable from capability alone. If one tier has more RPM
     * but less SU than another, neither is objectively the higher tier, so reject the
     * configuration instead of inventing an unrelated ordinal.
     */
    private static void validateCapabilityOrder(Map<ResourceLocation, Tier> pending) {
        Map<ResourceLocation, Tier> combined = new LinkedHashMap<>(TIERS);
        combined.putAll(pending);

        List<Map.Entry<ResourceLocation, Tier>> ordered = new ArrayList<>(combined.entrySet());
        ordered.sort(CAPABILITY_ORDER);

        int highestSU = -1;
        Map.Entry<ResourceLocation, Tier> highestSUEntry = null;
        for (Map.Entry<ResourceLocation, Tier> entry : ordered) {
            Tier tier = entry.getValue();
            if (tier.getMaxSU() < highestSU) {
                Tier conflicting = highestSUEntry.getValue();
                throw new IllegalArgumentException(
                        "Tier capabilities are incomparable: '" + highestSUEntry.getKey() + "' has "
                                + conflicting.getMaxRPM() + " RPM / " + conflicting.getMaxSU()
                                + " SU, while '" + entry.getKey() + "' has " + tier.getMaxRPM()
                                + " RPM / " + tier.getMaxSU()
                                + " SU. Higher RPM cannot come with lower Max SU.");
            }
            if (tier.getMaxSU() > highestSU) {
                highestSU = tier.getMaxSU();
                highestSUEntry = entry;
            }
        }
    }

    private static Set<String> registeredNames() {
        Set<String> names = new HashSet<>();
        for (Tier tier : TIERS.values()) {
            names.add(tier.getName());
        }
        return names;
    }

    private static void put(ResourceLocation id, Tier tier) {
        TIERS.put(id, tier);
        LOGGER.info("Registered tier: {} (maxRPM: {}, maxSU: {})",
                id, tier.getMaxRPM(), tier.getMaxSU());
    }

    private static void validateTier(Tier tier) {
        if (tier.getName() == null || tier.getName().isBlank()) {
            throw new IllegalArgumentException("Tier name cannot be blank");
        }
        if (!isValidGeneratedPath(tier.getName())) {
            throw new IllegalArgumentException(
                    "Tier generated name '" + tier.getName()
                            + "' contains characters that are invalid in Minecraft resource paths");
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

    private static boolean isValidGeneratedPath(String name) {
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.' || c == '/') {
                continue;
            }
            return false;
        }
        return true;
    }

    private static void validateColor(String field, int color, String tierName) {
        if (color < 0 || color > 0xFFFFFF) {
            throw new IllegalArgumentException(
                    field + " for tier '" + tierName + "' must be a 24-bit RGB value (0x000000-0xFFFFFF)");
        }
    }

    public static Tier get(ResourceLocation id) {
        return TIERS.get(id);
    }

    /**
     * Resolve the canonical registered id for a tier.
     *
     * @return the id, or {@code null} when the tier is not registered
     */
    public static ResourceLocation getId(Tier tier) {
        if (tier == null) {
            return null;
        }
        for (Map.Entry<ResourceLocation, Tier> entry : TIERS.entrySet()) {
            if (entry.getValue().equals(tier)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static int getMaxPossibleRPM() {
        return TIERS.values().stream()
                .mapToInt(Tier::getMaxRPM)
                .max()
                .orElse(0);
    }

    /**
     * Registered tiers in derived capability order: Max RPM, then Max SU, then id.
     */
    public static Collection<Tier> getAllTiers() {
        List<Map.Entry<ResourceLocation, Tier>> entries = new ArrayList<>(TIERS.entrySet());
        entries.sort(CAPABILITY_ORDER);
        return entries.stream().map(Map.Entry::getValue).toList();
    }

    public static Set<ResourceLocation> getAllTierIds() {
        return Collections.unmodifiableSet(TIERS.keySet());
    }

    public static boolean exists(ResourceLocation id) {
        return TIERS.containsKey(id);
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
        frozen = false;
        LOGGER.warn("Tier registry cleared - this should only be used for testing!");
    }
}
