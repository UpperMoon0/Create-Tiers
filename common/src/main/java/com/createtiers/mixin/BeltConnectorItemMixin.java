package com.createtiers.mixin;

import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
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
 * Preserves intrinsic shaft tiers when Create destroys pulley shafts and replaces them with belt blocks.
 */
@Mixin(value = BeltConnectorItem.class, remap = false)
public abstract class BeltConnectorItemMixin {

    @Unique
    private static final ThreadLocal<Map<BlockPos, Tier>> CREATETIERS$PULLEY_TIERS =
            ThreadLocal.withInitial(HashMap::new);

    @Redirect(
            method = "createBelts",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;destroyBlock(Lnet/minecraft/core/BlockPos;Z)Z",
                    ordinal = 0))
    private static boolean createtiers$captureTierBeforePulleyDestroy(Level level, BlockPos pos, boolean drop) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof TieredShaftBlock shaft) {
            CREATETIERS$PULLEY_TIERS.get().put(pos.immutable(), shaft.getTier());
        }
        return level.destroyBlock(pos, drop);
    }

    @Redirect(
            method = "createBelts",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;switchToBlockState(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private static void createtiers$restoreTierAfterBeltReplacement(Level level, BlockPos pos, BlockState newState) {
        Tier tier = CREATETIERS$PULLEY_TIERS.get().remove(pos);
        KineticBlockEntity.switchToBlockState(level, pos, newState);

        if (tier == null || level.isClientSide || !AllBlocks.BELT.has(level.getBlockState(pos))) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof IAttachedTierBlockEntity attachable) {
            attachable.setAttachedTier(tier);
        }
    }

    @Inject(method = "createBelts", at = @At("RETURN"))
    private static void createtiers$clearCapturedPulleyTiers(Level level, BlockPos start, BlockPos end,
            CallbackInfo ci) {
        CREATETIERS$PULLEY_TIERS.remove();
    }
}
