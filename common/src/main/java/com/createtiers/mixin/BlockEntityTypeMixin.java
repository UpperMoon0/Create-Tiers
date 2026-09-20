package com.createtiers.mixin;

import com.createtiers.api.TieredNativeKineticBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Allows a generated native Create Tiers relay block to reuse exactly the upstream Create
 * block-entity type it declares. No other foreign blocks are accepted by that type.
 */
@Mixin(BlockEntityType.class)
public abstract class BlockEntityTypeMixin {

    @Inject(method = "isValid", at = @At("HEAD"), cancellable = true)
    private void createtiers$acceptDeclaredNativeTierBlock(BlockState state,
            CallbackInfoReturnable<Boolean> cir) {
        if (state.getBlock() instanceof TieredNativeKineticBlock tiered
                && tiered.getExpectedBlockEntityType() == (Object) this) {
            cir.setReturnValue(true);
        }
    }
}
