package com.createtiers.gametest;

import com.createtiers.CreateTiers;
import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.foundation.item.CalibratedItemData;
import com.createtiers.foundation.utility.TierCalibration;
import com.createtiers.recipe.CalibrationRecipe;
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
public final class CalibrationGameTests {
    private CalibrationGameTests() {
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 20)
    public static void calibrationApplyClearAndNbtPersistence(GameTestHelper helper) {
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
        GameTestSupport.succeed(helper, "calibration-apply-clear", "calibration-nbt-persistence");
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 20)
    public static void calibrationChangesNetworkLimitAcrossRebuild(GameTestHelper helper) {
        Tier tier = GameTestSupport.ensureAttachmentTier();
        KineticBlockEntity kinetic = GameTestSupport.placeKinetic(helper, new BlockPos(1, 1, 1));
        IAttachedTierBlockEntity attachable = GameTestSupport.requireAttachable(helper, kinetic);

        attachable.setAttachedTier(tier);
        KineticNetwork calibrated = GameTestSupport.network(10_000f, kinetic);
        GameTestSupport.assertFloat(helper, tier.getMaxSU(), calibrated.calculateCapacity(),
                "Calibrated kinetic was not included in rebuilt network tier limits");

        attachable.clearAttachedTier();
        KineticNetwork cleared = GameTestSupport.network(10_000f, kinetic);
        GameTestSupport.assertFloat(helper, 10_000f, cleared.calculateCapacity(),
                "Cleared calibration leaked into a rebuilt network");
        GameTestSupport.succeed(helper, "calibration-network-rebuild");
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 20)
    public static void calibrationRecipeItemRoundTrip(GameTestHelper helper) {
        Tier tier = GameTestSupport.ensureAttachmentTier();
        CalibrationRecipe recipe = new CalibrationRecipe(
                new net.minecraft.resources.ResourceLocation(CreateTiers.MOD_ID, "gametest_calibration_recipe"),
                GameTestSupport.ATTACHMENT_TIER_ID,
                BuiltInRegistries.ITEM.getKey(AllBlocks.SHAFT.get().asItem()),
                java.util.List.of(Ingredient.of(Items.IRON_INGOT)));

        ItemStack calibrated = recipe.getResultItem(helper.getLevel().registryAccess()).copy();
        if (!tier.equals(CalibratedItemData.getTier(calibrated))) {
            helper.fail("Calibration recipe preview did not output the base Create item carrying its tier");
        }

        BlockPos relative = new BlockPos(1, 1, 1);
        KineticBlockEntity kinetic = GameTestSupport.placeKinetic(helper, relative);
        BlockPos absolute = helper.absolutePos(relative);
        if (!BlockItem.updateCustomBlockEntityTag(helper.getLevel(), null, absolute, calibrated)) {
            helper.fail("Vanilla BlockItem placement data did not apply calibration to the placed kinetic block entity");
        }

        IAttachedTierBlockEntity attachable = GameTestSupport.requireAttachable(helper, kinetic);
        GameTestSupport.assertAttachedTier(helper, attachable, tier,
                "Recipe-produced calibrated item did not restore its tier on placement");

        ItemStack preservedDrop = Block.getDrops(kinetic.getBlockState(), helper.getLevel(), absolute, kinetic)
                .stream()
                .filter(stack -> stack.is(AllBlocks.SHAFT.get().asItem()))
                .findFirst()
                .orElse(ItemStack.EMPTY);
        if (preservedDrop.isEmpty() || !tier.equals(CalibratedItemData.getTier(preservedDrop))) {
            helper.fail("Breaking a calibrated Create kinetic block did not preserve calibration on its item drop");
        }

        GameTestSupport.succeed(helper, "calibration-recipe-item-roundtrip");
    }


    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 20)
    public static void itemBackedCalibrationCannotBypassRecipe(GameTestHelper helper) {
        Tier tier = GameTestSupport.ensureAttachmentTier();
        KineticBlockEntity kinetic = GameTestSupport.placeKinetic(helper, new BlockPos(1, 1, 1));
        IAttachedTierBlockEntity attachable = GameTestSupport.requireAttachable(helper, kinetic);

        if (TierCalibration.canMutateWithShaft(kinetic, null, tier)) {
            helper.fail("Item-backed Create kinetic accepted free tier application through the shaft fallback");
        }

        attachable.setAttachedTier(tier);
        if (!TierCalibration.canMutateWithShaft(kinetic, tier, tier)) {
            helper.fail("Matching tiered shaft could not clear an existing item-backed calibration");
        }
        if (TierCalibration.canMutateWithShaft(kinetic, tier, GameTestSupport.HIGH_TIER)) {
            helper.fail("Item-backed Create kinetic could change tiers through the shaft fallback");
        }

        attachable.clearAttachedTier();
        GameTestSupport.succeed(helper, "shaft-cannot-bypass-item-recipe");
    }

}
