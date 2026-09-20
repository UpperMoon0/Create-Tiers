package com.createtiers.gametest;

import com.createtiers.CreateTiers;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateTiers.MOD_ID)
@PrefixGameTestTemplate(false)
public final class RpmNetworkGameTests {
    private RpmNetworkGameTests() {
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 20)
    public static void tieredToTieredPropagation(GameTestHelper helper) {
        int createMax = AllConfigs.server().kinetics.maxRotationSpeed.get();
        if (GameTestSupport.HIGH_TIER.getMaxRPM() <= createMax) {
            helper.fail("High test tier no longer exceeds Create's configured RPM limit");
        }
        GameTestSupport.assertPropagation(helper, new BlockPos(1, 1, 1),
                GameTestSupport.HIGH_TIER, GameTestSupport.HIGH_TIER, createMax + 1f, true);
        helper.succeed();
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 20)
    public static void highTierToOrdinaryCreateReceiverRejectsAboveCreateLimit(GameTestHelper helper) {
        int createMax = AllConfigs.server().kinetics.maxRotationSpeed.get();
        GameTestSupport.assertPropagation(helper, new BlockPos(1, 1, 1),
                GameTestSupport.HIGH_TIER, null, createMax + 1f, false);
        helper.succeed();
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 20)
    public static void tieredReceiverRejectsOverspeedAndDestroysSource(GameTestHelper helper) {
        GameTestSupport.assertPropagation(helper, new BlockPos(1, 1, 1),
                GameTestSupport.HIGH_TIER, GameTestSupport.LOW_TIER,
                GameTestSupport.LOW_TIER.getMaxRPM() + 1f, false);
        helper.succeed();
    }
}
