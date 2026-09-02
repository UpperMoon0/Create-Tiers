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

    /**
     * Create also uses this helper for non-RotatingInstance kinetic visuals such as scrolling belts.
     * Keep the kinetic debugger's network color, otherwise expose the attached tier color.
     */
    @Inject(method = "colorFromBE", at = @At("HEAD"), cancellable = true)
    private static void createtiers$attachedTierColorFromBlockEntity(KineticBlockEntity blockEntity,
            CallbackInfoReturnable<Integer> cir) {
        if (KineticDebugger.isActive()) {
            return;
        }

        Color color = AttachedTierVisuals.getRenderedColor(blockEntity);
        if (color != null) {
            cir.setReturnValue(color.getRGB());
        }
    }

    @Inject(
            method = "setup(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;Lnet/minecraft/core/Direction$Axis;F)Lcom/simibubi/create/content/kinetics/base/RotatingInstance;",
            at = @At("RETURN"))
    private void createtiers$rememberBlockEntity(KineticBlockEntity blockEntity, Direction.Axis axis, float speed,
            CallbackInfoReturnable<RotatingInstance> cir) {
        createtiers$blockEntity = blockEntity;
        createtiers$applyAttachedTierColor(cir.getReturnValue());
    }

    @Inject(
            method = "setColor(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)Lcom/simibubi/create/content/kinetics/base/RotatingInstance;",
            at = @At("HEAD"), cancellable = true)
    private void createtiers$keepAttachedTierColorFromBlockEntity(KineticBlockEntity blockEntity,
            CallbackInfoReturnable<RotatingInstance> cir) {
        createtiers$blockEntity = blockEntity;
        if (KineticDebugger.isActive()) {
            return;
        }

        Color color = AttachedTierVisuals.getRenderedColor(blockEntity);
        if (color != null) {
            createtiers$setColorAndReturn(color, cir);
        }
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
        if (color != null) {
            createtiers$setColorAndReturn(color, cir);
        }
    }

    @Unique
    private void createtiers$setColorAndReturn(Color color, CallbackInfoReturnable<RotatingInstance> cir) {
        RotatingInstance self = (RotatingInstance) (Object) this;
        self.color(color.getRed(), color.getGreen(), color.getBlue());
        self.setChanged();
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
