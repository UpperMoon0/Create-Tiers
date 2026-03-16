package com.createtiers.mixin;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.gauge.GaugeBlock;
import com.createtiers.content.kinetics.TieredCogwheelBlock;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import net.createmod.catnip.config.ConfigBase;
import net.minecraft.world.level.block.Block;

@Mixin(value = RotationPropagator.class, remap = false)
public abstract class RotationPropagatorMixin {

    @org.spongepowered.asm.mixin.injection.Redirect(method = "propagateNewSource(Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;)V",
            at = @At(value = "INVOKE", target = "Lnet/createmod/catnip/config/ConfigBase$ConfigInt;get()Ljava/lang/Object;"))
    private static Object redirectMaxSpeed(net.createmod.catnip.config.ConfigBase.ConfigInt instance, KineticBlockEntity currentTE) {
        Integer original = (Integer) instance.get();
        float tierMax = getBlockMaxRPM(currentTE);
        if (tierMax > 0)
            return (int) Math.max(original, (int) tierMax);
        return original;
    }

    private static float getBlockMaxRPM(KineticBlockEntity be) {
        if (be == null) return 0;
        Block block = be.getBlockState().getBlock();
        if (block instanceof GaugeBlock) {
            return Float.MAX_VALUE;
        }
        if (block instanceof TieredShaftBlock shaft) {
            return shaft.getTier().getMaxRPM();
        }
        if (block instanceof TieredCogwheelBlock cog) {
            return cog.getTier().getMaxRPM();
        }
        return 0;
    }
}
