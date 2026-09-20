package com.createtiers.gametest;

import com.createtiers.CreateTiers;
import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.content.kinetics.TieredPoweredShaftBlock;
import com.createtiers.content.kinetics.TieredPoweredShaftBlockEntity;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.createtiers.registry.ModBlocks;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
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
