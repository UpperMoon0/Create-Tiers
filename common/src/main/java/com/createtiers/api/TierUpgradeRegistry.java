package com.createtiers.api;

import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Startup registry of item/tier variants that Create Tiers is allowed to create.
 *
 * <p>Registration authorizes a variant; it does not prescribe how a pack obtains it.
 * A registration may optionally request the built-in fallback recipe, while KubeJS
 * and other integrations can use the same registered output in any recipe type.</p>
 */
public final class TierUpgradeRegistry {

    public record Registration(ResourceLocation itemId, ResourceLocation tierId, boolean defaultRecipe) {
        public Registration {
            Objects.requireNonNull(itemId, "Tier upgrade item id cannot be null");
            Objects.requireNonNull(tierId, "Tier upgrade tier id cannot be null");
        }
    }

    private record Key(ResourceLocation itemId, ResourceLocation tierId) {
    }

    private static final Map<Key, Registration> UPGRADES = new LinkedHashMap<>();
    private static boolean frozen;

    private TierUpgradeRegistry() {
    }

    public static synchronized Registration register(ResourceLocation itemId, ResourceLocation tierId,
            boolean defaultRecipe) {
        Registration registration = new Registration(itemId, tierId, defaultRecipe);
        registerAll(List.of(registration));
        return registration;
    }

    /** Atomically register a collection of legal item/tier variants. */
    public static synchronized List<Registration> registerAll(Collection<Registration> registrations) {
        Objects.requireNonNull(registrations, "Tier upgrade registrations cannot be null");
        validateMutable();

        List<Registration> pending = new ArrayList<>(registrations);
        Set<Key> keys = new HashSet<>(UPGRADES.keySet());

        for (Registration registration : pending) {
            Objects.requireNonNull(registration, "Tier upgrade registration cannot be null");
            if (!TierRegistry.exists(registration.tierId())) {
                throw new IllegalArgumentException(
                        "Tier upgrade references unknown tier '" + registration.tierId() + "'");
            }
            Key key = key(registration);
            if (!keys.add(key)) {
                throw new IllegalArgumentException(
                        "Tier upgrade for item '" + registration.itemId() + "' and tier '"
                                + registration.tierId() + "' is already registered");
            }
        }

        for (Registration registration : pending) {
            UPGRADES.put(key(registration), registration);
        }
        return List.copyOf(pending);
    }

    private static Key key(Registration registration) {
        return new Key(registration.itemId(), registration.tierId());
    }

    private static void validateMutable() {
        if (frozen) {
            throw new IllegalStateException(
                    "Tier upgrade registry is frozen. Upgrade variants must be registered during startup scripts.");
        }
    }

    public static synchronized boolean isRegistered(ResourceLocation itemId, ResourceLocation tierId) {
        return UPGRADES.containsKey(new Key(itemId, tierId));
    }

    public static synchronized Registration get(ResourceLocation itemId, ResourceLocation tierId) {
        return UPGRADES.get(new Key(itemId, tierId));
    }

    public static synchronized List<Registration> getAll() {
        return List.copyOf(UPGRADES.values());
    }

    public static synchronized int size() {
        return UPGRADES.size();
    }

    public static synchronized void freeze() {
        frozen = true;
    }

    public static synchronized boolean isFrozen() {
        return frozen;
    }

    /** Testing only. */
    public static synchronized void unfreeze() {
        frozen = false;
    }

    /** Testing only. */
    public static synchronized void clear() {
        UPGRADES.clear();
        frozen = false;
    }
}
