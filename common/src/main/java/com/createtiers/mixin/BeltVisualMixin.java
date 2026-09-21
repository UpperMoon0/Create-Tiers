package com.createtiers.mixin;

import com.createtiers.api.Tier;
import com.createtiers.client.AllTieredPartialModels;
import com.createtiers.client.AttachedTierVisuals;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltSlope;
import com.simibubi.create.content.kinetics.belt.BeltVisual;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.model.Models;
import dev.engine_room.flywheel.lib.transform.TransformStack;
import net.createmod.catnip.theme.Color;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Consumer;

/**
 * A tiered source shaft survives inside a Create belt pulley visually as well as logically:
 * keep Create's wooden pulley body unchanged and render the tiered shaft as a separate instance.
 */
@Mixin(value = BeltVisual.class, remap = false)
public abstract class BeltVisualMixin {

    @Shadow
    @Final
    @Mutable
    protected RotatingInstance pulley;

    @Unique
    private BeltBlockEntity createtiers$belt;

    @Unique
    private Tier createtiers$tier;

    @Unique
    private RotatingInstance createtiers$tieredShaft;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void createtiers$replaceTieredPulley(
            dev.engine_room.flywheel.api.visualization.VisualizationContext context,
            BeltBlockEntity blockEntity, float partialTick, CallbackInfo ci) {
        createtiers$belt = blockEntity;
        createtiers$tier = AttachedTierVisuals.getAttachedTier(blockEntity);

        if (createtiers$tier == null || !blockEntity.hasPulley() || pulley == null) {
            return;
        }

        AllTieredPartialModels.TieredPartials partials = AllTieredPartialModels.forTier(createtiers$tier);
        if (partials == null) {
            return;
        }

        // Create normally renders one mixed-material BELT_PULLEY model. Replace that instance
        // with an identical wood-only model using Create's exact getPulleyModel() transform.
        pulley.delete();
        Model woodBody = createtiers$createPulleyBodyModel(blockEntity);
        pulley = context.instancerProvider()
                .instancer(AllInstanceTypes.ROTATING, woodBody)
                .createInstance();
        pulley.setup(blockEntity)
                .setPosition(((BeltVisual) (Object) this).getVisualPosition())
                .setChanged();

        Direction.Axis axis = ((IRotate) blockEntity.getBlockState().getBlock())
                .getRotationAxis(blockEntity.getBlockState());

        createtiers$tieredShaft = context.instancerProvider()
                .instancer(AllInstanceTypes.ROTATING, Models.partial(partials.SHAFT))
                .createInstance();

        createtiers$tieredShaft
                .rotateToFace(Direction.UP, axis)
                .setup(blockEntity)
                .setPosition(((BeltVisual) (Object) this).getVisualPosition());

        createtiers$applyTieredShaftColor();
        createtiers$tieredShaft.setChanged();
    }

    @Unique
    private Model createtiers$createPulleyBodyModel(BeltBlockEntity blockEntity) {
        Direction orientation = blockEntity.getBlockState()
                .getValue(BeltBlock.HORIZONTAL_FACING)
                .getClockWise();
        if (blockEntity.getBlockState().getValue(BeltBlock.SLOPE) == BeltSlope.SIDEWAYS) {
            orientation = Direction.UP;
        }

        Direction.Axis axis = orientation.getAxis();
        return Models.partial(AllTieredPartialModels.BELT_PULLEY_BODY, axis,
                (Direction.Axis modelAxis, PoseStack poseStack) -> {
                    var transform = TransformStack.of(poseStack);
                    transform.center();
                    if (modelAxis == Direction.Axis.X) {
                        transform.rotateYDegrees(90);
                    }
                    if (modelAxis == Direction.Axis.Y) {
                        transform.rotateXDegrees(90);
                    }
                    transform.rotateXDegrees(90);
                    transform.uncenter();
                });
    }

    @Inject(method = "update", at = @At("RETURN"))
    private void createtiers$updateTieredShaft(float partialTick, CallbackInfo ci) {
        if (createtiers$tieredShaft == null) {
            return;
        }
        createtiers$tieredShaft.setup(createtiers$belt);
        createtiers$applyTieredShaftColor();
        createtiers$tieredShaft.setChanged();
    }

    @Inject(method = "updateLight", at = @At("RETURN"))
    private void createtiers$relightTieredShaft(float partialTick, CallbackInfo ci) {
        if (createtiers$tieredShaft != null) {
            ((AbstractBlockEntityVisualAccessor) (Object) this).createtiers$relight(createtiers$tieredShaft);
        }
    }

    @Inject(method = "_delete", at = @At("RETURN"))
    private void createtiers$deleteTieredShaft(CallbackInfo ci) {
        if (createtiers$tieredShaft != null) {
            createtiers$tieredShaft.delete();
        }
    }

    @Inject(method = "collectCrumblingInstances", at = @At("RETURN"))
    private void createtiers$collectTieredShaft(Consumer<Instance> consumer, CallbackInfo ci) {
        if (createtiers$tieredShaft != null) {
            consumer.accept(createtiers$tieredShaft);
        }
    }

    @Unique
    private void createtiers$applyTieredShaftColor() {
        Color color = AttachedTierVisuals.getRenderedColor(createtiers$belt);
        if (color != null && createtiers$tieredShaft != null) {
            createtiers$tieredShaft.setColor(color);
        }
    }
}
