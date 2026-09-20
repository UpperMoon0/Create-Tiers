package com.createtiers.mixin;

import com.createtiers.PlatformHelper;
import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

/** Restores the original tiered pulley shaft when a Create belt chain is removed. */
@Mixin(value = BeltBlock.class, remap = false)
public abstract class BeltBlockMixin {

    @Unique
    private static final ThreadLocal<Map<BlockPos, Tier>> CREATETIERS$REMOVED_PULLEY_TIERS =
            ThreadLocal.withInitial(HashMap::new);

    @Redirect(
            method = "onRemove",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;removeBlockEntity(Lnet/minecraft/core/BlockPos;)V"))
    private void createtiers$captureTierBeforeBeltEntityRemoval(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof IAttachedTierBlockEntity attached && attached.getAttachedTier() != null) {
            CREATETIERS$REMOVED_PULLEY_TIERS.get().put(pos.immutable(), attached.getAttachedTier());
        }
        level.removeBlockEntity(pos);
    }

    @Redirect(
            method = "onRemove",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean createtiers$restoreTieredPulley(Level level, BlockPos pos, BlockState requestedState, int flags) {
        Tier tier = CREATETIERS$REMOVED_PULLEY_TIERS.get().remove(pos);
        if (tier == null || !AllBlocks.SHAFT.has(requestedState)) {
            return level.setBlock(pos, requestedState, flags);
        }

        TieredShaftBlock shaft = createtiers$findTieredShaft(tier);
        if (shaft == null) {
            return level.setBlock(pos, requestedState, flags);
        }

        BlockState replacement = shaft.defaultBlockState()
                .setValue(ShaftBlock.AXIS, requestedState.getValue(ShaftBlock.AXIS));
        if (requestedState.hasProperty(ShaftBlock.WATERLOGGED)) {
            replacement = replacement.setValue(ShaftBlock.WATERLOGGED,
                    requestedState.getValue(ShaftBlock.WATERLOGGED));
        }
        return level.setBlock(pos, replacement, flags);
    }

    @Inject(method = "onRemove", at = @At("RETURN"))
    private void createtiers$clearRemovedPulleyTiers(BlockState state, Level level, BlockPos pos,
            BlockState newState, boolean movedByPiston, CallbackInfo ci) {
        CREATETIERS$REMOVED_PULLEY_TIERS.remove();
    }

    @Unique
    private static TieredShaftBlock createtiers$findTieredShaft(Tier tier) {
        for (Block block : PlatformHelper.get().getShafts()) {
            if (block instanceof TieredShaftBlock shaft && shaft.getTier().equals(tier)) {
                return shaft;
            }
        }
        return null;
    }
}
