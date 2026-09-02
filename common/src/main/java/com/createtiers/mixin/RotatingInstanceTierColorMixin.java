package com.createtiers.mixin;

import com.createtiers.client.AttachedTierVisuals;
import com.simibubi.create.content.kinetics.KineticDebugger;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import net.createmod.catnip.theme.Color;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Carries the owning kinetic block entity onto each Flywheel rotating instance and keeps
 * attached-tier tint authoritative whenever Create later updates that instance's color.
 */
@Mixin(value = RotatingInstance.class, remap = false)
public abstract class RotatingInstanceTierColorMixin {

    @Unique
    private KineticBlockEntity createtiers$blockEntity;

    @Inject(
            method = "setup(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;Lnet/minecraft/core/Direction$Axis;F)Lcom/simibubi/create/content/kinetics/base/RotatingInstance;",
            at = @At("RETURN"))
    private void createtiers$rememberBlockEntity(KineticBlockEntity blockEntity, Direction.Axis axis, float speed,
            CallbackInfoReturnable<RotatingInstance> cir) {
        createtiers$blockEntity = blockEntity;
        createtiers$applyAttachedTierColor(cir.getReturnValue());
    }

    @Inject(
            method = "setColor(Lnet/createmod/catnip/theme/Color;)Lcom/simibubi/create/content/kinetics/base/RotatingInstance;",
            at = @At("HEAD"), cancellable = true)
    private void createtiers$keepAttachedTierColor(Color requestedColor,
            CallbackInfoReturnable<RotatingInstance> cir) {
        if (KineticDebugger.isActive() || createtiers$blockEntity == null) {
            return;
        }

        Color color = AttachedTierVisuals.getRenderedColor(createtiers$blockEntity);
        if (color == null) {
            return;
        }

        RotatingInstance self = (RotatingInstance) (Object) this;
        self.color(color.getRed(), color.getGreen(), color.getBlue());
        cir.setReturnValue(self);
    }

    @Unique
    private void createtiers$applyAttachedTierColor(RotatingInstance instance) {
        if (KineticDebugger.isActive() || createtiers$blockEntity == null) {
            return;
        }

        Color color = AttachedTierVisuals.getRenderedColor(createtiers$blockEntity);
        if (color == null) {
            return;
        }

        instance.color(color.getRed(), color.getGreen(), color.getBlue());
        instance.setChanged();
    }
}
