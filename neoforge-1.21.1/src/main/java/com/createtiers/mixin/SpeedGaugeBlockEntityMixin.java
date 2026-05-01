package com.createtiers.mixin;

import com.createtiers.api.TierRegistry;
import com.simibubi.create.content.kinetics.gauge.SpeedGaugeBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.util.Mth;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;

@Mixin(value = SpeedGaugeBlockEntity.class, remap = false)
public abstract class SpeedGaugeBlockEntityMixin {

    /**
     * @author Kilo Code
     * @reason Scaling the speedometer dial to handle higher tiers RPM
     */
    @Overwrite
    public static float getDialTarget(float speed) {
        speed = Math.abs(speed);
        float medium = AllConfigs.server().kinetics.mediumSpeed.get().floatValue();
        float fast = AllConfigs.server().kinetics.fastSpeed.get().floatValue();
        
        // Use the maximum possible RPM from our tiers as the new "max" for the dial
        float max = Math.max(AllConfigs.server().kinetics.maxRotationSpeed.get().floatValue(), 
                             TierRegistry.getMaxPossibleRPM());
        
        float target = 0;
        if (speed == 0)
            target = 0;
        else if (speed < medium)
            target = Mth.lerp(speed / medium, 0, .45f);
        else if (speed < fast)
            target = Mth.lerp((speed - medium) / (fast - medium), .45f, .75f);
        else
            target = Mth.lerp((speed - fast) / (max - fast), .75f, 1.125f);
        return target;
    }
}
