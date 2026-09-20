package com.createtiers.gametest;

import com.createtiers.CreateTiers;
import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.Tier;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

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
        helper.succeed();
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
        helper.succeed();
    }
}
