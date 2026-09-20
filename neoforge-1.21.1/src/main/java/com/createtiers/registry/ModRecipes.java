package com.createtiers.registry;

import com.createtiers.CreateTiers;
import com.createtiers.recipe.CalibrationRecipe;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, CreateTiers.MOD_ID);

    public static final DeferredHolder<RecipeSerializer<?>, CalibrationRecipe.Serializer> CALIBRATION =
            SERIALIZERS.register("calibration", CalibrationRecipe.Serializer::new);

    private ModRecipes() {
    }

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}
