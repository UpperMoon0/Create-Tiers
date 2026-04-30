package com.createtiers;

import net.minecraft.resources.ResourceLocation;

import java.util.function.BiFunction;

public final class Compat {

    private Compat() {}

    private static BiFunction<String, String, ResourceLocation> FACTORY;

    public static void init(BiFunction<String, String, ResourceLocation> factory) {
        FACTORY = factory;
    }

    public static ResourceLocation rl(String namespace, String path) {
        if (FACTORY == null) {
            throw new IllegalStateException("Compat not initialized. Call Compat.init() from your mod entry point.");
        }
        return FACTORY.apply(namespace, path);
    }

    public static ResourceLocation withSuffix(ResourceLocation loc, String suffix) {
        return rl(loc.getNamespace(), loc.getPath() + suffix);
    }
}
