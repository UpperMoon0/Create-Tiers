package com.createtiers.mixin;

import com.createtiers.api.ITieredBlockEntity;
import com.createtiers.foundation.utility.AdjustableKineticTierPolicy;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Create constructs the controller's targetSpeed behaviour after the generic
 * KineticBlockEntity initialization hook. Refresh once that behaviour exists so
 * intrinsic native controllers immediately expose their tier RPM range.
 */
@Mixin(value = SpeedControllerBlockEntity.class, remap = false)
public abstract class SpeedControllerTierRangeMixin {

    @Inject(method = "addBehaviours", at = @At("TAIL"))
    private void createtiers$applyEffectiveTierRange(List<BlockEntityBehaviour> behaviours, CallbackInfo ci) {
        SpeedControllerBlockEntity self = (SpeedControllerBlockEntity) (Object) this;
        if (self instanceof ITieredBlockEntity tiered) {
            AdjustableKineticTierPolicy.refresh((KineticBlockEntity) self, tiered.getTier());
        }
    }
}
