package com.createtiers.mixin;

import com.simibubi.create.content.kinetics.base.KineticEffectHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(KineticEffectHandler.class)
public interface KineticEffectHandlerAccessor {
    @Accessor(remap = false)
    float getOverStressedEffect();
}
