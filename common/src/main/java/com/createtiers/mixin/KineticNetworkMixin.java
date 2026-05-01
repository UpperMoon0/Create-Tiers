package com.createtiers.mixin;

import com.createtiers.api.ITieredBlockEntity;
import com.createtiers.api.Tier;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

@Mixin(value = KineticNetwork.class, remap = false)
public abstract class KineticNetworkMixin {

    @Shadow
    public Map<KineticBlockEntity, Float> members;

    @Inject(method = "calculateCapacity", at = @At("RETURN"), cancellable = true)
    private void createtiers$enforceTierLimits(CallbackInfoReturnable<Float> cir) {
        float capacity = cir.getReturnValue();
        float minThroughput = Float.MAX_VALUE;
        boolean hasTieredBlock = false;

        for (KineticBlockEntity be : members.keySet()) {
            if (be instanceof ITieredBlockEntity tieredBe) {
                Tier tier = tieredBe.getTier();
                if (tier == null) continue;

                float speed = Math.abs(be.getTheoreticalSpeed());
                
                // RPM Limit
                if (speed > tier.getMaxRPM()) {
                    cir.setReturnValue(0f);
                    return;
                }

                // SU Hard Limit (Hard Cap)
                float limit = (float) tier.getMaxSU();
                if (limit < minThroughput) {
                    minThroughput = limit;
                }
                hasTieredBlock = true;
            }
        }

        if (hasTieredBlock && minThroughput < capacity) {
            cir.setReturnValue(minThroughput);
        }
    }
}
