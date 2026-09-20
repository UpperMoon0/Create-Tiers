package com.createtiers.mixin;

import com.createtiers.content.kinetics.TieredPoweredShaftBlock;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.createtiers.foundation.utility.AttachedTierTransfer;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Opens Create's exact shaft checks for native tiered shafts and powered shafts. */
@Mixin(value = SteamEngineBlock.class, remap = false)
public abstract class SteamEngineBlockMixin {

    @Inject(method = "isShaftValid", at = @At("HEAD"), cancellable = true)
    private static void createtiers$acceptTieredShaft(BlockState engineState, BlockState shaftState,
            CallbackInfoReturnable<Boolean> cir) {
        Block block = shaftState.getBlock();
        if (!(block instanceof TieredShaftBlock) && !(block instanceof TieredPoweredShaftBlock)) {
            return;
        }

        cir.setReturnValue(shaftState.getValue(ShaftBlock.AXIS) != SteamEngineBlock.getFacing(engineState).getAxis());
    }

    @Inject(method = "onPlace", at = @At("HEAD"))
    private void createtiers$captureAttachedTierBeforePoweredShaftReplacement(
            BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston, CallbackInfo ci) {
        AttachedTierTransfer.begin(level, SteamEngineBlock.getShaftPos(state, pos));
    }

    @Inject(method = "onPlace", at = @At("RETURN"))
    private void createtiers$restoreAttachedTierAfterPoweredShaftReplacement(
            BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston, CallbackInfo ci) {
        AttachedTierTransfer.end(level, SteamEngineBlock.getShaftPos(state, pos));
    }

    @Inject(method = "onRemove", at = @At("RETURN"))
    private void createtiers$scheduleTieredPoweredShaftRevert(BlockState state, Level level, BlockPos pos,
            BlockState newState, boolean movedByPiston, CallbackInfo ci) {
        if (state.is(newState.getBlock())) {
            return;
        }

        BlockPos shaftPos = SteamEngineBlock.getShaftPos(state, pos);
        BlockState shaftState = level.getBlockState(shaftPos);
        if (shaftState.getBlock() instanceof TieredPoweredShaftBlock powered) {
            level.scheduleTick(shaftPos, powered, 1);
        }
    }
}
