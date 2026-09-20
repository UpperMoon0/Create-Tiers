package com.createtiers.gametest;

import com.createtiers.CreateTiers;
import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import com.createtiers.api.TierUpgradeRegistry;
import com.createtiers.content.kinetics.TieredShaftBlockEntity;
import com.simibubi.create.AllBlockEntityTypes;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

final class GameTestSupport {
    static final String TEMPLATE = "empty";
    static final String ATTACHED_TIER_NBT_KEY = "CreateTiersTier";
    static final Tier LOW_TIER = new Tier(1, "gametest_low", 128, 512);
    static final Tier HIGH_TIER = new Tier(2, "gametest_high", 1024, 4096);
    static final ResourceLocation ATTACHMENT_TIER_ID =
            new ResourceLocation(CreateTiers.MOD_ID, "gametest_attachment");
    static final Tier ATTACHMENT_TIER =
            new Tier(Integer.MAX_VALUE - 7, "gametest_attachment", 1024, 4096);

    private GameTestSupport() {
    }

    static Tier ensureAttachmentTier() {
        Tier tier = TierRegistry.get(ATTACHMENT_TIER_ID);
        if (tier == null) {
            TierRegistry.unfreeze();
            try {
                tier = TierRegistry.register(ATTACHMENT_TIER_ID, ATTACHMENT_TIER);
            } finally {
                TierRegistry.freeze();
            }
        }

        ResourceLocation shaftItemId = BuiltInRegistries.ITEM.getKey(AllBlocks.SHAFT.get().asItem());
        if (!TierUpgradeRegistry.isRegistered(shaftItemId, ATTACHMENT_TIER_ID)) {
            TierUpgradeRegistry.unfreeze();
            try {
                TierUpgradeRegistry.register(shaftItemId, ATTACHMENT_TIER_ID, false);
            } finally {
                TierUpgradeRegistry.freeze();
            }
        }
        return tier;
    }

    static IAttachedTierBlockEntity requireAttachable(GameTestHelper helper, KineticBlockEntity kinetic) {
        if (kinetic instanceof IAttachedTierBlockEntity attachable) {
            return attachable;
        }
        helper.fail("Create KineticBlockEntity did not receive the generic tier attachment mixin");
        throw new IllegalStateException("GameTest failure did not abort");
    }

    static void assertAttachedTier(GameTestHelper helper, IAttachedTierBlockEntity attachable, Tier tier,
            String message) {
        if (!tier.equals(attachable.getTier()) || !ATTACHMENT_TIER_ID.equals(attachable.getAttachedTierId())) {
            helper.fail(message
                    + " (expected tier=" + tier
                    + ", actual tier=" + attachable.getTier()
                    + ", expected id=" + ATTACHMENT_TIER_ID
                    + ", actual id=" + attachable.getAttachedTierId()
                    + ", registry tier=" + TierRegistry.get(ATTACHMENT_TIER_ID) + ")");
        }
    }

    static void assertPropagation(GameTestHelper helper, BlockPos sourceRelative, Tier sourceTier,
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

    static KineticNetwork network(float capacity, KineticBlockEntity... members) {
        KineticNetwork network = new KineticNetwork();
        for (KineticBlockEntity member : members) {
            network.members.put(member, 0f);
        }
        network.initFromTE(capacity, 0f, 0);
        return network;
    }

    static KineticBlockEntity placeKinetic(GameTestHelper helper, BlockPos relative) {
        return placeBlockEntity(helper, relative, shaftState(), KineticBlockEntity.class);
    }

    static <T extends BlockEntity> T placeBlockEntity(GameTestHelper helper, BlockPos relative,
            BlockState state, Class<T> expectedType) {
        BlockPos absolute = helper.absolutePos(relative);
        helper.getLevel().setBlock(absolute, state, 3);
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(absolute);
        if (expectedType.isInstance(blockEntity)) {
            return expectedType.cast(blockEntity);
        }
        helper.fail("Expected " + expectedType.getSimpleName() + " at " + relative);
        throw new IllegalStateException("GameTest failure did not abort");
    }

    static KineticBlockEntity placeTieredKinetic(GameTestHelper helper, BlockPos relative, Tier tier) {
        BlockPos absolute = helper.absolutePos(relative);
        BlockState state = shaftState();
        helper.getLevel().setBlock(absolute, state, 3);
        helper.getLevel().removeBlockEntity(absolute);

        BlockState liveState = helper.getLevel().getBlockState(absolute);
        TestTieredShaftBlockEntity blockEntity = new TestTieredShaftBlockEntity(absolute, liveState, tier);
        helper.getLevel().setBlockEntity(blockEntity);
        return blockEntity;
    }

    static BlockState shaftState() {
        return AllBlocks.SHAFT.get().defaultBlockState()
                .setValue(BlockStateProperties.AXIS, Direction.Axis.X);
    }

    static void assertFloat(GameTestHelper helper, float expected, float actual, String message) {
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
