package com.createtiers.mixin;

import com.createtiers.PlatformHelper;
import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.IReplacementSourceBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.createtiers.foundation.utility.AttachedTierTransfer;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
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
    private record PulleySource(Tier tier, ResourceLocation blockId) {
    }

    @Unique
    private static final ThreadLocal<Map<BlockPos, PulleySource>> CREATETIERS$REMOVED_PULLEY_SOURCES =
            ThreadLocal.withInitial(HashMap::new);

    @Redirect(
            method = "onRemove",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;removeBlockEntity(Lnet/minecraft/core/BlockPos;)V"))
    private void createtiers$captureTierBeforeBeltEntityRemoval(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof IAttachedTierBlockEntity tiered) {
            ResourceLocation sourceBlockId = blockEntity instanceof IReplacementSourceBlockEntity source
                    ? source.getCreateTiersReplacementSourceBlockId()
                    : null;
            Tier effectiveTier = tiered.getTier();
            if (effectiveTier != null || sourceBlockId != null) {
                CREATETIERS$REMOVED_PULLEY_SOURCES.get().put(
                        pos.immutable(), new PulleySource(effectiveTier, sourceBlockId));
            }
        }
        level.removeBlockEntity(pos);
    }

    @Redirect(
            method = "onRemove",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean createtiers$restoreTieredPulley(Level level, BlockPos pos, BlockState requestedState, int flags) {
        PulleySource source = CREATETIERS$REMOVED_PULLEY_SOURCES.get().remove(pos);
        if (source == null || !AllBlocks.SHAFT.has(requestedState)) {
            return level.setBlock(pos, requestedState, flags);
        }

        Block sourceBlock = createtiers$resolveSourceBlock(source.blockId());
        if (sourceBlock == AllBlocks.SHAFT.get()) {
            boolean changed = level.setBlock(pos, requestedState, flags);
            AttachedTierTransfer.restore(level, pos, source.tier(), source.blockId());
            return changed;
        }

        if (sourceBlock instanceof TieredShaftBlock shaft) {
            return level.setBlock(pos, createtiers$copyShaftState(shaft.defaultBlockState(), requestedState), flags);
        }

        boolean changed = level.setBlock(pos, requestedState, flags);
        AttachedTierTransfer.restore(level, pos, source.tier(), source.blockId());
        return changed;
    }

    @Inject(method = "onRemove", at = @At("RETURN"))
    private void createtiers$clearRemovedPulleyTiers(BlockState state, Level level, BlockPos pos,
            BlockState newState, boolean movedByPiston, CallbackInfo ci) {
        CREATETIERS$REMOVED_PULLEY_SOURCES.remove();
    }

    @Unique
    private static Block createtiers$resolveSourceBlock(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        Block block = BuiltInRegistries.BLOCK.get(id);
        return id.equals(BuiltInRegistries.BLOCK.getKey(block)) ? block : null;
    }

    @Unique
    private static BlockState createtiers$copyShaftState(BlockState target, BlockState requestedState) {
        BlockState replacement = target.setValue(
                ShaftBlock.AXIS, requestedState.getValue(ShaftBlock.AXIS));
        if (requestedState.hasProperty(ShaftBlock.WATERLOGGED)
                && replacement.hasProperty(ShaftBlock.WATERLOGGED)) {
            replacement = replacement.setValue(
                    ShaftBlock.WATERLOGGED, requestedState.getValue(ShaftBlock.WATERLOGGED));
        }
        return replacement;
    }

}
