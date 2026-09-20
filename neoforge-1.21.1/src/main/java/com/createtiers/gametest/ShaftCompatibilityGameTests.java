package com.createtiers.gametest;

import com.createtiers.CreateTiers;
import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.content.kinetics.TieredPoweredShaftBlock;
import com.createtiers.content.kinetics.TieredPoweredShaftBlockEntity;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.createtiers.registry.ModBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.encasing.EncasedBlock;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

@GameTestHolder(CreateTiers.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShaftCompatibilityGameTests {
    private ShaftCompatibilityGameTests() {
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 40)
    public static void tieredShaftsWorkAsBeltsAndPreserveTier(GameTestHelper helper) {
        TieredShaftBlock shaft = requireTieredShaft(helper);
        Tier tier = shaft.getTier();
        BlockState shaftState = shaft.defaultBlockState().setValue(ShaftBlock.AXIS, Direction.Axis.X);

        if (!ShaftBlock.isShaft(shaftState)) {
            helper.fail("Create ShaftBlock.isShaft still rejects a native tiered shaft");
        }

        BlockPos start = new BlockPos(1, 1, 1);
        BlockPos middle = new BlockPos(1, 1, 3);
        BlockPos end = new BlockPos(1, 1, 5);
        for (BlockPos pos : new BlockPos[]{start, middle, end}) {
            helper.getLevel().setBlock(helper.absolutePos(pos), shaftState, 3);
        }

        if (!BeltConnectorItem.validateAxis(helper.getLevel(), helper.absolutePos(start))) {
            helper.fail("Create belt connector still rejects a tiered shaft endpoint");
        }

        BeltConnectorItem.createBelts(helper.getLevel(), helper.absolutePos(start), helper.absolutePos(end));

        for (BlockPos pos : new BlockPos[]{start, middle, end}) {
            BlockPos absolute = helper.absolutePos(pos);
            if (!AllBlocks.BELT.has(helper.getLevel().getBlockState(absolute))) {
                helper.fail("Tiered shaft at " + pos + " was not replaced by a Create belt pulley");
            }
            BlockEntity blockEntity = helper.getLevel().getBlockEntity(absolute);
            if (!(blockEntity instanceof IAttachedTierBlockEntity)) {
                helper.fail("Belt pulley at " + pos + " cannot retain its source shaft tier");
            }
            IAttachedTierBlockEntity attached = (IAttachedTierBlockEntity) blockEntity;
            if (!tier.equals(attached.getAttachedTier())) {
                helper.fail("Belt pulley at " + pos + " lost its source shaft tier");
            }
        }

        helper.getLevel().destroyBlock(helper.absolutePos(start), false);
        BlockState restored = helper.getLevel().getBlockState(helper.absolutePos(end));
        if (!(restored.getBlock() instanceof TieredShaftBlock restoredShaft)
                || !tier.equals(restoredShaft.getTier())) {
            helper.fail("Breaking the belt restored an untiered pulley instead of the original tiered shaft");
        }

        helper.succeed();
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 40)
    public static void tieredShaftsWorkWithSteamEnginesAndRecoverTier(GameTestHelper helper) {
        TieredShaftBlock shaft = requireTieredShaft(helper);
        Tier tier = shaft.getTier();
        BlockState engineState = AllBlocks.STEAM_ENGINE.get().defaultBlockState();
        Direction.Axis engineAxis = SteamEngineBlock.getFacing(engineState).getAxis();
        Direction.Axis shaftAxis = engineAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        BlockState shaftState = shaft.defaultBlockState().setValue(ShaftBlock.AXIS, shaftAxis);

        if (!SteamEngineBlock.isShaftValid(engineState, shaftState)) {
            helper.fail("Create steam engine still rejects a tiered shaft");
        }

        BlockState poweredState = PoweredShaftBlock.getEquivalent(shaftState);
        if (!(poweredState.getBlock() instanceof TieredPoweredShaftBlock)) {
            helper.fail("Steam engine converted a tiered shaft into an untiered powered shaft");
        }
        TieredPoweredShaftBlock powered = (TieredPoweredShaftBlock) poweredState.getBlock();
        if (!tier.equals(powered.getTier())) {
            helper.fail("Steam engine powered-shaft conversion changed the tier");
        }
        if (poweredState.getValue(PoweredShaftBlock.AXIS) != shaftAxis) {
            helper.fail("Steam engine powered-shaft conversion changed the shaft axis");
        }

        BlockPos pos = helper.absolutePos(new BlockPos(1, 1, 1));
        helper.getLevel().setBlock(pos, poweredState, 3);
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(pos);
        if (!(blockEntity instanceof TieredPoweredShaftBlockEntity tieredPowered)
                || !tier.equals(tieredPowered.getTier())) {
            helper.fail("Tiered powered shaft block entity did not retain its intrinsic tier");
        }

        powered.tick(poweredState, helper.getLevel(), pos, helper.getLevel().random);
        BlockState recovered = helper.getLevel().getBlockState(pos);
        if (!(recovered.getBlock() instanceof TieredShaftBlock recoveredShaft)
                || !tier.equals(recoveredShaft.getTier())) {
            helper.fail("Orphaned tiered powered shaft reverted to the wrong shaft");
        }

        helper.succeed();
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 40)
    public static void registeredShaftTierSurvivesBeltReplacement(GameTestHelper helper) {
        Tier tier = GameTestSupport.ensureAttachmentTier();
        BlockState shaftState = AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Direction.Axis.X);

        BlockPos start = new BlockPos(1, 1, 1);
        BlockPos middle = new BlockPos(1, 1, 3);
        BlockPos end = new BlockPos(1, 1, 5);
        for (BlockPos relative : new BlockPos[]{start, middle, end}) {
            BlockPos absolute = helper.absolutePos(relative);
            helper.getLevel().setBlock(absolute, shaftState, 3);
            attachTier(helper, absolute, tier, "Ordinary shaft could not receive the registered runtime tier");
        }

        BeltConnectorItem.createBelts(helper.getLevel(), helper.absolutePos(start), helper.absolutePos(end));

        for (BlockPos relative : new BlockPos[]{start, middle, end}) {
            BlockPos absolute = helper.absolutePos(relative);
            if (!AllBlocks.BELT.has(helper.getLevel().getBlockState(absolute))) {
                helper.fail("Registered-tier shaft was not converted into a Create belt pulley at " + relative);
            }
            assertAttachedTierAt(helper, absolute, tier,
                    "Create belt replacement silently lost the shaft's attached tier at " + relative);
        }

        helper.succeed();
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 40)
    public static void registeredShaftTierSurvivesSteamPoweredShaftRoundTrip(GameTestHelper helper) {
        Tier tier = GameTestSupport.ensureAttachmentTier();
        BlockState engineState = AllBlocks.STEAM_ENGINE.getDefaultState();
        Direction.Axis engineAxis = SteamEngineBlock.getFacing(engineState).getAxis();
        Direction.Axis shaftAxis = engineAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;

        BlockPos enginePos = helper.absolutePos(new BlockPos(4, 4, 4));
        BlockPos shaftPos = SteamEngineBlock.getShaftPos(engineState, enginePos);
        BlockState shaftState = AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, shaftAxis);
        helper.getLevel().setBlock(shaftPos, shaftState, 3);
        attachTier(helper, shaftPos, tier, "Ordinary steam input shaft could not receive the registered runtime tier");

        // Exercise Create's real onPlace conversion, which bypasses switchToBlockState.
        AllBlocks.STEAM_ENGINE.get().onPlace(
                engineState, helper.getLevel(), enginePos, Blocks.AIR.defaultBlockState(), false);

        BlockState poweredState = helper.getLevel().getBlockState(shaftPos);
        if (!AllBlocks.POWERED_SHAFT.has(poweredState)) {
            helper.fail("Steam engine did not convert the ordinary shaft into Create's powered shaft");
        }
        assertAttachedTierAt(helper, shaftPos, tier,
                "Steam shaft -> powered shaft conversion silently lost the attached tier");

        ((PoweredShaftBlock) AllBlocks.POWERED_SHAFT.get())
                .tick(poweredState, helper.getLevel(), shaftPos, helper.getLevel().random);

        if (!AllBlocks.SHAFT.has(helper.getLevel().getBlockState(shaftPos))) {
            helper.fail("Orphaned ordinary powered shaft did not revert to Create's shaft");
        }
        assertAttachedTierAt(helper, shaftPos, tier,
                "Powered shaft -> shaft recovery silently lost the attached tier");

        helper.succeed();
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 40)
    public static void registeredKineticTierSurvivesCreateEncasing(GameTestHelper helper) {
        Tier tier = GameTestSupport.ensureAttachmentTier();

        BlockPos shaftPos = helper.absolutePos(new BlockPos(1, 1, 7));
        BlockState shaftState = AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Direction.Axis.X);
        helper.getLevel().setBlock(shaftPos, shaftState, 3);
        attachTier(helper, shaftPos, tier, "Ordinary shaft could not receive the registered runtime tier");
        ((EncasedBlock) AllBlocks.ANDESITE_ENCASED_SHAFT.get()).handleEncasing(
                shaftState, helper.getLevel(), shaftPos, AllBlocks.ANDESITE_CASING.asStack(),
                null, InteractionHand.MAIN_HAND, null);

        if (!AllBlocks.ANDESITE_ENCASED_SHAFT.has(helper.getLevel().getBlockState(shaftPos))) {
            helper.fail("Create shaft encasing did not produce the andesite encased shaft");
        }
        assertAttachedTierAt(helper, shaftPos, tier,
                "Shaft -> encased shaft conversion silently lost the attached tier");

        BlockPos cogPos = helper.absolutePos(new BlockPos(3, 1, 7));
        BlockState cogState = AllBlocks.COGWHEEL.getDefaultState().setValue(ShaftBlock.AXIS, Direction.Axis.X);
        helper.getLevel().setBlock(cogPos, cogState, 3);
        attachTier(helper, cogPos, tier, "Ordinary cogwheel could not receive the registered runtime tier");
        ((EncasedBlock) AllBlocks.ANDESITE_ENCASED_COGWHEEL.get()).handleEncasing(
                cogState, helper.getLevel(), cogPos, AllBlocks.ANDESITE_CASING.asStack(),
                null, InteractionHand.MAIN_HAND, null);

        if (!AllBlocks.ANDESITE_ENCASED_COGWHEEL.has(helper.getLevel().getBlockState(cogPos))) {
            helper.fail("Create cogwheel encasing did not produce the andesite encased cogwheel");
        }
        assertAttachedTierAt(helper, cogPos, tier,
                "Cogwheel -> encased cogwheel conversion silently lost the attached tier");

        helper.succeed();
    }

    private static void attachTier(GameTestHelper helper, BlockPos absolute, Tier tier, String message) {
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(absolute);
        if (!(blockEntity instanceof IAttachedTierBlockEntity attachable)) {
            helper.fail(message + " (missing attached-tier interface)");
            return;
        }
        attachable.setAttachedTier(tier);
        GameTestSupport.assertAttachedTier(helper, attachable, tier, message);
    }

    private static void assertAttachedTierAt(GameTestHelper helper, BlockPos absolute, Tier tier, String message) {
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(absolute);
        if (!(blockEntity instanceof IAttachedTierBlockEntity attachable)) {
            helper.fail(message + " (replacement block entity has no attached-tier interface)");
            return;
        }
        GameTestSupport.assertAttachedTier(helper, attachable, tier, message);
    }

    private static TieredShaftBlock requireTieredShaft(GameTestHelper helper) {
        for (net.minecraft.world.level.block.Block block : ModBlocks.SHAFTS) {
            if (block instanceof TieredShaftBlock shaft) {
                return shaft;
            }
        }
        helper.fail("GameTest runtime has no registered Create Tiers shaft fixture");
        throw new IllegalStateException("GameTest failure did not abort");
    }
}
