package com.createtiers.mixin;

import com.createtiers.foundation.utility.InWorldTierUpgrade;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Adds the in-world tier-upgrade path for Create kinetics without a normal item form. */
@Mixin(BlockItem.class)
public abstract class TieredShaftUpgradeMixin {

    @Inject(method = "useOn", at = @At("HEAD"), cancellable = true)
    private void createtiers$upgradeKineticComponent(UseOnContext context,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (InWorldTierUpgrade.tryApply(context)) {
            cir.setReturnValue(InteractionResult.sidedSuccess(context.getLevel().isClientSide));
        }
    }
}
