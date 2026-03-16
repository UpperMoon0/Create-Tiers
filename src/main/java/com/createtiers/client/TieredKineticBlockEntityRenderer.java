package com.createtiers.client;

import com.createtiers.api.Tier;
import com.createtiers.content.kinetics.TieredCogwheelBlock;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.createtiers.mixin.KineticBlockEntityAccessor;
import com.createtiers.mixin.KineticEffectHandlerAccessor;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;
import com.simibubi.create.content.kinetics.base.KineticEffectHandler;
import com.simibubi.create.content.kinetics.simpleRelays.BracketedKineticBlockEntityRenderer;
import dev.engine_room.flywheel.api.visualization.VisualizationManager;
import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.createmod.catnip.theme.Color;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

public class TieredKineticBlockEntityRenderer<T extends KineticBlockEntity> extends KineticBlockEntityRenderer<T> {

    public TieredKineticBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(T be, float partialTicks, PoseStack ms, MultiBufferSource buffer, int light, int overlay) {
        if (VisualizationManager.supportsVisualization(be.getLevel())) return;

        BlockState state = be.getBlockState();
        if (state.getBlock() instanceof TieredShaftBlock shaftBlock) {
            renderShaft(be, shaftBlock, ms, buffer, light);
        } else if (state.getBlock() instanceof TieredCogwheelBlock cogBlock) {
            renderCogwheel(be, cogBlock, ms, buffer, light);
        } else {
            super.renderSafe(be, partialTicks, ms, buffer, light, overlay);
        }
    }

    private void renderShaft(T be, TieredShaftBlock block, PoseStack ms, MultiBufferSource buffer, int light) {
        BlockState renderState = be.getBlockState();
        SuperByteBuffer superBuffer = CachedBuffers.block(KINETIC_BLOCK, renderState);
        Direction.Axis axis = getRotationAxisOf(be);
        BlockPos pos = be.getBlockPos();
        if (pos == null) return;
        float angle = getAngleForBe(be, pos, axis);
        
        transformAndRender(be, superBuffer, axis, angle, light, block.getTier().getShaftColor(), ms, buffer.getBuffer(getRenderType(be, renderState)));
    }

    private void renderCogwheel(T be, TieredCogwheelBlock block, PoseStack ms, MultiBufferSource buffer, int light) {
        Tier tier = block.getTier();
        Direction.Axis axis = getRotationAxisOf(be);
        
        if (block.isLargeCog()) {
            Direction facing = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
            VertexConsumer vc = buffer.getBuffer(RenderType.solid());
            
            // Render wheel part
            SuperByteBuffer wheel = CachedBuffers.partialFacingVertical(AllPartialModels.SHAFTLESS_LARGE_COGWHEEL, be.getBlockState(), facing);
            float wheelAngle = getAngleForBe(be, be.getBlockPos(), axis);
            transformAndRender(be, wheel, axis, wheelAngle, light, tier.getCogwheelColor(), ms, vc);
            
            // Render shaft part
            float shaftAngle = getAngleForLargeCogShaft(be, axis);
            SuperByteBuffer shaft = CachedBuffers.partialFacingVertical(AllPartialModels.COGWHEEL_SHAFT, be.getBlockState(), facing);
            transformAndRender(be, shaft, axis, shaftAngle, light, tier.getShaftColor(), ms, vc);
        } else {
            // Small cogwheel
            Direction facing = Direction.fromAxisAndDirection(axis, Direction.AxisDirection.POSITIVE);
            VertexConsumer vc = buffer.getBuffer(RenderType.solid());
            
            // Render gear part
            dev.engine_room.flywheel.lib.model.baked.PartialModel gearModel = dev.engine_room.flywheel.lib.model.baked.PartialModel.of(new net.minecraft.resources.ResourceLocation(com.createtiers.CreateTiers.MOD_ID, "block/" + tier.getName() + "/cogwheel_shaftless"));
            SuperByteBuffer gear = CachedBuffers.partial(gearModel, be.getBlockState());
            float gearAngle = getAngleForBe(be, be.getBlockPos(), axis);
            transformAndRender(be, gear, axis, gearAngle, light, tier.getCogwheelColor(), ms, vc);
            
            // Render shaft part
            SuperByteBuffer shaft = CachedBuffers.partialFacingVertical(AllPartialModels.COGWHEEL_SHAFT, be.getBlockState(), facing);
            float shaftAngle = getAngleForBe(be, be.getBlockPos(), axis);
            transformAndRender(be, shaft, axis, shaftAngle, light, tier.getShaftColor(), ms, vc);
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
        
        // Use accessors for overstress effect
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
}
