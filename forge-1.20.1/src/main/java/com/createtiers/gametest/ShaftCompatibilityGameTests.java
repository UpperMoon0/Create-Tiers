package com.createtiers.gametest;

import com.createtiers.CreateTiers;
import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.IReplacementSourceBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import com.createtiers.content.kinetics.TieredPoweredShaftBlock;
import com.createtiers.content.kinetics.TieredPoweredShaftBlockEntity;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.createtiers.registry.ModBlocks;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.encasing.EncasedBlock;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@GameTestHolder(CreateTiers.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ShaftCompatibilityGameTests {
    private ShaftCompatibilityGameTests() {
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 40)
    public static void tieredShaftsWorkAsBeltsAndPreserveTier(GameTestHelper helper) {
        TieredShaftBlock shaft = requireTieredShaft(helper);
        Tier tier = shaft.getTier();
        var tierId = TierRegistry.getId(tier);
        var shaftId = BuiltInRegistries.BLOCK.getKey(shaft);
        BlockState shaftState = shaft.defaultBlockState().setValue(ShaftBlock.AXIS, Direction.Axis.X);

        if (!ShaftBlock.isShaft(shaftState)) {
            helper.fail("Create ShaftBlock.isShaft still rejects a native tiered shaft");
        }
        if (tierId == null) {
            helper.fail("Native GameTest tier has no registered tier id");
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
            if (!(blockEntity instanceof IAttachedTierBlockEntity attached)) {
                helper.fail("Belt pulley at " + pos + " cannot expose its source shaft tier");
                continue;
            }
            if (!tier.equals(attached.getTier())) {
                helper.fail("Belt pulley at " + pos + " lost its effective intrinsic source tier");
            }
            if (attached.getAttachedTier() != null) {
                helper.fail("Intrinsic shaft belt pulley incorrectly stored its tier as mutable attached tier data");
            }
            if (!(blockEntity instanceof IReplacementSourceBlockEntity source)
                    || !shaftId.equals(source.getCreateTiersReplacementSourceBlockId())) {
                helper.fail("Belt pulley at " + pos + " did not persist its exact intrinsic source shaft id");
            }
            if (!(blockEntity instanceof KineticBlockEntity)) {
                helper.fail("Intrinsic shaft belt pulley lost its kinetic block entity");
            }
        }

        // Serialize and recreate one pulley BE before teardown. Add the old attached-tier
        // field to emulate belts saved by an earlier PR head where intrinsic provenance
        // was also duplicated as mutable attached tier data.
        BlockPos reloadedPos = helper.absolutePos(end);
        BlockEntity original = helper.getLevel().getBlockEntity(reloadedPos);
        if (original == null) {
            helper.fail("Missing belt block entity before reload");
        }
        CompoundTag saved = original.saveWithFullMetadata();
        saved.putString(GameTestSupport.ATTACHED_TIER_NBT_KEY, tierId.toString());

        BlockState beltState = helper.getLevel().getBlockState(reloadedPos);
        helper.getLevel().removeBlockEntity(reloadedPos);
        BlockEntity reloaded = AllBlockEntityTypes.BELT.get().create(reloadedPos, beltState);
        if (reloaded == null) {
            helper.fail("Could not recreate belt block entity from saved state");
        }
        reloaded.load(saved);
        helper.getLevel().setBlockEntity(reloaded);

        if (!(reloaded instanceof IAttachedTierBlockEntity attachedReloaded)
                || !(reloaded instanceof IReplacementSourceBlockEntity sourceReloaded)) {
            helper.fail("Reloaded belt block entity lost Create Tiers runtime interfaces");
        } else {
            if (!shaftId.equals(sourceReloaded.getCreateTiersReplacementSourceBlockId())) {
                helper.fail("Belt source shaft identity did not survive NBT serialize/reload");
            }
            if (!tier.equals(attachedReloaded.getTier())) {
                helper.fail("Reloaded belt no longer derives its effective tier from intrinsic source provenance");
            }
            if (!(reloaded instanceof KineticBlockEntity)) {
                helper.fail("Reloaded intrinsic belt lost its kinetic block entity");
            }

            // Even a direct legacy-state clear must not make provenance-derived tier state disappear.
            attachedReloaded.clearAttachedTier();
            if (!tier.equals(attachedReloaded.getTier())) {
                helper.fail("Clearing legacy attached data made an intrinsic-source belt temporarily untiered");
            }
        }

        helper.getLevel().destroyBlock(helper.absolutePos(start), false);
        BlockState restored = helper.getLevel().getBlockState(reloadedPos);
        if (!(restored.getBlock() instanceof TieredShaftBlock restoredShaft)
                || !tier.equals(restoredShaft.getTier())) {
            helper.fail("Reloaded intrinsic belt restored a vanilla/untiered shaft instead of its source tiered shaft");
        }

        GameTestSupport.succeed(helper, "tiered-shaft-belt");
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

        GameTestSupport.succeed(helper, "tiered-shaft-steam-engine");
    }


    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 40)
    public static void itemlessBeltCannotMintRegisteredTier(GameTestHelper helper) {
        Tier tier = GameTestSupport.ensureAttachmentTier();
        BlockState shaftState = AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Direction.Axis.X);
        BlockPos start = new BlockPos(1, 1, 1);
        BlockPos end = new BlockPos(1, 1, 5);
        helper.getLevel().setBlock(helper.absolutePos(start), shaftState, 3);
        helper.getLevel().setBlock(helper.absolutePos(end), shaftState, 3);

        BeltConnectorItem.createBelts(helper.getLevel(), helper.absolutePos(start), helper.absolutePos(end));
        BlockPos beltPos = helper.absolutePos(start);
        BlockEntity beltEntity = helper.getLevel().getBlockEntity(beltPos);
        if (!(beltEntity instanceof KineticBlockEntity kinetic)
                || !(beltEntity instanceof IAttachedTierBlockEntity attachable)) {
            helper.fail("Vanilla shaft belt endpoint did not become an attachable kinetic block entity");
            return;
        }

        try {
            attachable.setAttachedTier(tier);
            helper.fail("Itemless belt accepted a tier without a registered source item+tier pair");
        } catch (IllegalArgumentException expected) {
            // Expected: itemless states may only inherit an authorized tier from source provenance.
        }

        helper.getLevel().destroyBlock(beltPos, false);
        BlockPos restoredPos = helper.absolutePos(end);
        BlockState restored = helper.getLevel().getBlockState(restoredPos);
        if (!AllBlocks.SHAFT.has(restored) || restored.getBlock() instanceof TieredShaftBlock) {
            helper.fail("Untiered vanilla belt teardown minted an intrinsic tiered shaft");
        }
        BlockEntity restoredEntity = helper.getLevel().getBlockEntity(restoredPos);
        if (restoredEntity instanceof IAttachedTierBlockEntity restoredTier && restoredTier.getTier() != null) {
            helper.fail("Untiered vanilla belt teardown restored an attached tier");
        }

        GameTestSupport.succeed(helper, "itemless-belt-cannot-mint-tier");
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 40)
    public static void itemlessPoweredShaftCannotMintRegisteredTier(GameTestHelper helper) {
        Tier tier = GameTestSupport.ensureAttachmentTier();
        BlockState engineState = AllBlocks.STEAM_ENGINE.getDefaultState();
        Direction.Axis engineAxis = SteamEngineBlock.getFacing(engineState).getAxis();
        Direction.Axis shaftAxis = engineAxis == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X;
        BlockPos enginePos = helper.absolutePos(new BlockPos(4, 4, 4));
        BlockPos shaftPos = SteamEngineBlock.getShaftPos(engineState, enginePos);
        BlockState shaftState = AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, shaftAxis);
        helper.getLevel().setBlock(shaftPos, shaftState, 3);

        AllBlocks.STEAM_ENGINE.get().onPlace(
                engineState, helper.getLevel(), enginePos, Blocks.AIR.defaultBlockState(), false);
        BlockState poweredState = helper.getLevel().getBlockState(shaftPos);
        if (!AllBlocks.POWERED_SHAFT.has(poweredState)) {
            helper.fail("Steam engine did not create the vanilla itemless powered shaft");
        }

        BlockEntity poweredEntity = helper.getLevel().getBlockEntity(shaftPos);
        if (!(poweredEntity instanceof IAttachedTierBlockEntity attachable)) {
            helper.fail("Powered shaft did not expose attached-tier runtime state");
            return;
        }
        try {
            attachable.setAttachedTier(tier);
            helper.fail("Itemless powered shaft accepted a tier without registered source provenance");
        } catch (IllegalArgumentException expected) {
            // Expected.
        }

        ((PoweredShaftBlock) AllBlocks.POWERED_SHAFT.get())
                .tick(poweredState, helper.getLevel(), shaftPos, helper.getLevel().random);
        BlockState restored = helper.getLevel().getBlockState(shaftPos);
        if (!AllBlocks.SHAFT.has(restored) || restored.getBlock() instanceof TieredShaftBlock) {
            helper.fail("Untiered powered shaft recovery minted an intrinsic tiered shaft");
        }
        BlockEntity restoredEntity = helper.getLevel().getBlockEntity(shaftPos);
        if (restoredEntity instanceof IAttachedTierBlockEntity restoredTier && restoredTier.getTier() != null) {
            helper.fail("Untiered powered shaft recovery restored an attached tier");
        }

        GameTestSupport.succeed(helper, "itemless-powered-shaft-cannot-mint-tier");
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

        helper.getLevel().destroyBlock(helper.absolutePos(start), false);
        BlockPos restoredPos = helper.absolutePos(end);
        BlockState restored = helper.getLevel().getBlockState(restoredPos);
        if (!AllBlocks.SHAFT.has(restored) || restored.getBlock() instanceof TieredShaftBlock) {
            helper.fail("Tier-upgraded belt pulley did not restore the original vanilla Create shaft identity");
        }
        assertAttachedTierAt(helper, restoredPos, tier,
                "Belt pulley -> vanilla shaft restoration silently lost the attached tier");

        GameTestSupport.succeed(helper, "registered-shaft-belt-tier-preservation");
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

        GameTestSupport.succeed(helper, "registered-shaft-steam-tier-preservation");
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

        decase(helper, shaftPos);
        BlockState restoredShaft = helper.getLevel().getBlockState(shaftPos);
        if (!AllBlocks.SHAFT.has(restoredShaft) || restoredShaft.getBlock() instanceof TieredShaftBlock) {
            helper.fail("Standard Create encased shaft did not decase back to the vanilla shaft identity");
        }
        assertAttachedTierAt(helper, shaftPos, tier,
                "Encased shaft -> vanilla shaft conversion silently lost the attached tier");

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

        decase(helper, cogPos);
        BlockState restoredCog = helper.getLevel().getBlockState(cogPos);
        if (!AllBlocks.COGWHEEL.has(restoredCog)) {
            helper.fail("Standard Create encased cogwheel did not decase back to the vanilla cogwheel identity");
        }
        assertAttachedTierAt(helper, cogPos, tier,
                "Encased cogwheel -> vanilla cogwheel conversion silently lost the attached tier");

        GameTestSupport.succeed(helper, "registered-kinetic-encasing-tier-preservation");
    }

    private static void decase(GameTestHelper helper, BlockPos absolute) {
        BlockState state = helper.getLevel().getBlockState(absolute);
        if (!(state.getBlock() instanceof IWrenchable wrenchable)) {
            helper.fail("Expected a Create wrenchable encased kinetic block at " + absolute);
            return;
        }

        Player player = helper.makeMockPlayer();
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        UseOnContext context = new UseOnContext(player, InteractionHand.MAIN_HAND, hit);
        wrenchable.onSneakWrenched(state, context);
    }

    private static void attachTier(GameTestHelper helper, BlockPos absolute, Tier tier, String message) {
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(absolute);
        if (!(blockEntity instanceof IAttachedTierBlockEntity attachable)) {
            helper.fail(message + " (missing attached-tier interface)");
            return;
        }
        GameTestSupport.ensureUpgradeFor(blockEntity.getBlockState().getBlock(), tier);
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
