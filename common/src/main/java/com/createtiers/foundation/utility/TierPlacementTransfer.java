package com.createtiers.foundation.utility;

import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.IReplacementSourceBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.foundation.item.TierUpgradeItemData;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Replays Create Tiers' registered tier payload after Catnip PlacementOffset
 * creates a kinetic block without running BlockItem's BE-data placement path.
 */
public final class TierPlacementTransfer {

    private TierPlacementTransfer() {
    }

    public static void apply(Level level, BlockPos pos, ItemStack placedFrom) {
        if (level.isClientSide || placedFrom.isEmpty()
                || !(placedFrom.getItem() instanceof BlockItem sourceItem)) {
            return;
        }

        Tier tier = TierUpgradeItemData.getTier(placedFrom);
        if (tier == null) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof KineticBlockEntity kinetic)
                || !(blockEntity instanceof IAttachedTierBlockEntity attachable)) {
            return;
        }

        Block sourceBlock = sourceItem.getBlock();
        if (kinetic.getBlockState().getBlock() != sourceBlock) {
            ResourceLocation sourceId = BuiltInRegistries.BLOCK.getKey(sourceBlock);
            if (!ReplacementSourcePolicy.isLegal(kinetic.getBlockState(), sourceId)
                    || !(blockEntity instanceof IReplacementSourceBlockEntity provenance)) {
                return;
            }
            provenance.setCreateTiersReplacementSourceBlockId(sourceId);
        }

        if (AttachedTierAuthorization.canCarry(kinetic, tier)
                && !tier.equals(attachable.getAttachedTier())) {
            attachable.setAttachedTier(tier);
        }
    }

}
