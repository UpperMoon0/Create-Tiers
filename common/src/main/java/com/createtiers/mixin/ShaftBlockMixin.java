package com.createtiers.mixin;

import com.createtiers.content.kinetics.TieredShaftBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Extends Create's exact vanilla-shaft predicate to native Create Tiers shafts. */
@Mixin(value = ShaftBlock.class, remap = false)
public abstract class ShaftBlockMixin {

    @Inject(method = "isShaft", at = @At("HEAD"), cancellable = true)
    private static void createtiers$recognizeTieredShaft(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof TieredShaftBlock) {
            cir.setReturnValue(true);
        }
    }
}
