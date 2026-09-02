package com.createtiers.mixin;

import com.createtiers.client.AttachedTierVisuals;
import com.simibubi.create.content.kinetics.KineticDebugger;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.theme.Color;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Applies attached-tier color to Create's non-Flywheel rotating render path. */
@Mixin(value = KineticBlockEntityRenderer.class, remap = false)
public abstract class KineticBlockEntityRendererTierColorMixin {

    @Inject(method = "kineticRotationTransform", at = @At("RETURN"))
    private static void createtiers$applyAttachedTierColor(SuperByteBuffer buffer, KineticBlockEntity blockEntity,
            Direction.Axis axis, float angle, int light, CallbackInfoReturnable<SuperByteBuffer> cir) {
        if (KineticDebugger.isActive()) {
            return;
        }

        Color color = AttachedTierVisuals.getRenderedColor(blockEntity);
        if (color != null) {
            cir.getReturnValue().color(color);
        }
    }
}
