package com.createtiers.mixin;

import com.createtiers.api.ITieredBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.api.TierLimitPolicy;
import com.createtiers.content.kinetics.TieredSpeedControllerBlock;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.gauge.GaugeBlock;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlock;
import net.createmod.catnip.config.ConfigBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = RotationPropagator.class, remap = false)
public abstract class RotationPropagatorMixin {

    /**
     * Create's speed-controller coupling checks the exact vanilla block entry.
     * Native Create Tiers controllers reuse Create's SpeedControllerBlockEntity,
     * so they must participate in the same dedicated large-cog connection.
     */
    @Inject(method = "isLargeCogToSpeedController", at = @At("HEAD"), cancellable = true)
    private static void createtiers$acceptTieredSpeedController(BlockState from, BlockState to, BlockPos diff,
            CallbackInfoReturnable<Boolean> cir) {
        if (!(to.getBlock() instanceof TieredSpeedControllerBlock)) {
            return;
        }

        if (!ICogWheel.isLargeCog(from) || !diff.equals(BlockPos.ZERO.below())) {
            cir.setReturnValue(false);
            return;
        }

        Axis cogAxis = from.getValue(CogWheelBlock.AXIS);
        if (cogAxis.isVertical() || to.getValue(SpeedControllerBlock.HORIZONTAL_AXIS) == cogAxis) {
            cir.setReturnValue(false);
            return;
        }

        cir.setReturnValue(true);
    }

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
