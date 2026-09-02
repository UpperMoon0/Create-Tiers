package com.createtiers.mixin;

import com.createtiers.api.ITieredBlockEntity;
import com.createtiers.api.Tier;
import com.simibubi.create.content.kinetics.mechanicalArm.ArmBlockEntity;
import org.spongepowered.asm.mixin.Constant;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** Lets calibrated Mechanical Arms benefit from RPM tiers above Create's vanilla 256 RPM movement ceiling. */
@Mixin(value = ArmBlockEntity.class, remap = false)
public abstract class ArmBlockEntityMixin {

    @ModifyConstant(method = "tickMovementProgress()Z", constant = @Constant(intValue = 256))
    private int createtiers$tierMovementSpeedCeiling(int vanillaCeiling) {
        if ((Object) this instanceof ITieredBlockEntity tiered) {
            Tier tier = tiered.getTier();
            if (tier != null) {
                return tier.getMaxRPM();
            }
        }
        return vanillaCeiling;
    }
}
