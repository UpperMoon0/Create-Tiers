package com.createtiers.foundation.utility;

import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.IReplacementSourceBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.createtiers.foundation.item.TierUpgradeItemData;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Tier-aware form of Create's "shaft item on middle belt" interaction. */
public final class TieredBeltPulleyInteraction {

    private record Source(Block block, Tier tier, boolean intrinsic) {
    }

    private TieredBeltPulleyInteraction() {
    }

    public static boolean tryAddPulley(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player) {
        if (player.isShiftKeyDown() || !player.mayBuild()
                || !AllBlocks.BELT.has(state)
                || state.getValue(BeltBlock.PART) != BeltPart.MIDDLE) {
            return false;
        }

        Source source = resolveSource(stack);
        if (source == null) {
            return false;
        }

        if (level.isClientSide) {
            return true;
        }

        BlockState pulleyState = state.setValue(BeltBlock.PART, BeltPart.PULLEY);
        KineticBlockEntity.switchToBlockState(level, pos, pulleyState);

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof IReplacementSourceBlockEntity provenance)) {
            throw new IllegalStateException("Create belt pulley has no replacement-source interface");
        }

        ResourceLocation sourceId = BuiltInRegistries.BLOCK.getKey(source.block());
        provenance.setCreateTiersReplacementSourceBlockId(sourceId);

        if (!source.intrinsic()) {
            if (!(blockEntity instanceof IAttachedTierBlockEntity attachable)) {
                throw new IllegalStateException("Create belt pulley has no attached-tier interface");
            }
            attachable.setAttachedTier(source.tier());
        }

        if (!player.isCreative()) {
            stack.shrink(1);
        }
        return true;
    }

    private static Source resolveSource(ItemStack stack) {
        if (!(stack.getItem() instanceof BlockItem item)) {
            return null;
        }

        Block block = item.getBlock();
        if (block instanceof TieredShaftBlock shaft) {
            return new Source(block, shaft.getTier(), true);
        }

        if (block != AllBlocks.SHAFT.get()) {
            return null;
        }

        Tier tier = TierUpgradeItemData.getTier(stack);
        return tier == null ? null : new Source(block, tier, false);
    }
}
