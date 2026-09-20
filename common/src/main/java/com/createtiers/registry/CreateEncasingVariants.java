package com.createtiers.registry;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.encasing.EncasedBlock;
import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Discovers the standard Create encasing variants available in the running Create version.
 * Only Create-owned variants are mirrored automatically; addon encasings remain opt-in.
 */
public final class CreateEncasingVariants {

    public record Variant(String sourcePath, String casingKey, Block sourceBlock, Block casingBlock) {
    }

    private CreateEncasingVariants() {
    }

    public static List<Variant> shaftVariants() {
        return discover(AllBlocks.SHAFT.get(), "_encased_shaft",
                List.of(AllBlocks.ANDESITE_ENCASED_SHAFT.get(), AllBlocks.BRASS_ENCASED_SHAFT.get()));
    }

    public static List<Variant> cogwheelVariants() {
        return discover(AllBlocks.COGWHEEL.get(), "_encased_cogwheel",
                List.of(AllBlocks.ANDESITE_ENCASED_COGWHEEL.get(), AllBlocks.BRASS_ENCASED_COGWHEEL.get()));
    }

    public static List<Variant> largeCogwheelVariants() {
        return discover(AllBlocks.LARGE_COGWHEEL.get(), "_encased_large_cogwheel",
                List.of(AllBlocks.ANDESITE_ENCASED_LARGE_COGWHEEL.get(), AllBlocks.BRASS_ENCASED_LARGE_COGWHEEL.get()));
    }

    private static List<Variant> discover(Block base, String suffix, List<Block> fallback) {
        List<Block> candidates = new ArrayList<>(EncasingRegistry.getVariants(base));
        if (candidates.isEmpty()) {
            candidates.addAll(fallback);
        }

        List<Variant> variants = new ArrayList<>();
        for (Block block : candidates) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
            if (id == null || !"create".equals(id.getNamespace()) || !id.getPath().endsWith(suffix)
                    || !(block instanceof EncasedBlock encased)) {
                continue;
            }

            String casingKey = id.getPath().substring(0, id.getPath().length() - suffix.length());
            variants.add(new Variant(id.getPath(), casingKey, block, encased.getCasing()));
        }

        variants.sort(Comparator.comparing(Variant::sourcePath));
        return List.copyOf(variants);
    }
}
