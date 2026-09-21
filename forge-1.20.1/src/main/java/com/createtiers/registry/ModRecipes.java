package com.createtiers.registry;

import com.createtiers.CreateTiers;
import com.createtiers.recipe.TierUpgradeRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModRecipes {
    private static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, CreateTiers.MOD_ID);

    public static final RegistryObject<RecipeSerializer<TierUpgradeRecipe>> TIER_UPGRADE =
            SERIALIZERS.register("tier_upgrade", TierUpgradeRecipe.Serializer::new);

    private ModRecipes() {
    }

    public static void register(IEventBus eventBus) {
        SERIALIZERS.register(eventBus);
    }
}
