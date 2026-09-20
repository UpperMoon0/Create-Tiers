package com.createtiers.mixin;

import com.createtiers.content.kinetics.TieredPoweredShaftBlock;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.createtiers.foundation.utility.AttachedTierTransfer;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps a tiered shaft tiered while Create converts it into an engine-powered shaft. */
@Mixin(value = PoweredShaftBlock.class, remap = false)
public abstract class PoweredShaftBlockMixin {

    @Inject(method = "tick", at = @At("HEAD"))
    private void createtiers$captureAttachedTierBeforePoweredShaftRevert(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        AttachedTierTransfer.begin(level, pos);
    }

    @Inject(method = "tick", at = @At("RETURN"))
    private void createtiers$restoreAttachedTierAfterPoweredShaftRevert(
            BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        AttachedTierTransfer.end(level, pos);
    }

    @Inject(method = "getEquivalent", at = @At("HEAD"), cancellable = true)
    private static void createtiers$tieredEquivalent(BlockState state,
            CallbackInfoReturnable<BlockState> cir) {
        if (state.getBlock() instanceof TieredPoweredShaftBlock) {
            cir.setReturnValue(state);
            return;
        }
        if (!(state.getBlock() instanceof TieredShaftBlock shaft)) {
            return;
        }

        TieredPoweredShaftBlock powered = TieredPoweredShaftBlock.forTier(shaft.getTier());
        if (powered == null) {
            return;
        }

        cir.setReturnValue(powered.defaultBlockState()
                .setValue(PoweredShaftBlock.AXIS, state.getValue(PoweredShaftBlock.AXIS))
                .setValue(PoweredShaftBlock.WATERLOGGED, state.getValue(PoweredShaftBlock.WATERLOGGED)));
    }
}
