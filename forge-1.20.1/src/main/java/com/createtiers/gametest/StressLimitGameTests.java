package com.createtiers.gametest;

import com.createtiers.CreateTiers;
import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateTiers.MOD_ID)
@PrefixGameTestTemplate(false)
public final class StressLimitGameTests {
    private StressLimitGameTests() {
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 20)
    public static void connectedNetworkUsesLowestTierSuCap(GameTestHelper helper) {
        KineticBlockEntity lowEntity = GameTestSupport.placeTieredKinetic(
                helper, new BlockPos(1, 1, 1), GameTestSupport.LOW_TIER);
        KineticBlockEntity highEntity = GameTestSupport.placeTieredKinetic(
                helper, new BlockPos(4, 1, 1), GameTestSupport.HIGH_TIER);

        KineticNetwork network = GameTestSupport.network(10_000f, lowEntity, highEntity);
        GameTestSupport.assertFloat(helper, GameTestSupport.LOW_TIER.getMaxSU(), network.calculateCapacity(),
                "Connected network did not clamp capacity to its lowest tier Max SU");
        helper.succeed();
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 20)
    public static void tierOverspeedZeroesConnectedNetworkCapacity(GameTestHelper helper) {
        KineticBlockEntity lowEntity = GameTestSupport.placeTieredKinetic(
                helper, new BlockPos(1, 1, 1), GameTestSupport.LOW_TIER);
        lowEntity.setSpeed(GameTestSupport.LOW_TIER.getMaxRPM() + 1f);

        KineticNetwork network = GameTestSupport.network(10_000f, lowEntity);
        GameTestSupport.assertFloat(helper, 0f, network.calculateCapacity(),
                "Tier overspeed did not zero the connected network capacity");
        helper.succeed();
    }
}
