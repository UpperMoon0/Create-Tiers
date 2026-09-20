package com.createtiers.recipe;

import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import com.createtiers.foundation.item.CalibratedItemData;
import com.createtiers.registry.ModRecipes;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Shapeless calibration recipe. The output keeps the same Create block item and stores
 * the selected tier in vanilla BlockEntityTag data so normal BlockItem placement restores it.
 */
public final class CalibrationRecipe extends ShapelessRecipe {
    private final ResourceLocation tierId;
    private final ResourceLocation inputId;
    private final Item inputItem;
    private final Tier tier;
    private final List<Ingredient> extraIngredients;

    public CalibrationRecipe(ResourceLocation tierId, ResourceLocation inputId, List<Ingredient> extraIngredients) {
        this(tierId, inputId, resolveInput(inputId), resolveTier(tierId), validateIngredients(extraIngredients));
    }

    private CalibrationRecipe(ResourceLocation tierId, ResourceLocation inputId, Item inputItem, Tier tier,
            List<Ingredient> extraIngredients) {
        super("", CraftingBookCategory.MISC,
                CalibratedItemData.calibratedCopy(new ItemStack(inputItem), tier),
                allIngredients(inputItem, extraIngredients));
        this.tierId = tierId;
        this.inputId = inputId;
        this.inputItem = inputItem;
        this.tier = tier;
        this.extraIngredients = List.copyOf(extraIngredients);
    }

    @Override
    public ItemStack assemble(CraftingContainer container, RegistryAccess registries) {
        for (int slot = 0; slot < container.getContainerSize(); slot++) {
            ItemStack stack = container.getItem(slot);
            if (stack.is(inputItem)) {
                return CalibratedItemData.calibratedCopy(stack, tier);
            }
        }
        return CalibratedItemData.calibratedCopy(new ItemStack(inputItem), tier);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.CALIBRATION.get();
    }

    public ResourceLocation tierId() {
        return tierId;
    }

    public ResourceLocation inputId() {
        return inputId;
    }

    public List<Ingredient> extraIngredients() {
        return extraIngredients;
    }

    private static Item resolveInput(ResourceLocation id) {
        Item item = BuiltInRegistries.ITEM.get(id);
        if (!id.equals(BuiltInRegistries.ITEM.getKey(item))) {
            throw new IllegalArgumentException("Unknown calibration input item: " + id);
        }
        return item;
    }

    private static Tier resolveTier(ResourceLocation id) {
        Tier tier = TierRegistry.get(id);
        if (tier == null) {
            throw new IllegalArgumentException("Unknown Create Tiers calibration tier: " + id);
        }
        return tier;
    }

    private static List<Ingredient> validateIngredients(List<Ingredient> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            throw new IllegalArgumentException("Calibration recipe requires at least one upgrade ingredient");
        }
        if (ingredients.size() > 8) {
            throw new IllegalArgumentException("Calibration recipe supports at most eight upgrade ingredients");
        }
        return List.copyOf(ingredients);
    }

    private static NonNullList<Ingredient> allIngredients(Item inputItem, List<Ingredient> extras) {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(inputItem));
        ingredients.addAll(extras);
        return ingredients;
    }

    public static final class Serializer implements RecipeSerializer<CalibrationRecipe> {
        @Override
        public CalibrationRecipe fromJson(ResourceLocation recipeId, JsonObject json) {
            ResourceLocation tierId = new ResourceLocation(GsonHelper.getAsString(json, "tier"));
            ResourceLocation inputId = new ResourceLocation(GsonHelper.getAsString(json, "input"));
            JsonArray array = GsonHelper.getAsJsonArray(json, "ingredients");
            if (array.size() < 1 || array.size() > 8) {
                throw new JsonParseException("Create Tiers calibration recipes require 1-8 upgrade ingredients");
            }

            List<Ingredient> ingredients = new ArrayList<>(array.size());
            array.forEach(element -> ingredients.add(Ingredient.fromJson(element, false)));
            try {
                return new CalibrationRecipe(tierId, inputId, ingredients);
            } catch (IllegalArgumentException ex) {
                throw new JsonParseException("Invalid calibration recipe " + recipeId + ": " + ex.getMessage(), ex);
            }
        }

        @Override
        public CalibrationRecipe fromNetwork(ResourceLocation recipeId, FriendlyByteBuf buffer) {
            ResourceLocation tierId = buffer.readResourceLocation();
            ResourceLocation inputId = buffer.readResourceLocation();
            int count = buffer.readVarInt();
            List<Ingredient> ingredients = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                ingredients.add(Ingredient.fromNetwork(buffer));
            }
            return new CalibrationRecipe(tierId, inputId, ingredients);
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, CalibrationRecipe recipe) {
            buffer.writeResourceLocation(recipe.tierId);
            buffer.writeResourceLocation(recipe.inputId);
            buffer.writeVarInt(recipe.extraIngredients.size());
            recipe.extraIngredients.forEach(ingredient -> ingredient.toNetwork(buffer));
        }
    }
}
