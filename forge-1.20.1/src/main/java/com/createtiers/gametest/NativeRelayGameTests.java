package com.createtiers.gametest;

import com.createtiers.CreateTiers;
import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import com.createtiers.api.TieredNativeKineticBlock;
import com.createtiers.api.TierUpgradeRegistry;
import com.createtiers.foundation.item.TierUpgradeItemData;
import com.createtiers.registry.CommonCreativeTab;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlock;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateTiers.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NativeRelayGameTests {
    private NativeRelayGameTests() {
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 20)
    public static void defaultTierRegistersNativeRelayFamily(GameTestHelper helper) {
        Tier tier = TierRegistry.get(new net.minecraft.resources.ResourceLocation(CreateTiers.MOD_ID, "gametest_native"));
        if (tier == null) {
            helper.fail("GameTest startup tier was not registered");
        }

        assertNative(helper, tier, "clutch_gametest_native", new BlockPos(1, 1, 1));
        assertNative(helper, tier, "gearshift_gametest_native", new BlockPos(2, 1, 1));
        assertNative(helper, tier, "encased_chain_drive_gametest_native", new BlockPos(3, 1, 1));
        assertNative(helper, tier, "adjustable_chain_gearshift_gametest_native", new BlockPos(4, 1, 1));
        assertNative(helper, tier, "metal_girder_encased_shaft_gametest_native", new BlockPos(5, 1, 1));

        assertAxeOrPickaxe(helper, "clutch_gametest_native");
        assertAxeOrPickaxe(helper, "gearshift_gametest_native");
        assertAxeOrPickaxe(helper, "encased_chain_drive_gametest_native");
        assertAxeOrPickaxe(helper, "adjustable_chain_gearshift_gametest_native");

        Block controllerBlock = BuiltInRegistries.BLOCK.get(
                new net.minecraft.resources.ResourceLocation(CreateTiers.MOD_ID,
                        "rotation_speed_controller_gametest_native"));
        if (!controllerBlock.defaultBlockState().is(BlockTags.MINEABLE_WITH_AXE)
                || !controllerBlock.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            helper.fail("Native Rotation Speed Controller does not mirror Create's axeOrPickaxe mining tags");
        }

        SpeedControllerBlockEntity controller = GameTestSupport.placeBlockEntity(
                helper, new BlockPos(6, 1, 1), controllerBlock.defaultBlockState(), SpeedControllerBlockEntity.class);
        IAttachedTierBlockEntity tieredController = GameTestSupport.requireAttachable(helper, controller);
        if (!tier.equals(tieredController.getTier()) || tieredController.getAttachedTier() != null) {
            helper.fail("Native Rotation Speed Controller did not resolve its intrinsic tier");
        }
        controller.targetSpeed.setValue(tier.getMaxRPM());
        if (controller.targetSpeed.getValue() != tier.getMaxRPM()) {
            helper.fail("Native Rotation Speed Controller did not initialize its tier RPM range");
        }

        assertUpgradeTargetValidation(helper);
        assertTierUpgradeCreativeEntry(helper, tier);

        GameTestSupport.succeed(helper,
                "native-relay-default-family",
                "native-axe-or-pickaxe-parity",
                "tier-upgrade-creative-tab-entry");
    }


    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 40)
    public static void nativeSpeedControllerCouplesTieredLargeCog(GameTestHelper helper) {
        Block shaftBlock = BuiltInRegistries.BLOCK.get(
                new net.minecraft.resources.ResourceLocation(CreateTiers.MOD_ID, "shaft_gametest_native"));
        Block controllerBlock = BuiltInRegistries.BLOCK.get(
                new net.minecraft.resources.ResourceLocation(CreateTiers.MOD_ID, "rotation_speed_controller_gametest_native"));
        Block largeCogBlock = BuiltInRegistries.BLOCK.get(
                new net.minecraft.resources.ResourceLocation(CreateTiers.MOD_ID, "large_cogwheel_gametest_native"));

        KineticBlockEntity source = GameTestSupport.placeBlockEntity(
                helper,
                new BlockPos(2, 1, 2),
                shaftBlock.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.X),
                KineticBlockEntity.class);
        SpeedControllerBlockEntity controller = GameTestSupport.placeBlockEntity(
                helper,
                new BlockPos(3, 1, 2),
                controllerBlock.defaultBlockState().setValue(SpeedControllerBlock.HORIZONTAL_AXIS, Direction.Axis.X),
                SpeedControllerBlockEntity.class);
        KineticBlockEntity largeCog = GameTestSupport.placeBlockEntity(
                helper,
                new BlockPos(3, 2, 2),
                largeCogBlock.defaultBlockState().setValue(BlockStateProperties.AXIS, Direction.Axis.Z),
                KineticBlockEntity.class);

        controller.targetSpeed.setValue(160);
        source.setSpeed(64);
        source.setNetwork(source.getBlockPos().asLong());
        RotationPropagator.handleAdded(helper.getLevel(), source.getBlockPos(), source);

        GameTestSupport.assertFloat(
                helper, 64, controller.getTheoreticalSpeed(),
                "Tiered Rotation Speed Controller did not accept shaft input");
        GameTestSupport.assertFloat(
                helper, 160, largeCog.getTheoreticalSpeed(),
                "Tiered Rotation Speed Controller did not drive the tiered large cog above it");

        GameTestSupport.succeed(helper, "native-speed-controller-large-cog");
    }

    private static void assertAxeOrPickaxe(GameTestHelper helper, String path) {
        Block block = BuiltInRegistries.BLOCK.get(
                new net.minecraft.resources.ResourceLocation(CreateTiers.MOD_ID, path));
        if (!block.defaultBlockState().is(BlockTags.MINEABLE_WITH_AXE)
                || !block.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            helper.fail("Native tier block does not mirror Create's axeOrPickaxe mining tags: " + path);
        }
    }

    private static void assertTierUpgradeCreativeEntry(GameTestHelper helper, Tier tier) {
        boolean found = CommonCreativeTab.tierUpgradeEntries().stream()
                .anyMatch(stack -> stack.is(AllBlocks.SHAFT.get().asItem())
                        && tier.equals(TierUpgradeItemData.getTier(stack)));
        if (!found) {
            helper.fail("Registered tier-upgrade output is missing from the Create Tiers creative tab");
        }
    }

    private static void assertUpgradeTargetValidation(GameTestHelper helper) {
        assertValidUpgradeTarget(helper, new net.minecraft.resources.ResourceLocation("create", "large_water_wheel"));

        assertInvalidUpgradeTarget(helper, new net.minecraft.resources.ResourceLocation("minecraft", "does_not_exist"));
        assertInvalidUpgradeTarget(helper, new net.minecraft.resources.ResourceLocation("minecraft", "stick"));
        assertInvalidUpgradeTarget(helper, new net.minecraft.resources.ResourceLocation("minecraft", "stone"));
        assertInvalidUpgradeTarget(helper, new net.minecraft.resources.ResourceLocation("create", "speedometer"));
        assertInvalidUpgradeTarget(helper,
                new net.minecraft.resources.ResourceLocation(CreateTiers.MOD_ID, "clutch_gametest_native"));
    }

    private static void assertValidUpgradeTarget(GameTestHelper helper, net.minecraft.resources.ResourceLocation id) {
        try {
            TierUpgradeRegistry.validateTarget(id);
        } catch (IllegalArgumentException ex) {
            helper.fail("Expected valid tier upgrade target " + id + ": " + ex.getMessage());
        }
    }

    private static void assertInvalidUpgradeTarget(GameTestHelper helper, net.minecraft.resources.ResourceLocation id) {
        try {
            TierUpgradeRegistry.validateTarget(id);
            helper.fail("Expected invalid tier upgrade target to be rejected: " + id);
        } catch (IllegalArgumentException expected) {
            // Expected validation failure.
        }
    }

    private static void assertNative(GameTestHelper helper, Tier tier, String path, BlockPos pos) {
        Block block = BuiltInRegistries.BLOCK.get(new net.minecraft.resources.ResourceLocation(CreateTiers.MOD_ID, path));
        if (!(block instanceof TieredNativeKineticBlock)) {
            helper.fail("Missing native default tier block: " + path);
            return;
        }
        TieredNativeKineticBlock nativeBlock = (TieredNativeKineticBlock) block;
        if (!nativeBlock.getExpectedBlockEntityType().isValid(block.defaultBlockState())) {
            helper.fail("Declared Create block-entity type rejects native tier block state: " + path);
        }

        KineticBlockEntity blockEntity = GameTestSupport.placeBlockEntity(
                helper, pos, block.defaultBlockState(), KineticBlockEntity.class);
        if (blockEntity.getType() != nativeBlock.getExpectedBlockEntityType()) {
            helper.fail("Native tier block did not reuse its declared Create block-entity type: " + path);
        }
        IAttachedTierBlockEntity attached = GameTestSupport.requireAttachable(helper, blockEntity);
        if (!tier.equals(attached.getTier()) || attached.getAttachedTier() != null) {
            helper.fail("Native tier block did not expose only its intrinsic tier: " + path);
        }
        try {
            attached.setAttachedTier(tier);
            helper.fail("Native tier block accepted an attached tier: " + path);
        } catch (IllegalStateException expected) {
            // Native blocks are already intrinsically tiered.
        }
    }
}
