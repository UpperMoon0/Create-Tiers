package com.createtiers.gametest;

import com.createtiers.CreateTiers;
import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.foundation.item.TierUpgradeItemData;
import com.createtiers.foundation.utility.InWorldTierUpgrade;
import com.createtiers.recipe.TierUpgradeRecipe;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateTiers.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TierUpgradeGameTests {
    private TierUpgradeGameTests() {
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 20)
    public static void attachedTierPersistsAndCanClear(GameTestHelper helper) {
        Tier tier = GameTestSupport.ensureAttachmentTier();
        KineticBlockEntity kinetic = GameTestSupport.placeKinetic(helper, new BlockPos(1, 1, 1));
        IAttachedTierBlockEntity attachable = GameTestSupport.requireAttachable(helper, kinetic);

        attachable.setAttachedTier(tier);
        GameTestSupport.assertAttachedTier(helper, attachable, tier,
                "Ordinary Create kinetic component did not expose its attached tier");

        CompoundTag saved = kinetic.saveWithFullMetadata();
        if (!GameTestSupport.ATTACHMENT_TIER_ID.toString()
                .equals(saved.getString(GameTestSupport.ATTACHED_TIER_NBT_KEY))) {
            helper.fail("Attached tier id was not persisted in Create block-entity NBT");
        }

        attachable.clearAttachedTier();
        if (attachable.getTier() != null || attachable.getAttachedTierId() != null) {
            helper.fail("Clearing an attached tier did not restore ordinary Create tier state");
        }

        kinetic.load(saved);
        GameTestSupport.assertAttachedTier(helper, attachable, tier,
                "Attached tier was not restored from Create block-entity NBT");
        attachable.clearAttachedTier();
        GameTestSupport.succeed(helper, "attached-tier-apply-clear", "attached-tier-nbt-persistence");
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 20)
    public static void attachedTierChangesNetworkLimitAcrossRebuild(GameTestHelper helper) {
        Tier tier = GameTestSupport.ensureAttachmentTier();
        KineticBlockEntity kinetic = GameTestSupport.placeKinetic(helper, new BlockPos(1, 1, 1));
        IAttachedTierBlockEntity attachable = GameTestSupport.requireAttachable(helper, kinetic);

        attachable.setAttachedTier(tier);
        KineticNetwork tieredNetwork = GameTestSupport.network(10_000f, kinetic);
        GameTestSupport.assertFloat(helper, tier.getMaxSU(), tieredNetwork.calculateCapacity(),
                "Attached tier was not included in rebuilt network limits");

        attachable.clearAttachedTier();
        KineticNetwork cleared = GameTestSupport.network(10_000f, kinetic);
        GameTestSupport.assertFloat(helper, 10_000f, cleared.calculateCapacity(),
                "Cleared attached tier leaked into a rebuilt network");
        GameTestSupport.succeed(helper, "attached-tier-network-rebuild");
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 20)
    public static void tierUpgradeRecipeItemRoundTrip(GameTestHelper helper) {
        Tier tier = GameTestSupport.ensureAttachmentTier();
        TierUpgradeRecipe recipe = new TierUpgradeRecipe(
                new net.minecraft.resources.ResourceLocation(CreateTiers.MOD_ID, "gametest_tier_upgrade_recipe"),
                GameTestSupport.ATTACHMENT_TIER_ID,
                BuiltInRegistries.ITEM.getKey(AllBlocks.SHAFT.get().asItem()),
                java.util.List.of(Ingredient.of(Items.IRON_INGOT)));

        ItemStack upgraded = recipe.getResultItem(helper.getLevel().registryAccess()).copy();
        if (!tier.equals(TierUpgradeItemData.getTier(upgraded))) {
            helper.fail("Tier-upgrade recipe preview did not output the base Create item carrying its tier");
        }

        BlockPos relative = new BlockPos(1, 1, 1);
        KineticBlockEntity kinetic = GameTestSupport.placeKinetic(helper, relative);
        BlockPos absolute = helper.absolutePos(relative);
        if (!BlockItem.updateCustomBlockEntityTag(helper.getLevel(), null, absolute, upgraded)) {
            helper.fail("Vanilla BlockItem placement data did not apply tier data to the placed kinetic block entity");
        }

        IAttachedTierBlockEntity attachable = GameTestSupport.requireAttachable(helper, kinetic);
        GameTestSupport.assertAttachedTier(helper, attachable, tier,
                "Recipe-produced tier-upgraded item did not restore its tier on placement");

        ItemStack preservedDrop = Block.getDrops(kinetic.getBlockState(), helper.getLevel(), absolute, kinetic)
                .stream()
                .filter(stack -> stack.is(AllBlocks.SHAFT.get().asItem()))
                .findFirst()
                .orElse(ItemStack.EMPTY);
        if (preservedDrop.isEmpty() || !tier.equals(TierUpgradeItemData.getTier(preservedDrop))) {
            helper.fail("Breaking a tier-upgraded Create kinetic block did not preserve tier data on its item drop");
        }

        GameTestSupport.succeed(helper, "tier-upgrade-item-roundtrip");
    }


    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 20)
    public static void itemBackedTierCannotBypassRecipe(GameTestHelper helper) {
        Tier tier = GameTestSupport.ensureAttachmentTier();
        KineticBlockEntity kinetic = GameTestSupport.placeKinetic(helper, new BlockPos(1, 1, 1));
        IAttachedTierBlockEntity attachable = GameTestSupport.requireAttachable(helper, kinetic);

        if (InWorldTierUpgrade.canApplyWithShaft(kinetic, null, tier)) {
            helper.fail("Item-backed Create kinetic accepted free tier application through the shaft fallback");
        }

        attachable.setAttachedTier(tier);
        if (InWorldTierUpgrade.canApplyWithShaft(kinetic, tier, tier)) {
            helper.fail("Item-backed Create kinetic allowed a tiered shaft to clear its recipe-produced tier");
        }
        if (InWorldTierUpgrade.canApplyWithShaft(kinetic, tier, GameTestSupport.HIGH_TIER)) {
            helper.fail("Item-backed Create kinetic could change tiers through the shaft fallback");
        }

        attachable.clearAttachedTier();
        GameTestSupport.succeed(helper, "shaft-cannot-bypass-item-recipe");
    }

}
