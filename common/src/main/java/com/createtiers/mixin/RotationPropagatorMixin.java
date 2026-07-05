package com.createtiers.mixin;

import com.createtiers.api.ITieredBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.gauge.GaugeBlock;
import net.createmod.catnip.config.ConfigBase;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = RotationPropagator.class, remap = false)
public abstract class RotationPropagatorMixin {

    private static final ThreadLocal<KineticBlockEntity[]> CREATETIERS$OVERSPEED_TARGETS =
            ThreadLocal.withInitial(() -> new KineticBlockEntity[2]);

    @Shadow
    private static float getConveyedSpeed(KineticBlockEntity from, KineticBlockEntity to) {
        throw new AssertionError();
    }

    @Redirect(method = "propagateNewSource(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/RotationPropagator;getConveyedSpeed(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)F",
                    ordinal = 0))
    private static float createtiers$captureNewSpeedTarget(KineticBlockEntity from, KineticBlockEntity to) {
        CREATETIERS$OVERSPEED_TARGETS.get()[0] = to;
        return getConveyedSpeed(from, to);
    }

    @Redirect(method = "propagateNewSource(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/RotationPropagator;getConveyedSpeed(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)F",
                    ordinal = 1))
    private static float createtiers$captureOppositeSpeedTarget(KineticBlockEntity from, KineticBlockEntity to) {
        CREATETIERS$OVERSPEED_TARGETS.get()[1] = to;
        return getConveyedSpeed(from, to);
    }

    @Redirect(method = "propagateNewSource(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/createmod/catnip/config/ConfigBase$ConfigInt;get()Ljava/lang/Object;",
                    ordinal = 0))
    private static Object createtiers$newSpeedLimit(ConfigBase.ConfigInt instance) {
        return getAllowedRPM(CREATETIERS$OVERSPEED_TARGETS.get()[0], (Integer) instance.get());
    }

    @Redirect(method = "propagateNewSource(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/createmod/catnip/config/ConfigBase$ConfigInt;get()Ljava/lang/Object;",
                    ordinal = 1))
    private static Object createtiers$oppositeSpeedLimit(ConfigBase.ConfigInt instance) {
        return getAllowedRPM(CREATETIERS$OVERSPEED_TARGETS.get()[1], (Integer) instance.get());
    }

    private static int getAllowedRPM(KineticBlockEntity blockEntity, int createDefault) {
        if (blockEntity == null)
            return createDefault;

        Block block = blockEntity.getBlockState().getBlock();
        if (block instanceof GaugeBlock)
            return Integer.MAX_VALUE;

        if (blockEntity instanceof ITieredBlockEntity tieredBlockEntity) {
            Tier tier = tieredBlockEntity.getTier();
            if (tier != null)
                return tier.getMaxRPM();
        }

        return Math.max(createDefault, TierRegistry.getMaxPossibleRPM());
    }
}
