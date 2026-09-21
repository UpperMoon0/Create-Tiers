package com.createtiers.gametest;

import com.createtiers.CreateTiers;
import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.IReplacementSourceBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import com.createtiers.content.kinetics.TieredPoweredShaftBlock;
import com.createtiers.content.kinetics.TieredPoweredShaftBlockEntity;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.createtiers.foundation.item.TierUpgradeItemData;
import com.createtiers.foundation.utility.TieredBeltPulleyInteraction;
import com.createtiers.registry.ModBlocks;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.encasing.EncasedBlock;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltHelper;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.kinetics.belt.BeltSlicer;
import com.simibubi.create.content.kinetics.belt.item.BeltConnectorItem;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlock;
import com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraft.world.level.block.Block;
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
    public static void placementHelperPreservesRegisteredShaftTier(GameTestHelper helper) {
        Tier tier = GameTestSupport.ensureAttachmentTier();
        BlockState shaftState = AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Direction.Axis.X);
        BlockPos base = helper.absolutePos(new BlockPos(4, 2, 4));
        helper.getLevel().setBlock(base, shaftState, 3);

        Player player = helper.makeMockPlayer();
        ItemStack upgraded = TierUpgradeItemData.upgradedCopy(AllBlocks.SHAFT.asStack(), tier);
        player.setItemInHand(InteractionHand.MAIN_HAND, upgraded);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(base).add(.49, 0, 0), Direction.EAST, base, false);

        AllBlocks.SHAFT.get().use(
                shaftState, helper.getLevel(), base, player, InteractionHand.MAIN_HAND, hit);

        BlockPos placed = AllBlocks.SHAFT.has(helper.getLevel().getBlockState(base.east()))
                ? base.east()
                : base.west();
        if (!AllBlocks.SHAFT.has(helper.getLevel().getBlockState(placed))) {
            helper.fail("Create shaft placement helper did not extend the shaft");
        }
        assertAttachedTierAt(helper, placed, tier,
                "PlacementOffset shaft extension consumed a registered upgraded item without applying its tier");
        GameTestSupport.succeed(helper, "placement-helper-shaft-tier-preservation");
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 40)
    public static void placementHelperPreservesRegisteredSteamShaftTier(GameTestHelper helper) {
        Tier tier = GameTestSupport.ensureAttachmentTier();
        BlockState engineState = AllBlocks.STEAM_ENGINE.getDefaultState();
        BlockPos enginePos = helper.absolutePos(new BlockPos(4, 4, 4));
        helper.getLevel().setBlock(enginePos, engineState, 3);
        BlockPos shaftPos = SteamEngineBlock.getShaftPos(engineState, enginePos);

        Player player = helper.makeMockPlayer();
        ItemStack upgraded = TierUpgradeItemData.upgradedCopy(AllBlocks.SHAFT.asStack(), tier);
        player.setItemInHand(InteractionHand.MAIN_HAND, upgraded);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(enginePos), Direction.UP, enginePos, false);

        AllBlocks.STEAM_ENGINE.get().use(
                engineState, helper.getLevel(), enginePos, player, InteractionHand.MAIN_HAND, hit);

        BlockState poweredState = helper.getLevel().getBlockState(shaftPos);
        if (!AllBlocks.POWERED_SHAFT.has(poweredState)) {
            helper.fail("Steam Engine placement helper did not place Create's powered shaft");
        }
        assertAttachedTierAt(helper, shaftPos, tier,
                "Steam Engine PlacementOffset consumed a registered upgraded shaft without applying its tier");
        BlockEntity poweredEntity = helper.getLevel().getBlockEntity(shaftPos);
        ResourceLocation expectedSource = BuiltInRegistries.BLOCK.getKey(AllBlocks.SHAFT.get());
        if (!(poweredEntity instanceof IReplacementSourceBlockEntity source)
                || !expectedSource.equals(source.getCreateTiersReplacementSourceBlockId())) {
            helper.fail("Steam helper placement did not retain the upgraded vanilla shaft as powered-shaft provenance");
        }

        helper.getLevel().setBlock(enginePos, Blocks.AIR.defaultBlockState(), 3);
        ((PoweredShaftBlock) AllBlocks.POWERED_SHAFT.get())
                .tick(poweredState, helper.getLevel(), shaftPos, helper.getLevel().random);
        if (!AllBlocks.SHAFT.has(helper.getLevel().getBlockState(shaftPos))) {
            helper.fail("Placed powered shaft did not recover to a vanilla shaft after engine removal");
        }
        assertAttachedTierAt(helper, shaftPos, tier,
                "Powered shaft recovery lost the tier from helper-placed upgraded shaft");
        GameTestSupport.succeed(helper, "placement-helper-steam-tier-preservation");
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 40)
    public static void registeredShaftItemPulleyWrenchRoundTrip(GameTestHelper helper) {
        Tier tier = GameTestSupport.ensureAttachmentTier();
        BlockPos middle = createPlainMiddleBelt(helper);
        BlockState middleState = helper.getLevel().getBlockState(middle);

        Player player = helper.makeMockPlayer();
        ItemStack upgraded = TierUpgradeItemData.upgradedCopy(AllBlocks.SHAFT.asStack(), tier);
        player.setItemInHand(InteractionHand.MAIN_HAND, upgraded);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(middle), Direction.UP, middle, false);

        ((BeltBlock) AllBlocks.BELT.get()).use(
                middleState, helper.getLevel(), middle, player, InteractionHand.MAIN_HAND, hit);

        BlockState pulleyState = helper.getLevel().getBlockState(middle);
        if (pulleyState.getValue(BeltBlock.PART) != BeltPart.PULLEY) {
            helper.fail("Registered upgraded shaft item did not add a pulley to a middle belt");
        }
        assertAttachedTierAt(helper, middle, tier,
                "Registered upgraded shaft item lost its tier when adding a belt pulley");
        BlockEntity pulleyEntity = helper.getLevel().getBlockEntity(middle);
        ResourceLocation vanillaShaftId = BuiltInRegistries.BLOCK.getKey(AllBlocks.SHAFT.get());
        if (!(pulleyEntity instanceof IReplacementSourceBlockEntity source)
                || !vanillaShaftId.equals(source.getCreateTiersReplacementSourceBlockId())) {
            helper.fail("Registered shaft pulley did not record vanilla shaft source provenance");
        }

        ((BeltBlock) AllBlocks.BELT.get()).onWrenched(
                pulleyState, new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        assertMiddleBeltHasNoTierSource(helper, middle,
                "Wrenching registered shaft pulley left tier/source provenance on middle belt");
        if (!inventoryContainsTieredShaft(player, tier)) {
            helper.fail("Wrenching registered shaft pulley did not return the upgraded shaft item");
        }
        GameTestSupport.succeed(helper, "registered-pulley-item-roundtrip");
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 40)
    public static void intrinsicShaftItemPulleyWrenchRoundTrip(GameTestHelper helper) {
        TieredShaftBlock shaft = requireTieredShaft(helper);
        Tier tier = shaft.getTier();
        BlockPos middle = createPlainMiddleBelt(helper);
        BlockState middleState = helper.getLevel().getBlockState(middle);

        Player player = helper.makeMockPlayer();
        ItemStack nativeShaft = shaft.asItem().getDefaultInstance();
        player.setItemInHand(InteractionHand.MAIN_HAND, nativeShaft);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(middle), Direction.UP, middle, false);

        ((BeltBlock) AllBlocks.BELT.get()).use(
                middleState, helper.getLevel(), middle, player, InteractionHand.MAIN_HAND, hit);

        BlockState pulleyState = helper.getLevel().getBlockState(middle);
        if (pulleyState.getValue(BeltBlock.PART) != BeltPart.PULLEY) {
            helper.fail("Native tiered shaft item did not perform Create's middle-belt pulley interaction");
        }
        BlockEntity pulleyEntity = helper.getLevel().getBlockEntity(middle);
        if (!(pulleyEntity instanceof IAttachedTierBlockEntity attachable)
                || !tier.equals(attachable.getTier())
                || attachable.getAttachedTier() != null) {
            helper.fail("Native tiered shaft pulley did not derive its intrinsic tier from source provenance");
        }
        ResourceLocation shaftId = BuiltInRegistries.BLOCK.getKey(shaft);
        if (!(pulleyEntity instanceof IReplacementSourceBlockEntity source)
                || !shaftId.equals(source.getCreateTiersReplacementSourceBlockId())) {
            helper.fail("Native tiered shaft item did not record intrinsic source provenance");
        }

        ((BeltBlock) AllBlocks.BELT.get()).onWrenched(
                pulleyState, new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        assertMiddleBeltHasNoTierSource(helper, middle,
                "Wrenching intrinsic tiered pulley left tier/source provenance on middle belt");
        if (!inventoryContainsItem(player, shaft.asItem())) {
            helper.fail("Wrenching intrinsic tiered pulley did not return the intrinsic shaft item");
        }
        GameTestSupport.succeed(helper, "intrinsic-pulley-item-roundtrip");
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 40)
    public static void registeredPulleyDirectMiningReturnsUpgradedShaft(GameTestHelper helper) {
        Tier tier = GameTestSupport.ensureAttachmentTier();
        BeltPositions belt = createRegisteredSourceBelt(helper, tier);
        Player player = makeSurvivalPlayer(helper);
        addPulley(helper, belt.middle(), player,
                TierUpgradeItemData.upgradedCopy(AllBlocks.SHAFT.asStack(), tier));

        for (BlockPos pos : new BlockPos[]{belt.start(), belt.middle(), belt.end()}) {
            BlockState state = helper.getLevel().getBlockState(pos);
            BeltPart part = state.getValue(BeltBlock.PART);
            BlockEntity blockEntity = helper.getLevel().getBlockEntity(pos);
            java.util.List<ItemStack> drops = Block.getDrops(
                    state, helper.getLevel(), pos, blockEntity);

            boolean upgradedRefund = drops.stream()
                    .anyMatch(stack -> stack.is(AllBlocks.SHAFT.get().asItem())
                            && tier.equals(TierUpgradeItemData.getTier(stack)));
            boolean plainRefund = drops.stream()
                    .anyMatch(stack -> stack.is(AllBlocks.SHAFT.get().asItem())
                            && TierUpgradeItemData.getTier(stack) == null);
            if (!upgradedRefund || plainRefund) {
                helper.fail("Directly mining registered-tier belt part " + part
                        + " did not refund exactly the upgraded shaft");
            }
        }

        GameTestSupport.succeed(helper, "registered-pulley-direct-mining");
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 40)
    public static void intrinsicPulleyDirectMiningReturnsIntrinsicShaft(GameTestHelper helper) {
        TieredShaftBlock shaft = requireTieredShaft(helper);
        BeltPositions belt = createIntrinsicSourceBelt(helper, shaft);
        Player player = makeSurvivalPlayer(helper);
        addPulley(helper, belt.middle(), player, shaft.asItem().getDefaultInstance());

        for (BlockPos pos : new BlockPos[]{belt.start(), belt.middle(), belt.end()}) {
            BlockState state = helper.getLevel().getBlockState(pos);
            BeltPart part = state.getValue(BeltBlock.PART);
            BlockEntity blockEntity = helper.getLevel().getBlockEntity(pos);
            java.util.List<ItemStack> drops = Block.getDrops(
                    state, helper.getLevel(), pos, blockEntity);

            if (drops.stream().noneMatch(stack -> stack.is(shaft.asItem()))) {
                helper.fail("Directly mining intrinsic-tier belt part " + part
                        + " did not refund its source shaft");
            }
            if (drops.stream().anyMatch(stack -> stack.is(AllBlocks.SHAFT.get().asItem()))) {
                helper.fail("Directly mining intrinsic-tier belt part " + part
                        + " leaked a vanilla Create shaft");
            }
        }

        GameTestSupport.succeed(helper, "intrinsic-pulley-direct-mining");
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 40)
    public static void registeredPulleyBeltSlicerShorteningPreservesSource(GameTestHelper helper) {
        Tier endpointTier = GameTestSupport.ensureAttachmentTier();
        TieredShaftBlock overwrittenPulleyShaft = requireTieredShaft(helper);
        BlockState endpointShaftState = AllBlocks.SHAFT.getDefaultState()
                .setValue(ShaftBlock.AXIS, Direction.Axis.X);
        ShorteningSetup setup = createShorteningSetup(helper, endpointShaftState, endpointTier);
        Player player = makeSurvivalPlayer(helper);
        addPulley(helper, setup.inner(), player, overwrittenPulleyShaft.asItem().getDefaultInstance());
        helper.runAfterDelay(2, () -> {
            if (BeltHelper.getControllerBE(helper.getLevel(), setup.end()) == null) {
                helper.fail("Create belt controller metadata was not initialized before shortening");
            }
            shortenBelt(helper, setup, player);
            BlockState endpoint = helper.getLevel().getBlockState(setup.inner());
            if (!AllBlocks.BELT.has(endpoint) || endpoint.getValue(BeltBlock.PART) == BeltPart.PULLEY) {
                helper.fail("BeltSlicer did not turn the adjacent pulley into the shortened belt endpoint");
            }
            assertAttachedTierAt(helper, setup.inner(), endpointTier,
                    "BeltSlicer shortening did not move the registered old-endpoint tier inward");
            BlockEntity endpointEntity = helper.getLevel().getBlockEntity(setup.inner());
            ResourceLocation shaftId = BuiltInRegistries.BLOCK.getKey(AllBlocks.SHAFT.get());
            if (!(endpointEntity instanceof IReplacementSourceBlockEntity source)
                    || !shaftId.equals(source.getCreateTiersReplacementSourceBlockId())) {
                helper.fail("BeltSlicer shortening did not move registered old-endpoint provenance inward");
            }
            if (!inventoryContainsItem(player, overwrittenPulleyShaft.asItem())) {
                helper.fail("BeltSlicer shortening did not refund the overwritten intrinsic pulley source");
            }
            if (inventoryContainsTieredShaft(player, endpointTier)) {
                helper.fail("BeltSlicer shortening duplicated the registered old-endpoint source into inventory");
            }
            GameTestSupport.succeed(helper, "registered-pulley-slicer-shortening");
        });
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 40)
    public static void intrinsicPulleyBeltSlicerShorteningPreservesSource(GameTestHelper helper) {
        TieredShaftBlock endpointShaft = requireTieredShaft(helper);
        Tier endpointTier = endpointShaft.getTier();
        Tier overwrittenPulleyTier = GameTestSupport.ensureAttachmentTier();
        BlockState endpointShaftState = endpointShaft.defaultBlockState()
                .setValue(ShaftBlock.AXIS, Direction.Axis.X);
        ShorteningSetup setup = createShorteningSetup(helper, endpointShaftState, null);
        Player player = makeSurvivalPlayer(helper);
        addPulley(helper, setup.inner(), player,
                TierUpgradeItemData.upgradedCopy(AllBlocks.SHAFT.asStack(), overwrittenPulleyTier));
        helper.runAfterDelay(2, () -> {
            if (BeltHelper.getControllerBE(helper.getLevel(), setup.end()) == null) {
                helper.fail("Create belt controller metadata was not initialized before shortening");
            }
            shortenBelt(helper, setup, player);
            BlockState endpoint = helper.getLevel().getBlockState(setup.inner());
            if (!AllBlocks.BELT.has(endpoint) || endpoint.getValue(BeltBlock.PART) == BeltPart.PULLEY) {
                helper.fail("BeltSlicer did not turn the adjacent pulley into the shortened belt endpoint");
            }
            BlockEntity endpointEntity = helper.getLevel().getBlockEntity(setup.inner());
            ResourceLocation shaftId = BuiltInRegistries.BLOCK.getKey(endpointShaft);
            if (!(endpointEntity instanceof IAttachedTierBlockEntity attachable)
                    || !endpointTier.equals(attachable.getTier())
                    || attachable.getAttachedTier() != null) {
                helper.fail("BeltSlicer shortening did not move the intrinsic old-endpoint tier inward");
            }
            if (!(endpointEntity instanceof IReplacementSourceBlockEntity source)
                    || !shaftId.equals(source.getCreateTiersReplacementSourceBlockId())) {
                helper.fail("BeltSlicer shortening did not move intrinsic old-endpoint provenance inward");
            }
            if (!inventoryContainsTieredShaft(player, overwrittenPulleyTier)) {
                helper.fail("BeltSlicer shortening did not refund the overwritten registered pulley source");
            }
            if (inventoryContainsItem(player, endpointShaft.asItem())) {
                helper.fail("BeltSlicer shortening duplicated the intrinsic old-endpoint source into inventory");
            }
            GameTestSupport.succeed(helper, "intrinsic-pulley-slicer-shortening");
        });
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

    private record BeltPositions(BlockPos start, BlockPos middle, BlockPos end) {
    }

    private static BeltPositions createRegisteredSourceBelt(GameTestHelper helper, Tier tier) {
        BlockState shaftState = AllBlocks.SHAFT.getDefaultState()
                .setValue(ShaftBlock.AXIS, Direction.Axis.X);
        BlockPos start = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos middle = helper.absolutePos(new BlockPos(1, 1, 3));
        BlockPos end = helper.absolutePos(new BlockPos(1, 1, 5));
        helper.getLevel().setBlock(start, shaftState, 3);
        helper.getLevel().setBlock(end, shaftState, 3);
        attachTier(helper, start, tier,
                "GameTest setup could not attach the registered start source");
        attachTier(helper, end, tier,
                "GameTest setup could not attach the registered end source");
        BeltConnectorItem.createBelts(helper.getLevel(), start, end);
        assertBeltEndpoints(helper, start, middle, end);
        return new BeltPositions(start, middle, end);
    }

    private static BeltPositions createIntrinsicSourceBelt(
            GameTestHelper helper, TieredShaftBlock shaft) {
        BlockState shaftState = shaft.defaultBlockState()
                .setValue(ShaftBlock.AXIS, Direction.Axis.X);
        BlockPos start = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos middle = helper.absolutePos(new BlockPos(1, 1, 3));
        BlockPos end = helper.absolutePos(new BlockPos(1, 1, 5));
        helper.getLevel().setBlock(start, shaftState, 3);
        helper.getLevel().setBlock(end, shaftState, 3);
        BeltConnectorItem.createBelts(helper.getLevel(), start, end);
        assertBeltEndpoints(helper, start, middle, end);
        return new BeltPositions(start, middle, end);
    }

    private static void assertBeltEndpoints(
            GameTestHelper helper, BlockPos start, BlockPos middle, BlockPos end) {
        BlockState startState = helper.getLevel().getBlockState(start);
        BlockState middleState = helper.getLevel().getBlockState(middle);
        BlockState endState = helper.getLevel().getBlockState(end);
        if (!AllBlocks.BELT.has(startState)
                || !AllBlocks.BELT.has(endState)
                || (startState.getValue(BeltBlock.PART) != BeltPart.START
                && startState.getValue(BeltBlock.PART) != BeltPart.END)
                || (endState.getValue(BeltBlock.PART) != BeltPart.START
                && endState.getValue(BeltBlock.PART) != BeltPart.END)
                || !AllBlocks.BELT.has(middleState)
                || middleState.getValue(BeltBlock.PART) != BeltPart.MIDDLE) {
            helper.fail("GameTest setup did not create START/END endpoints with a middle segment");
        }
    }

    private record ShorteningSetup(BlockPos end, BlockPos inner, BlockState endState) {
    }

    private static ShorteningSetup createShorteningSetup(
            GameTestHelper helper, BlockState endShaftState, Tier endAttachedTier) {
        BlockState shaftState = AllBlocks.SHAFT.getDefaultState()
                .setValue(ShaftBlock.AXIS, Direction.Axis.X);
        BlockPos start = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos end = helper.absolutePos(new BlockPos(1, 1, 5));
        helper.getLevel().setBlock(start, shaftState, 3);
        helper.getLevel().setBlock(end, endShaftState, 3);
        if (endAttachedTier != null) {
            attachTier(helper, end, endAttachedTier,
                    "GameTest setup could not attach the old endpoint tier before belt creation");
        }
        BeltConnectorItem.createBelts(helper.getLevel(), start, end);
        BlockState endState = helper.getLevel().getBlockState(end);
        if (!AllBlocks.BELT.has(endState)
                || (endState.getValue(BeltBlock.PART) != BeltPart.END
                && endState.getValue(BeltBlock.PART) != BeltPart.START)) {
            helper.fail("GameTest setup did not create a belt endpoint for shortening");
        }
        BlockPos vector = BlockPos.containing(BeltHelper.getBeltVector(endState));
        BlockPos inner = endState.getValue(BeltBlock.PART) == BeltPart.END
                ? end.subtract(vector)
                : end.offset(vector);
        BlockState innerState = helper.getLevel().getBlockState(inner);
        if (!AllBlocks.BELT.has(innerState) || innerState.getValue(BeltBlock.PART) != BeltPart.MIDDLE) {
            helper.fail("GameTest setup did not create a middle segment adjacent to the belt endpoint");
        }
        return new ShorteningSetup(end, inner, endState);
    }

    private static void addPulley(
            GameTestHelper helper, BlockPos pos, Player player, ItemStack sourceStack) {
        BlockState state = helper.getLevel().getBlockState(pos);
        if (!TieredBeltPulleyInteraction.tryAddPulley(
                sourceStack, state, helper.getLevel(), pos, player)) {
            helper.fail("GameTest setup could not add a tier-aware pulley");
        }
        if (helper.getLevel().getBlockState(pos).getValue(BeltBlock.PART) != BeltPart.PULLEY) {
            helper.fail("GameTest setup did not produce a pulley");
        }
    }

    private static void shortenBelt(
            GameTestHelper helper, ShorteningSetup setup, Player player) {
        BlockState endState = helper.getLevel().getBlockState(setup.end());
        Vec3 beltVector = BeltHelper.getBeltVector(endState);
        double direction = endState.getValue(BeltBlock.PART) == BeltPart.END ? 0.49 : -0.49;
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(setup.end()).add(beltVector.scale(direction)),
                Direction.UP, setup.end(), false);

        BeltSlicer.useWrench(
                endState, helper.getLevel(), setup.end(), player,
                InteractionHand.MAIN_HAND, hit, new BeltSlicer.Feedback());
    }

    private static Player makeSurvivalPlayer(GameTestHelper helper) {
        return helper.makeMockSurvivalPlayer();
    }

    private static BlockPos createPlainMiddleBelt(GameTestHelper helper) {
        BlockState shaftState = AllBlocks.SHAFT.getDefaultState().setValue(ShaftBlock.AXIS, Direction.Axis.X);
        BlockPos start = helper.absolutePos(new BlockPos(1, 1, 1));
        BlockPos end = helper.absolutePos(new BlockPos(1, 1, 5));
        BlockPos middle = helper.absolutePos(new BlockPos(1, 1, 3));
        helper.getLevel().setBlock(start, shaftState, 3);
        helper.getLevel().setBlock(end, shaftState, 3);
        BeltConnectorItem.createBelts(helper.getLevel(), start, end);
        BlockState middleState = helper.getLevel().getBlockState(middle);
        if (!AllBlocks.BELT.has(middleState) || middleState.getValue(BeltBlock.PART) != BeltPart.MIDDLE) {
            helper.fail("GameTest setup did not create a plain middle belt segment");
        }
        return middle;
    }

    private static void assertMiddleBeltHasNoTierSource(GameTestHelper helper, BlockPos absolute, String message) {
        BlockState state = helper.getLevel().getBlockState(absolute);
        if (!AllBlocks.BELT.has(state) || state.getValue(BeltBlock.PART) != BeltPart.MIDDLE) {
            helper.fail(message + " (not a middle belt)");
            return;
        }
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(absolute);
        if (!(blockEntity instanceof IAttachedTierBlockEntity attachable)
                || !(blockEntity instanceof IReplacementSourceBlockEntity source)) {
            helper.fail(message + " (missing Create Tiers interfaces)");
            return;
        }
        if (attachable.getTier() != null || attachable.getAttachedTier() != null
                || source.getCreateTiersReplacementSourceBlockId() != null) {
            helper.fail(message);
        }
        CompoundTag saved = blockEntity.saveWithFullMetadata();
        if (saved.contains(GameTestSupport.ATTACHED_TIER_NBT_KEY)
                || saved.contains(GameTestSupport.REPLACEMENT_SOURCE_NBT_KEY)) {
            helper.fail(message + " (stale tier/source remained in persisted NBT)");
        }
    }

    private static boolean inventoryContainsTieredShaft(Player player, Tier tier) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(AllBlocks.SHAFT.get().asItem()) && tier.equals(TierUpgradeItemData.getTier(stack))) {
                return true;
            }
        }
        return false;
    }

    private static boolean inventoryContainsItem(Player player, net.minecraft.world.item.Item item) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (player.getInventory().getItem(slot).is(item)) {
                return true;
            }
        }
        return false;
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
