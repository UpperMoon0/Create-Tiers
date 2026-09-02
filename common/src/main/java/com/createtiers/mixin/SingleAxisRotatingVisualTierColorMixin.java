package com.createtiers.mixin;

import com.createtiers.client.AttachedTierVisuals;
import com.simibubi.create.content.kinetics.KineticDebugger;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.base.SingleAxisRotatingVisual;
import net.createmod.catnip.theme.Color;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Re-applies tier color after Create's per-tick overstress coloring on common single-axis visuals. */
@Mixin(value = SingleAxisRotatingVisual.class, remap = false)
public abstract class SingleAxisRotatingVisualTierColorMixin {

    @Shadow
    @Final
    protected RotatingInstance rotatingModel;

    @Inject(method = "tick", at = @At("TAIL"))
    private void createtiers$keepAttachedTierColor(CallbackInfo ci) {
        if (KineticDebugger.isActive()) {
            return;
        }

        BlockEntity raw = ((AbstractBlockEntityVisualAccessor) this).createtiers$getBlockEntity();
        if (!(raw instanceof KineticBlockEntity blockEntity)) {
            return;
        }

        Color color = AttachedTierVisuals.getRenderedColor(blockEntity);
        if (color != null) {
            rotatingModel.setColor(color).setChanged();
        }
    }
}
