package com.createtiers.gametest;

import com.createtiers.CreateTiers;
import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import com.createtiers.content.kinetics.TieredShaftBlockEntity;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateTiers.MOD_ID)
public final class CreateTiersGameTests {

    private static final String TEMPLATE = "empty";
    private static final Tier LOW_TIER = new Tier(1, "gametest_low", 128, 512);
    private static final Tier HIGH_TIER = new Tier(2, "gametest_high", 1024, 4096);
    private static final ResourceLocation ATTACHMENT_TIER_ID =
            ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, "gametest_attachment");
    private static final Tier ATTACHMENT_TIER =
            new Tier(Integer.MAX_VALUE - 7, "gametest_attachment", 1024, 4096);

    private CreateTiersGameTests() {
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void receiverScopedRpmLimits(GameTestHelper helper) {
        int createMax = AllConfigs.server().kinetics.maxRotationSpeed.get();
        if (HIGH_TIER.getMaxRPM() <= createMax || LOW_TIER.getMaxRPM() >= createMax) {
            helper.fail("GameTest tiers no longer bracket Create's configured RPM limit of " + createMax);
        }

        // Tiered receiver above Create's ordinary limit: allowed by the receiver's tier.
        assertPropagation(helper, new BlockPos(1, 1, 1), null, HIGH_TIER, createMax + 1f, true);

        // Tiered receiver below Create's ordinary limit: rejected by the receiver's tier.
        assertPropagation(helper, new BlockPos(1, 1, 4), null, LOW_TIER, LOW_TIER.getMaxRPM() + 1f, false);

        // High-tier source -> untiered Create receiver: the receiver still enforces Create's configured limit.
        assertPropagation(helper, new BlockPos(5, 1, 1), HIGH_TIER, null, createMax + 1f, false);
        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void connectedNetworkUsesLowestTierSuCap(GameTestHelper helper) {
        KineticBlockEntity lowEntity = placeTieredKinetic(helper, new BlockPos(1, 1, 1), LOW_TIER);
        KineticBlockEntity highEntity = placeTieredKinetic(helper, new BlockPos(4, 1, 1), HIGH_TIER);

        KineticNetwork network = new KineticNetwork();
        network.members.put(lowEntity, 0f);
        network.members.put(highEntity, 0f);
        network.initFromTE(10_000f, 0f, 0);

        assertFloat(helper, LOW_TIER.getMaxSU(), network.calculateCapacity(),
                "Connected network did not clamp capacity to its lowest tier Max SU");

        lowEntity.setSpeed(LOW_TIER.getMaxRPM() + 1f);
        assertFloat(helper, 0f, network.calculateCapacity(),
                "Tier overspeed did not zero the connected network capacity");
        helper.succeed();
    }

    @PrefixGameTestTemplate(false)
    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void ordinaryCreateKineticAcceptsAttachedTier(GameTestHelper helper) {
        Tier tier = ensureAttachmentTier();
        KineticBlockEntity kinetic = placeKinetic(helper, new BlockPos(1, 1, 1));
        if (!(kinetic instanceof IAttachedTierBlockEntity attachable)) {
            helper.fail("Create KineticBlockEntity did not receive the generic tier attachment mixin");
            return;
        }

        attachable.setAttachedTier(tier);
        if (!tier.equals(attachable.getTier()) || !ATTACHMENT_TIER_ID.equals(attachable.getAttachedTierId())) {
            helper.fail("Ordinary Create kinetic component did not expose its attached tier");
        }

        kinetic.setSpeed(tier.getMaxRPM() + 1f);
        KineticNetwork network = new KineticNetwork();
        network.members.put(kinetic, 0f);
        network.initFromTE(10_000f, 0f, 0);
        assertFloat(helper, 0f, network.calculateCapacity(),
                "Attached tier RPM limit was not enforced for an ordinary Create kinetic component");

        attachable.clearAttachedTier();
        if (attachable.getTier() != null || attachable.getAttachedTierId() != null) {
            helper.fail("Clearing an attached tier did not restore ordinary Create tier state");
        }
        helper.succeed();
    }

    private static Tier ensureAttachmentTier() {
        Tier existing = TierRegistry.get(ATTACHMENT_TIER_ID);
        if (existing != null) {
            return existing;
        }

        TierRegistry.unfreeze();
        try {
            return TierRegistry.register(ATTACHMENT_TIER_ID, ATTACHMENT_TIER);
        } finally {
            TierRegistry.freeze();
        }
    }

    private static void assertPropagation(GameTestHelper helper, BlockPos sourceRelative, Tier sourceTier,
            Tier receiverTier, float speed, boolean shouldPropagate) {
        KineticBlockEntity source = sourceTier == null
                ? placeKinetic(helper, sourceRelative)
                : placeTieredKinetic(helper, sourceRelative, sourceTier);
        KineticBlockEntity target = receiverTier == null
                ? placeKinetic(helper, sourceRelative.east())
                : placeTieredKinetic(helper, sourceRelative.east(), receiverTier);

        source.setSpeed(speed);
        source.setNetwork(source.getBlockPos().asLong());
        RotationPropagator.handleAdded(helper.getLevel(), source.getBlockPos(), source);

        boolean sourceSurvived = !helper.getLevel().getBlockState(source.getBlockPos()).isAir();
        if (sourceSurvived != shouldPropagate) {
            helper.fail(shouldPropagate
                    ? "Valid receiver-scoped RPM propagation destroyed the source block"
                    : "Receiver accepted RPM above its allowed limit");
        }
        if (shouldPropagate) {
            assertFloat(helper, speed, target.getTheoreticalSpeed(), "Receiver did not inherit the conveyed speed");
        }
    }

    private static KineticBlockEntity placeKinetic(GameTestHelper helper, BlockPos relative) {
        BlockPos absolute = helper.absolutePos(relative);
        BlockState state = shaftState();
        helper.getLevel().setBlock(absolute, state, 3);
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(absolute);
        if (blockEntity instanceof KineticBlockEntity kinetic) {
            return kinetic;
        }
        helper.fail("Expected Create shaft kinetic block entity at " + relative);
        throw new IllegalStateException("GameTest failure did not abort");
    }

    private static KineticBlockEntity placeTieredKinetic(GameTestHelper helper, BlockPos relative, Tier tier) {
        BlockPos absolute = helper.absolutePos(relative);
        BlockState state = shaftState();
        helper.getLevel().setBlock(absolute, state, 3);
        helper.getLevel().removeBlockEntity(absolute);

        BlockState liveState = helper.getLevel().getBlockState(absolute);
        TestTieredShaftBlockEntity blockEntity = new TestTieredShaftBlockEntity(absolute, liveState, tier);
        helper.getLevel().setBlockEntity(blockEntity);
        return blockEntity;
    }

    private static BlockState shaftState() {
        return AllBlocks.SHAFT.get().defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X);
    }

    private static void assertFloat(GameTestHelper helper, float expected, float actual, String message) {
        if (Math.abs(expected - actual) > 0.0001f) {
            helper.fail(message + " (expected " + expected + ", got " + actual + ")");
        }
    }

    private static final class TestTieredShaftBlockEntity extends TieredShaftBlockEntity {
        private final Tier tier;

        private TestTieredShaftBlockEntity(BlockPos pos, BlockState state, Tier tier) {
            super(AllBlockEntityTypes.BRACKETED_KINETIC.get(), pos, state);
            this.tier = tier;
        }

        @Override
        public Tier getTier() {
            return tier;
        }
    }
}
