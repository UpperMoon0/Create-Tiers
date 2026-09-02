package com.createtiers.mixin;

import com.createtiers.api.ITieredBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.api.TierLimitPolicy;
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

    private static final ThreadLocal<LimitTarget[]> CREATETIERS$OVERSPEED_TARGETS =
            ThreadLocal.withInitial(() -> new LimitTarget[2]);

    private record LimitTarget(Tier tier, boolean bypassLimit) {
    }

    @Shadow
    private static float getConveyedSpeed(KineticBlockEntity from, KineticBlockEntity to) {
        throw new AssertionError();
    }

    @Redirect(method = "propagateNewSource(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/RotationPropagator;getConveyedSpeed(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)F",
                    ordinal = 0))
    private static float createtiers$captureNewSpeedTarget(KineticBlockEntity from, KineticBlockEntity to) {
        CREATETIERS$OVERSPEED_TARGETS.get()[0] = resolveTarget(to);
        return getConveyedSpeed(from, to);
    }

    @Redirect(method = "propagateNewSource(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)V",
            at = @At(value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/RotationPropagator;getConveyedSpeed(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)F",
                    ordinal = 1))
    private static float createtiers$captureOppositeSpeedTarget(KineticBlockEntity from, KineticBlockEntity to) {
        CREATETIERS$OVERSPEED_TARGETS.get()[1] = resolveTarget(to);
        return getConveyedSpeed(from, to);
    }

    @Redirect(method = "propagateNewSource(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/createmod/catnip/config/ConfigBase$ConfigInt;get()Ljava/lang/Object;",
                    ordinal = 0))
    private static Object createtiers$newSpeedLimit(ConfigBase.ConfigInt instance) {
        return getAllowedRPM(consumeTarget(0), (Integer) instance.get());
    }

    @Redirect(method = "propagateNewSource(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)V",
            at = @At(value = "INVOKE",
                    target = "Lnet/createmod/catnip/config/ConfigBase$ConfigInt;get()Ljava/lang/Object;",
                    ordinal = 1))
    private static Object createtiers$oppositeSpeedLimit(ConfigBase.ConfigInt instance) {
        return getAllowedRPM(consumeTarget(1), (Integer) instance.get());
    }

    private static LimitTarget resolveTarget(KineticBlockEntity blockEntity) {
        Block block = blockEntity.getBlockState().getBlock();
        Tier tier = null;
        if (blockEntity instanceof ITieredBlockEntity tieredBlockEntity) {
            tier = tieredBlockEntity.getTier();
        }
        return new LimitTarget(tier, block instanceof GaugeBlock);
    }

    private static LimitTarget consumeTarget(int index) {
        LimitTarget[] targets = CREATETIERS$OVERSPEED_TARGETS.get();
        LimitTarget target = targets[index];
        targets[index] = null;
        return target;
    }

    private static int getAllowedRPM(LimitTarget target, int createDefault) {
        if (target == null) {
            return createDefault;
        }

        // The limit belongs to the component receiving the conveyed speed.
        // Untiered Create components must never inherit a higher registered tier limit.
        return TierLimitPolicy.allowedRPM(target.tier(), createDefault, target.bypassLimit());
    }
}
