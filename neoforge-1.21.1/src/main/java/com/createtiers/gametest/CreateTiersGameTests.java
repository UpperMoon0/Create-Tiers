package com.createtiers.gametest;

import com.createtiers.CreateTiers;
import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateTiers.MOD_ID)
@PrefixGameTestTemplate(false)
public final class CreateTiersGameTests {

    private static final String TEMPLATE = "empty";
    private static final String LOW_TIER = "gametest_low";
    private static final String HIGH_TIER = "gametest_high";

    private CreateTiersGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void receiverScopedRpmLimits(GameTestHelper helper) {
        Tier low = requireTier(helper, LOW_TIER);
        Tier high = requireTier(helper, HIGH_TIER);
        int createMax = AllConfigs.server().kinetics.maxRotationSpeed.get();

        if (high.getMaxRPM() <= createMax) {
            helper.fail("GameTest high tier must exceed Create's configured RPM limit");
        }
        if (low.getMaxRPM() >= createMax) {
            helper.fail("GameTest low tier must be below Create's configured RPM limit");
        }

        Block highShaft = requireBlock(helper, "shaft_" + HIGH_TIER);
        Block lowShaft = requireBlock(helper, "shaft_" + LOW_TIER);

        // A high-tier receiver accepts RPM above Create's ordinary limit.
        assertPropagation(helper, new BlockPos(1, 1, 1), highShaft, highShaft, createMax + 1f, true);

        // A low-tier receiver rejects RPM above its own tier limit even though Create itself would allow it.
        assertPropagation(helper, new BlockPos(1, 1, 4), highShaft, lowShaft, low.getMaxRPM() + 1f, false);

        // An untiered Create receiver never inherits the registered high-tier limit.
        assertPropagation(helper, new BlockPos(5, 1, 1), highShaft, AllBlocks.SHAFT.get(), createMax + 1f, false);

        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void connectedNetworkUsesLowestTierSuCap(GameTestHelper helper) {
        Tier low = requireTier(helper, LOW_TIER);
        Block lowShaft = requireBlock(helper, "shaft_" + LOW_TIER);
        Block highShaft = requireBlock(helper, "shaft_" + HIGH_TIER);

        KineticBlockEntity lowEntity = placeKinetic(helper, new BlockPos(1, 1, 1), lowShaft);
        KineticBlockEntity highEntity = placeKinetic(helper, new BlockPos(4, 1, 1), highShaft);

        KineticNetwork network = new KineticNetwork();
        network.members.put(lowEntity, 0f);
        network.members.put(highEntity, 0f);
        network.initFromTE(10_000f, 0f, 0);

        assertFloat(helper, low.getMaxSU(), network.calculateCapacity(),
                "Connected network did not clamp capacity to its lowest tier Max SU");

        lowEntity.setSpeed(low.getMaxRPM() + 1f);
        assertFloat(helper, 0f, network.calculateCapacity(),
                "Tier overspeed did not zero the connected network capacity");

        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void generatedServerDataWorksInLiveGame(GameTestHelper helper) {
        Block highShaft = requireBlock(helper, "shaft_" + HIGH_TIER);
        Block gearbox = requireBlock(helper, "gearbox_" + HIGH_TIER);

        if (!highShaft.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            helper.fail("Generated shaft pickaxe tag was not loaded into the live block registry");
        }
        if (!gearbox.defaultBlockState().is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            helper.fail("Generated gearbox pickaxe tag was not loaded into the live block registry");
        }
        if (gearbox.asItem().getDefaultInstance().isEmpty()) {
            helper.fail("Generated gearbox item is not registered");
        }

        BlockPos relative = new BlockPos(2, 1, 2);
        BlockPos absolute = helper.absolutePos(relative);
        helper.getLevel().setBlock(absolute, gearbox.defaultBlockState(), 3);
        helper.getLevel().destroyBlock(absolute, true);
        helper.assertItemEntityPresent(gearbox.asItem(), relative, 2.0);
        helper.succeed();
    }

    private static void assertPropagation(GameTestHelper helper, BlockPos sourceRelative, Block sourceBlock,
            Block targetBlock, float speed, boolean shouldPropagate) {
        BlockPos targetRelative = sourceRelative.east();
        KineticBlockEntity source = placeKinetic(helper, sourceRelative, sourceBlock);
        KineticBlockEntity target = placeKinetic(helper, targetRelative, targetBlock);

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

    private static KineticBlockEntity placeKinetic(GameTestHelper helper, BlockPos relative, Block block) {
        BlockState state = block.defaultBlockState();
        if (state.hasProperty(BlockStateProperties.AXIS)) {
            state = state.setValue(BlockStateProperties.AXIS, Direction.Axis.X);
        }

        BlockPos absolute = helper.absolutePos(relative);
        helper.getLevel().setBlock(absolute, state, 3);
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(absolute);
        if (blockEntity instanceof KineticBlockEntity kinetic) {
            return kinetic;
        }
        helper.fail("Expected kinetic block entity at " + relative + " for " + BuiltInRegistries.BLOCK.getKey(block));
        throw new IllegalStateException("GameTest failure did not abort");
    }

    private static Tier requireTier(GameTestHelper helper, String name) {
        Tier tier = TierRegistry.get(ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, name));
        if (tier != null) {
            return tier;
        }
        helper.fail("Missing GameTest startup tier: " + name);
        throw new IllegalStateException("GameTest failure did not abort");
    }

    private static Block requireBlock(GameTestHelper helper, String path) {
        ResourceLocation id = ResourceLocation.fromNamespaceAndPath(CreateTiers.MOD_ID, path);
        if (BuiltInRegistries.BLOCK.containsKey(id)) {
            return BuiltInRegistries.BLOCK.get(id);
        }
        helper.fail("Missing GameTest block: " + id);
        throw new IllegalStateException("GameTest failure did not abort");
    }

    private static void assertFloat(GameTestHelper helper, float expected, float actual, String message) {
        if (Math.abs(expected - actual) > 0.0001f) {
            helper.fail(message + " (expected " + expected + ", got " + actual + ")");
        }
    }
}
