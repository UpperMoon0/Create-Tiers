package com.createtiers.api;

import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.gauge.GaugeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;

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

    /**
     * Resolve and validate one upgrade target. Call only after item/block registries are stable.
     */
    public static void validateTarget(ResourceLocation itemId) {
        Item item = BuiltInRegistries.ITEM.get(itemId);
        if (!itemId.equals(BuiltInRegistries.ITEM.getKey(item))) {
            throw new IllegalArgumentException("Unknown tier upgrade item '" + itemId + "'");
        }
        if (!(item instanceof BlockItem blockItem)) {
            throw new IllegalArgumentException(
                    "Tier upgrade item '" + itemId + "' must be a block item");
        }

        var block = blockItem.getBlock();
        if (block instanceof GaugeBlock) {
            throw new IllegalArgumentException(
                    "Tier upgrade item '" + itemId + "' is a Create gauge; observation devices cannot be tier-upgraded");
        }
        if (block instanceof TieredNativeKineticBlock) {
            throw new IllegalArgumentException(
                    "Tier upgrade item '" + itemId + "' is already an intrinsic Create Tiers block");
        }
        if (!(block instanceof EntityBlock entityBlock)) {
            throw new IllegalArgumentException(
                    "Tier upgrade item '" + itemId + "' must have a block entity");
        }

        BlockEntity blockEntity = entityBlock.newBlockEntity(BlockPos.ZERO, block.defaultBlockState());
        if (!(blockEntity instanceof KineticBlockEntity kinetic)) {
            throw new IllegalArgumentException(
                    "Tier upgrade item '" + itemId + "' must be backed by a Create KineticBlockEntity");
        }
        if (kinetic instanceof ITieredBlockEntity tiered && tiered.getTier() != null) {
            throw new IllegalArgumentException(
                    "Tier upgrade item '" + itemId + "' already has an intrinsic Create Tiers tier");
        }
    }

    /**
     * Validate every registered target against the finalized Minecraft/Create registries.
     * Loader common-setup hooks call this before gameplay or generated recipes can consume
     * the registrations.
     */
    public static synchronized void validateTargets() {
        for (Registration registration : UPGRADES.values()) {
            validateTarget(registration.itemId());
        }
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
