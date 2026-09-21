package com.createtiers.recipe;

import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import com.createtiers.api.TierUpgradeRegistry;
import com.createtiers.foundation.item.TierUpgradeItemData;
import com.createtiers.registry.ModRecipes;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;

import java.util.ArrayList;
import java.util.List;

/**
 * Shapeless built-in tier-upgrade recipe. The output keeps the same Create block item and stores
 * the selected tier in vanilla block-entity item data so normal BlockItem placement restores it.
 */
public final class TierUpgradeRecipe extends ShapelessRecipe {
    private final ResourceLocation tierId;
    private final ResourceLocation inputId;
    private final Item inputItem;
    private final Tier tier;
    private final List<Ingredient> extraIngredients;

    public TierUpgradeRecipe(ResourceLocation tierId, ResourceLocation inputId, List<Ingredient> extraIngredients) {
        this(tierId, inputId, resolveInput(inputId), resolveTier(inputId, tierId), requireIngredients(extraIngredients));
    }

    private TierUpgradeRecipe(ResourceLocation tierId, ResourceLocation inputId, Item inputItem, Tier tier,
            List<Ingredient> extraIngredients) {
        super("", CraftingBookCategory.MISC,
                TierUpgradeItemData.upgradedCopy(new ItemStack(inputItem), tier),
                allIngredients(inputItem, extraIngredients));
        this.tierId = tierId;
        this.inputId = inputId;
        this.inputItem = inputItem;
        this.tier = tier;
        this.extraIngredients = List.copyOf(extraIngredients);
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        for (ItemStack stack : input.items()) {
            if (stack.is(inputItem)) {
                return TierUpgradeItemData.upgradedCopy(stack, tier);
            }
        }
        return TierUpgradeItemData.upgradedCopy(new ItemStack(inputItem), tier);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.TIER_UPGRADE.get();
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
            throw new IllegalArgumentException("Unknown tier upgrade input item: " + id);
        }
        return item;
    }

    private static Tier resolveTier(ResourceLocation inputId, ResourceLocation tierId) {
        Tier tier = TierRegistry.get(tierId);
        if (tier == null) {
            throw new IllegalArgumentException("Unknown Create Tiers tier: " + tierId);
        }
        if (!TierUpgradeRegistry.isRegistered(inputId, tierId)) {
            throw new IllegalArgumentException(
                    "Unregistered tier upgrade pair: item '" + inputId + "', tier '" + tierId + "'");
        }
        return tier;
    }

    private static List<Ingredient> requireIngredients(List<Ingredient> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            throw new IllegalArgumentException("Tier upgrade recipe requires at least one upgrade ingredient");
        }
        if (ingredients.size() > 8) {
            throw new IllegalArgumentException("Tier upgrade recipe supports at most eight upgrade ingredients");
        }
        return List.copyOf(ingredients);
    }

    private static DataResult<List<Ingredient>> validateIngredients(List<Ingredient> ingredients) {
        if (ingredients.isEmpty()) {
            return DataResult.error(() -> "Tier upgrade recipe requires at least one upgrade ingredient");
        }
        if (ingredients.size() > 8) {
            return DataResult.error(() -> "Tier upgrade recipe supports at most eight upgrade ingredients");
        }
        return DataResult.success(List.copyOf(ingredients));
    }

    private static NonNullList<Ingredient> allIngredients(Item inputItem, List<Ingredient> extras) {
        NonNullList<Ingredient> ingredients = NonNullList.create();
        ingredients.add(Ingredient.of(inputItem));
        ingredients.addAll(extras);
        return ingredients;
    }

    public static final class Serializer implements RecipeSerializer<TierUpgradeRecipe> {
        private static final MapCodec<TierUpgradeRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("tier").forGetter(recipe -> recipe.tierId),
                ResourceLocation.CODEC.fieldOf("input").forGetter(recipe -> recipe.inputId),
                Ingredient.CODEC_NONEMPTY.listOf().fieldOf("ingredients")
                        .flatXmap(TierUpgradeRecipe::validateIngredients, DataResult::success)
                        .forGetter(recipe -> recipe.extraIngredients)
        ).apply(instance, TierUpgradeRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, TierUpgradeRecipe> STREAM_CODEC = StreamCodec.of(
                Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public MapCodec<TierUpgradeRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, TierUpgradeRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static TierUpgradeRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            ResourceLocation tierId = buffer.readResourceLocation();
            ResourceLocation inputId = buffer.readResourceLocation();
            int count = buffer.readVarInt();
            List<Ingredient> ingredients = new ArrayList<>(count);
            for (int i = 0; i < count; i++) {
                ingredients.add(Ingredient.CONTENTS_STREAM_CODEC.decode(buffer));
            }
            return new TierUpgradeRecipe(tierId, inputId, ingredients);
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, TierUpgradeRecipe recipe) {
            buffer.writeResourceLocation(recipe.tierId);
            buffer.writeResourceLocation(recipe.inputId);
            buffer.writeVarInt(recipe.extraIngredients.size());
            recipe.extraIngredients.forEach(ingredient -> Ingredient.CONTENTS_STREAM_CODEC.encode(buffer, ingredient));
        }
    }
}
