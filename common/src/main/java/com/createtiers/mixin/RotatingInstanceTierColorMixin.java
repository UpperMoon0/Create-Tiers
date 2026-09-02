package com.createtiers.mixin;

import com.createtiers.client.AttachedTierVisuals;
import com.simibubi.create.content.kinetics.KineticDebugger;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import net.createmod.catnip.theme.Color;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies attached-tier color whenever Flywheel configures a rotating kinetic instance. */
@Mixin(value = RotatingInstance.class, remap = false)
public abstract class RotatingInstanceTierColorMixin {

    @Inject(
            method = "setup(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;Lnet/minecraft/core/Direction$Axis;F)Lcom/simibubi/create/content/kinetics/base/RotatingInstance;",
            at = @At("RETURN"))
    private void createtiers$applyAttachedTierColor(KineticBlockEntity blockEntity, Direction.Axis axis, float speed,
            CallbackInfoReturnable<RotatingInstance> cir) {
        if (KineticDebugger.isActive()) {
            return;
        }

        Color color = AttachedTierVisuals.getRenderedColor(blockEntity);
        if (color == null) {
            return;
        }

        cir.getReturnValue().setColor(color).setChanged();
    }
}
