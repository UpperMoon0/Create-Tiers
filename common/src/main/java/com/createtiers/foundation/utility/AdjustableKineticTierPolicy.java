package com.createtiers.foundation.utility;

import com.createtiers.api.Tier;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.scrollValue.ScrollValueBehaviour;
import com.simibubi.create.infrastructure.config.AllConfigs;

/**
 * Keeps Create components with their own adjustable-RPM controls aligned with
 * the component's effective Create Tiers limit.
 */
public final class AdjustableKineticTierPolicy {

    private AdjustableKineticTierPolicy() {
    }

    public static void refresh(KineticBlockEntity blockEntity, Tier tier) {
        if (blockEntity instanceof SpeedControllerBlockEntity controller && controller.targetSpeed != null) {
            int max = tier != null ? tier.getMaxRPM() : AllConfigs.server().kinetics.maxRotationSpeed.get();
            applyRange(controller.targetSpeed, max);
        }

        if (blockEntity instanceof CreativeMotorBlockEntity motor && motor.generatedSpeed != null) {
            int max = tier != null ? tier.getMaxRPM() : CreativeMotorBlockEntity.MAX_SPEED;
            applyRange(motor.generatedSpeed, max);
        }
    }

    private static void applyRange(ScrollValueBehaviour behaviour, int max) {
        int positiveMax = Math.max(1, max);
        behaviour.between(-positiveMax, positiveMax);
        behaviour.value = Math.max(-positiveMax, Math.min(positiveMax, behaviour.getValue()));
    }
}
