package com.createtiers.mixin;

import com.createtiers.content.kinetics.TieredSpeedControllerBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlock;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mirrors Create's large-cog auto-alignment when the controller below is a
 * native Create Tiers speed controller instead of the exact vanilla block.
 */
@Mixin(value = CogWheelBlock.class, remap = false)
public abstract class CogWheelBlockSpeedControllerMixin {

    @Inject(method = "getAxisForPlacement", at = @At("HEAD"), cancellable = true)
    private void createtiers$alignAboveTieredSpeedController(BlockPlaceContext context,
            CallbackInfoReturnable<Axis> cir) {
        CogWheelBlock self = (CogWheelBlock) (Object) this;
        if (!self.isLargeCog()) {
            return;
        }
        if (context.getPlayer() != null && context.getPlayer().isShiftKeyDown()) {
            return;
        }

        BlockState below = context.getLevel().getBlockState(context.getClickedPos().below());
        if (!(below.getBlock() instanceof TieredSpeedControllerBlock)) {
            return;
        }

        cir.setReturnValue(below.getValue(SpeedControllerBlock.HORIZONTAL_AXIS) == Axis.X ? Axis.Z : Axis.X);
    }
}
