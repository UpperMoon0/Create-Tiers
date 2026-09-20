package com.createtiers.mixin;

import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.IReplacementSourceBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

/**
 * Preserves intrinsic and attached shaft tiers when Create destroys pulley shafts and replaces them with belt blocks.
 */
@Mixin(value = BeltConnectorItem.class, remap = false)
public abstract class BeltConnectorItemMixin {

    @Unique
    private record PulleySource(Tier tier, ResourceLocation blockId) {
    }

    @Unique
    private static final ThreadLocal<Map<BlockPos, PulleySource>> CREATETIERS$PULLEY_SOURCES =
            ThreadLocal.withInitial(HashMap::new);

    @Redirect(
            method = "createBelts",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;Z)Z",
                    ordinal = 0))
    private static boolean createtiers$captureTierBeforePulleyDestroy(Level level, BlockPos pos, boolean drop) {
        BlockState state = level.getBlockState(pos);
        Tier tier = null;

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof IAttachedTierBlockEntity attachable) {
            tier = attachable.getAttachedTier();
        }
        if (tier == null && state.getBlock() instanceof TieredShaftBlock shaft) {
            tier = shaft.getTier();
        }
        if (tier != null) {
            ResourceLocation sourceBlockId = BuiltInRegistries.BLOCK.getKey(state.getBlock());
            CREATETIERS$PULLEY_SOURCES.get().put(
                    pos.immutable(), new PulleySource(tier, sourceBlockId));
        }
        return level.destroyBlock(pos, drop);
    }

    @Redirect(
            method = "createBelts",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;switchToBlockState(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private static void createtiers$restoreTierAfterBeltReplacement(Level level, BlockPos pos, BlockState newState) {
        PulleySource source = CREATETIERS$PULLEY_SOURCES.get().remove(pos);
        KineticBlockEntity.switchToBlockState(level, pos, newState);

        if (source == null || level.isClientSide || !AllBlocks.BELT.has(level.getBlockState(pos))) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof IReplacementSourceBlockEntity replacementSource) {
            replacementSource.setCreateTiersReplacementSourceBlockId(source.blockId());
        }

        Block sourceBlock = BuiltInRegistries.BLOCK.get(source.blockId());
        boolean intrinsicSource = sourceBlock instanceof TieredShaftBlock
                && source.blockId().equals(BuiltInRegistries.BLOCK.getKey(sourceBlock));
        if (!intrinsicSource && blockEntity instanceof IAttachedTierBlockEntity attachable) {
            attachable.setAttachedTier(source.tier());
        }
    }

    @Inject(method = "createBelts", at = @At("RETURN"))
    private static void createtiers$clearCapturedPulleyTiers(Level level, BlockPos start, BlockPos end,
            CallbackInfo ci) {
        CREATETIERS$PULLEY_SOURCES.remove();
    }
}
