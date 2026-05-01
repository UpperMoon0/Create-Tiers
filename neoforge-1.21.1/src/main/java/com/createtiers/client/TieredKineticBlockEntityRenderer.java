package com.createtiers.client;

import com.createtiers.api.Tier;
import com.createtiers.content.kinetics.TieredCogwheelBlock;
import com.createtiers.content.kinetics.TieredEncasedCogwheelBlock;
import com.createtiers.content.kinetics.TieredEncasedShaftBlock;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.createtiers.content.kinetics.TieredShaftBlockEntity;
import com.createtiers.content.kinetics.TieredCogwheelBlockEntity;
import com.createtiers.mixin.KineticBlockEntityAccessor;
import com.createtiers.mixin.KineticEffectHandlerAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.KineticEffectHandler;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import dev.engine_room.flywheel.lib.model.baked.PartialModel;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.Direction.AxisDirection;
import net.minecraft.world.level.block.state.BlockState;

public class TieredKineticBlockEntityRenderer<T extends KineticBlockEntity> extends KineticBlockEntityRenderer<T> {

    public TieredKineticBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(T be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) return;

        BlockState state = be.getBlockState();
        if (state.getBlock() instanceof TieredEncasedShaftBlock encasedShaft) {
            renderEncasedShaft(be, encasedShaft, ms, buffer, light);
        } else if (state.getBlock() instanceof TieredEncasedCogwheelBlock encasedCog) {
            renderEncasedCogwheel(be, encasedCog, ms, buffer, light);
        } else if (state.getBlock() instanceof TieredShaftBlock shaftBlock) {
            renderShaft(be, shaftBlock, ms, buffer, light);
        } else if (state.getBlock() instanceof TieredCogwheelBlock cogBlock) {
            renderCogwheel(be, cogBlock, ms, buffer, light);
        } else {
            super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        }
    }

    private void renderShaft(T be, TieredShaftBlock block, PoseStack ms, MultiBufferSource buffer, int light) {
        AllTieredPartialModels.TieredPartials partials = AllTieredPartialModels.forTier(block.getTier());
        SuperByteBuffer superBuffer = CachedBuffers.partial(partials.SHAFT, be.getBlockState());
        Direction.Axis axis = getRotationAxisOf(be);
        BlockPos pos = be.getBlockPos();
        if (pos == null) return;
        float angle = getAngleForBe(be, pos, axis);

        transformAndRender(be, superBuffer, axis, angle, light, block.getTier().getShaftColor(), ms, buffer.getBuffer(getRenderType(be, be.getBlockState())));
    }

    private void renderCogwheel(T be, TieredCogwheelBlock block, PoseStack ms, MultiBufferSource buffer, int light) {
        Tier tier = block.getTier();
        Direction.Axis axis = getRotationAxisOf(be);
        AllTieredPartialModels.TieredPartials partials = AllTieredPartialModels.forTier(tier);

        if (block.isLargeCog()) {
            VertexConsumer vc = buffer.getBuffer(RenderType.solid());

            SuperByteBuffer wheel = CachedBuffers.partial(partials.LARGE_COGWHEEL_SHAFTLESS, be.getBlockState());
            float wheelAngle = getAngleForBe(be, be.getBlockPos(), axis);
            transformAndRender(be, wheel, axis, wheelAngle, light, tier.getCogwheelColor(), ms, vc);

            float shaftAngle = getAngleForLargeCogShaft(be, axis);
            SuperByteBuffer shaft = CachedBuffers.partial(partials.COGWHEEL_SHAFT, be.getBlockState());
            transformAndRender(be, shaft, axis, shaftAngle, light, tier.getShaftColor(), ms, vc);
        } else {
            VertexConsumer vc = buffer.getBuffer(RenderType.solid());

            SuperByteBuffer gear = CachedBuffers.partial(partials.COGWHEEL_SHAFTLESS, be.getBlockState());
            float gearAngle = getAngleForBe(be, be.getBlockPos(), axis);
            transformAndRender(be, gear, axis, gearAngle, light, tier.getCogwheelColor(), ms, vc);

            SuperByteBuffer shaft = CachedBuffers.partial(partials.COGWHEEL_SHAFT, be.getBlockState());
            float shaftAngle = getAngleForBe(be, be.getBlockPos(), axis);
            transformAndRender(be, shaft, axis, shaftAngle, light, tier.getShaftColor(), ms, vc);
        }
    }

    private void renderEncasedShaft(T be, TieredEncasedShaftBlock block, PoseStack ms, MultiBufferSource buffer, int light) {
        AllTieredPartialModels.TieredPartials partials = AllTieredPartialModels.forTier(block.getTier());
        SuperByteBuffer superBuffer = CachedBuffers.partial(partials.SHAFT, be.getBlockState());
        Direction.Axis axis = getRotationAxisOf(be);
        BlockPos pos = be.getBlockPos();
        if (pos == null) return;
        float angle = getAngleForBe(be, pos, axis);

        transformAndRender(be, superBuffer, axis, angle, light, block.getTier().getShaftColor(), ms, buffer.getBuffer(getRenderType(be, be.getBlockState())));
    }

    private void renderEncasedCogwheel(T be, TieredEncasedCogwheelBlock block, PoseStack ms, MultiBufferSource buffer, int light) {
        Tier tier = block.getTier();
        Direction.Axis axis = getRotationAxisOf(be);
        AllTieredPartialModels.TieredPartials partials = AllTieredPartialModels.forTier(tier);

        PartialModel shaftlessModel = block.isLargeCog() ? partials.LARGE_COGWHEEL_SHAFTLESS : partials.COGWHEEL_SHAFTLESS;

        VertexConsumer vc = buffer.getBuffer(RenderType.solid());

        Direction facing = Direction.fromAxisAndDirection(axis, AxisDirection.POSITIVE);
        SuperByteBuffer wheel = CachedBuffers.partialFacingVertical(shaftlessModel, be.getBlockState(), facing);
        float wheelAngle = block.isLargeCog()
                ? BracketedKineticBlockEntityRenderer.getAngleForLargeCogShaft((SimpleKineticBlockEntity) be, axis)
                : getAngleForBe(be, be.getBlockPos(), axis);
        kineticRotationTransform(wheel, be, axis, wheelAngle, light);
        applyColor(be, wheel, tier.getCogwheelColor());
        wheel.renderInto(ms, vc);

        float shaftAngle = block.isLargeCog()
                ? BracketedKineticBlockEntityRenderer.getAngleForLargeCogShaft((SimpleKineticBlockEntity) be, axis)
                : getAngleForBe(be, be.getBlockPos(), axis);

        for (Direction d : Iterate.directionsInAxis(axis)) {
            if (!block.hasShaftTowards(be.getLevel(), be.getBlockPos(), be.getBlockState(), d))
                continue;
            SuperByteBuffer shaft = CachedBuffers.partialFacing(AllTieredPartialModels.forTier(tier).SHAFT_HALF, be.getBlockState(), d);
            kineticRotationTransform(shaft, be, axis, shaftAngle, light);
            applyColor(be, shaft, tier.getShaftColor());
            shaft.renderInto(ms, buffer.getBuffer(RenderType.solid()));
        }
    }

    private float getAngleForLargeCogShaft(T be, Direction.Axis axis) {
        BlockPos pos = be.getBlockPos();
        if (pos == null || be.getLevel() == null) return 0;
        float offset = BracketedKineticBlockEntityRenderer.getShaftAngleOffset(axis, pos);
        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        return ((time * be.getSpeed() * 3f / 10 + offset) % 360) / 180 * (float) Math.PI;
    }

    private void transformAndRender(T be, SuperByteBuffer buffer, Direction.Axis axis, float angle, int light, int colorHex, PoseStack ms, VertexConsumer vc) {
        buffer.light(light);
        buffer.rotateCentered(angle, Direction.get(Direction.AxisDirection.POSITIVE, axis));

        Color tierColor = new Color(colorHex);

        float overStressedEffect = 0;
        KineticEffectHandler effects = ((KineticBlockEntityAccessor) be).getEffects();
        if (effects != null) {
            overStressedEffect = ((KineticEffectHandlerAccessor) effects).getOverStressedEffect();
        }

        if (overStressedEffect != 0) {
            boolean overstressed = overStressedEffect > 0;
            Color mixColor = overstressed ? Color.RED : Color.SPRING_GREEN;
            float weight = overstressed ? overStressedEffect : -overStressedEffect;
            buffer.color(tierColor.mixWith(mixColor, weight));
        } else {
            buffer.color(tierColor);
        }

        buffer.renderInto(ms, vc);
    }

    private void applyColor(T be, SuperByteBuffer buffer, int colorHex) {
        Color tierColor = new Color(colorHex);

        float overStressedEffect = 0;
        KineticEffectHandler effects = ((KineticBlockEntityAccessor) be).getEffects();
        if (effects != null) {
            overStressedEffect = ((KineticEffectHandlerAccessor) effects).getOverStressedEffect();
        }

        if (overStressedEffect != 0) {
            boolean overstressed = overStressedEffect > 0;
            Color mixColor = overstressed ? Color.RED : Color.SPRING_GREEN;
            float weight = overstressed ? overStressedEffect : -overStressedEffect;
            buffer.color(tierColor.mixWith(mixColor, weight));
        } else {
            buffer.color(tierColor);
        }
    }
}
