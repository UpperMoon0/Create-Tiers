package com.createtiers.mixin;

import com.createtiers.foundation.utility.TierCalibration;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds the tier-calibration secondary use to existing tiered shaft BlockItems. */
@Mixin(BlockItem.class)
public abstract class TieredShaftCalibrationMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void createtiers$calibrateKineticComponent(UseOnContext context,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (TierCalibration.tryCalibrate(context)) {
            cir.setReturnValue(InteractionResult.sidedSuccess(context.getLevel().isClientSide));
        }
    }
}
