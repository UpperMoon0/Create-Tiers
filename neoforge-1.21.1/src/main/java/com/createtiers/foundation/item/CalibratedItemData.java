package com.createtiers.foundation.item;

import com.createtiers.api.ITieredBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import com.createtiers.api.TierUpgradeRegistry;
import com.createtiers.api.TieredNativeKineticBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.gauge.GaugeBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Version-specific item storage for registered tier-upgrade variants. */
public final class CalibratedItemData {
    public static final String TIER_KEY = "CreateTiersTier";

    private CalibratedItemData() {
    }

    public static ItemStack calibratedCopy(ItemStack stack, Tier tier) {
        ResourceLocation tierId = TierRegistry.getId(tier);
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (tierId == null) {
            throw new IllegalArgumentException("Cannot create an item with an unregistered Create Tiers tier");
        }
        if (!TierUpgradeRegistry.isRegistered(itemId, tierId)) {
            throw new IllegalArgumentException(
                    "Tier upgrade is not registered for item '" + itemId + "' and tier '" + tierId + "'");
        }

        ItemStack copy = stack.copy();
        copy.setCount(1);
        setTier(copy, tier);
        return copy;
    }

    public static void setTier(ItemStack stack, Tier tier) {
        setTier(stack, requireKineticType(stack), tier);
    }

    public static void setTier(ItemStack stack, BlockEntityType<?> type, Tier tier) {
        ResourceLocation id = TierRegistry.getId(tier);
        if (id == null) {
            throw new IllegalArgumentException("Cannot put an unregistered Create Tiers tier on an item");
        }

        CustomData existing = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        CompoundTag data = existing == null ? new CompoundTag() : existing.copyTag();
        data.putString(TIER_KEY, id.toString());
        BlockItem.setBlockEntityData(stack, type, data);
    }

    public static Tier getTier(ItemStack stack) {
        CustomData data = stack.get(DataComponents.BLOCK_ENTITY_DATA);
        if (data == null || !data.contains(TIER_KEY)) {
            return null;
        }
        ResourceLocation id = ResourceLocation.tryParse(data.copyTag().getString(TIER_KEY));
        return id == null ? null : TierRegistry.get(id);
    }

    private static BlockEntityType<?> requireKineticType(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            throw new IllegalArgumentException("Tier upgrade target must be a block item");
        }
        if (blockItem.getBlock() instanceof GaugeBlock) {
            throw new IllegalArgumentException("Create gauges are observation devices and cannot be tier-upgraded");
        }
        if (blockItem.getBlock() instanceof TieredNativeKineticBlock) {
            throw new IllegalArgumentException("Native Create Tiers relay blocks already have an intrinsic tier");
        }
        if (!(blockItem.getBlock() instanceof EntityBlock entityBlock)) {
            throw new IllegalArgumentException("Tier upgrade target must have a block entity");
        }

        BlockEntity blockEntity = entityBlock.newBlockEntity(BlockPos.ZERO, blockItem.getBlock().defaultBlockState());
        if (!(blockEntity instanceof KineticBlockEntity kinetic)) {
            throw new IllegalArgumentException("Tier upgrade target must be backed by a Create KineticBlockEntity");
        }
        if (kinetic instanceof ITieredBlockEntity tiered && tiered.getTier() != null) {
            throw new IllegalArgumentException("Native Create Tiers blocks already have an intrinsic tier");
        }
        return kinetic.getType();
    }
}
