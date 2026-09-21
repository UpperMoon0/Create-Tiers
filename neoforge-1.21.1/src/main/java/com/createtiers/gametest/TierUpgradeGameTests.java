package com.createtiers.gametest;

import com.createtiers.CreateTiers;
import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.IReplacementSourceBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.foundation.item.TierUpgradeItemData;
import com.createtiers.content.kinetics.TieredShaftBlock;
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
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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

        CompoundTag saved = kinetic.saveWithFullMetadata(helper.getLevel().registryAccess());
        if (!GameTestSupport.ATTACHMENT_TIER_ID.toString()
                .equals(saved.getString(GameTestSupport.ATTACHED_TIER_NBT_KEY))) {
            helper.fail("Attached tier id was not persisted in Create block-entity NBT");
        }

        attachable.clearAttachedTier();
        if (attachable.getTier() != null || attachable.getAttachedTierId() != null) {
            helper.fail("Clearing an attached tier did not restore ordinary Create tier state");
        }

        kinetic.loadWithComponents(saved, helper.getLevel().registryAccess());
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
    public static void unregisteredTierDataCannotBecomePersistentUpgrade(GameTestHelper helper) {
        Tier tier = GameTestSupport.ensureAttachmentTier();
        ItemStack forged = AllBlocks.MILLSTONE.asStack();

        try {
            TierUpgradeItemData.setTier(forged, tier);
            helper.fail("Unregistered item+tier pair accepted persistent tier item data");
        } catch (IllegalArgumentException expected) {
            // Expected: only registered item+tier variants may exist as upgraded items.
        }

        BlockPos relative = new BlockPos(1, 1, 1);
        KineticBlockEntity kinetic = GameTestSupport.placeBlockEntity(
                helper, relative, AllBlocks.MILLSTONE.getDefaultState(), KineticBlockEntity.class);
        IAttachedTierBlockEntity attachable = GameTestSupport.requireAttachable(helper, kinetic);

        CompoundTag forgedNbt = kinetic.saveWithFullMetadata(helper.getLevel().registryAccess());
        forgedNbt.putString(GameTestSupport.ATTACHED_TIER_NBT_KEY, GameTestSupport.ATTACHMENT_TIER_ID.toString());
        forgedNbt.putString(GameTestSupport.REPLACEMENT_SOURCE_NBT_KEY,
                BuiltInRegistries.BLOCK.getKey(AllBlocks.SHAFT.get()).toString());
        kinetic.loadWithComponents(forgedNbt, helper.getLevel().registryAccess());
        if (attachable.getAttachedTier() != null || attachable.getTier() != null) {
            helper.fail("Unregistered attached tier was accepted from block-entity NBT");
        }
        if (kinetic instanceof IReplacementSourceBlockEntity source
                && source.getCreateTiersReplacementSourceBlockId() != null) {
            helper.fail("Unrelated kinetic accepted forged shaft replacement-source provenance");
        }

        net.minecraft.world.level.block.Block intrinsicSource =
                BuiltInRegistries.BLOCK.get(CreateTiers.asResource("shaft_gametest_native"));
        if (!(intrinsicSource instanceof TieredShaftBlock)) {
            helper.fail("Missing intrinsic tiered-shaft GameTest fixture");
        }
        CompoundTag intrinsicOnly = kinetic.saveWithFullMetadata(helper.getLevel().registryAccess());
        intrinsicOnly.remove(GameTestSupport.ATTACHED_TIER_NBT_KEY);
        intrinsicOnly.putString(GameTestSupport.REPLACEMENT_SOURCE_NBT_KEY,
                BuiltInRegistries.BLOCK.getKey(intrinsicSource).toString());
        kinetic.loadWithComponents(intrinsicOnly, helper.getLevel().registryAccess());
        if (attachable.getTier() != null) {
            helper.fail("Unrelated kinetic derived an effective tier from forged intrinsic-shaft provenance");
        }
        if (kinetic instanceof IReplacementSourceBlockEntity source
                && source.getCreateTiersReplacementSourceBlockId() != null) {
            helper.fail("Forged intrinsic replacement-source provenance survived validation");
        }

        BlockPos absolute = helper.absolutePos(relative);
        ItemStack drop = Block.getDrops(kinetic.getBlockState(), helper.getLevel(), absolute, kinetic)
                .stream()
                .filter(stack -> stack.is(AllBlocks.MILLSTONE.get().asItem()))
                .findFirst()
                .orElse(ItemStack.EMPTY);
        if (!drop.isEmpty() && TierUpgradeItemData.getTier(drop) != null) {
            helper.fail("Unregistered attached tier persisted onto an item drop");
        }

        GameTestSupport.succeed(helper, "unregistered-tier-data-rejected", "forged-replacement-source-rejected");
    }


}
