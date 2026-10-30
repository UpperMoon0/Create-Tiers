package com.createtiers.registry;

import com.createtiers.Compat;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;

/**
 * Do not read Create's Registrate holders during NeoForge's error-screen
 * resource reload if another mod failed construction and registration was skipped.
 *
 * This checks the vanilla registry, never the potentially unbound holder's get().
 * A later normal resource reload can still generate models after registration.
 */
public final class CreateRegistryReadiness {
    private static final List<String> REQUIRED_BLOCKS = List.of(
            "shaft", "cogwheel", "large_cogwheel",
            "andesite_encased_shaft", "brass_encased_shaft",
            "andesite_encased_cogwheel", "brass_encased_cogwheel",
            "andesite_encased_large_cogwheel", "brass_encased_large_cogwheel");

    private CreateRegistryReadiness() {
    }

    public static boolean canGenerateModels() {
        return REQUIRED_BLOCKS.stream()
                .allMatch(path -> BuiltInRegistries.BLOCK.containsKey(Compat.rl("create", path)));
    }
}
