package com.createtiers.mixin;

import com.createtiers.client.AllTieredPartialModels;
import com.createtiers.client.AttachedTierVisuals;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.KineticDebugger;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.foundation.blockEntity.renderer.SafeBlockEntityRenderer;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a small colored top-edge accent to calibrated ordinary Create kinetic machines.
 * This remains visible even when a specialized renderer does not expose a tintable rotating part.
 */
@Mixin(value = SafeBlockEntityRenderer.class, remap = false)
public abstract class SafeBlockEntityRendererTierAccentMixin {

    @Inject(method = "render", at = @At("TAIL"))
    private void createtiers$renderAttachedTierAccent(BlockEntity blockEntity, float partialTicks, PoseStack poseStack,
            MultiBufferSource bufferSource, int light, int overlay, CallbackInfo ci) {
        if (KineticDebugger.isActive() || !(blockEntity instanceof KineticBlockEntity kinetic)) {
            return;
        }

        Color color = AttachedTierVisuals.getRenderedColor(kinetic);
        if (color == null) {
            return;
        }

        SuperByteBuffer accent = CachedBuffers.partial(AllTieredPartialModels.ATTACHED_TIER_ACCENT,
                blockEntity.getBlockState());
        accent.light(light);
        accent.color(color);
        accent.renderInto(poseStack, bufferSource.getBuffer(RenderType.solid()));
    }
}
