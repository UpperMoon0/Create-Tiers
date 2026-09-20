package com.createtiers.mixin;

import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.visual.AbstractBlockEntityVisual;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = AbstractBlockEntityVisual.class, remap = false)
public interface AbstractBlockEntityVisualAccessor {

    @Invoker("relight")
    void createtiers$relight(FlatLit... instances);
}
