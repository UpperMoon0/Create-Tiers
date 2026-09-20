package com.createtiers.gametest;

import com.createtiers.CreateTiers;
import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.Tier;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.motor.CreativeMotorBlockEntity;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(CreateTiers.MOD_ID)
@PrefixGameTestTemplate(false)
public final class AdjustableComponentGameTests {
    private AdjustableComponentGameTests() {
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 20)
    public static void rotationSpeedControllerUsesTierRangeAndRestoresCreateRange(GameTestHelper helper) {
        Tier tier = GameTestSupport.ensureAttachmentTier();
        int createMax = AllConfigs.server().kinetics.maxRotationSpeed.get();

        SpeedControllerBlockEntity controller = GameTestSupport.placeBlockEntity(
                helper, new BlockPos(1, 1, 1),
                AllBlocks.ROTATION_SPEED_CONTROLLER.get().defaultBlockState(),
                SpeedControllerBlockEntity.class);
        IAttachedTierBlockEntity controllerTier = GameTestSupport.requireAttachable(helper, controller);
        controllerTier.setAttachedTier(tier);
        controller.targetSpeed.setValue(tier.getMaxRPM());
        if (controller.targetSpeed.getValue() != tier.getMaxRPM()) {
            helper.fail("Calibrated Rotation Speed Controller remained capped below tier Max RPM");
        }

        controllerTier.clearAttachedTier();
        if (Math.abs(controller.targetSpeed.getValue()) > createMax) {
            helper.fail("Cleared Rotation Speed Controller did not restore Create's configured RPM range");
        }
        helper.succeed();
    }

    @GameTest(template = GameTestSupport.TEMPLATE, timeoutTicks = 20)
    public static void creativeMotorUsesTierRangeAndRestoresVanillaRange(GameTestHelper helper) {
        Tier tier = GameTestSupport.ensureAttachmentTier();

        CreativeMotorBlockEntity motor = GameTestSupport.placeBlockEntity(
                helper, new BlockPos(1, 1, 1),
                AllBlocks.CREATIVE_MOTOR.get().defaultBlockState(),
                CreativeMotorBlockEntity.class);
        IAttachedTierBlockEntity motorTier = GameTestSupport.requireAttachable(helper, motor);
        motorTier.setAttachedTier(tier);
        motor.generatedSpeed.setValue(tier.getMaxRPM());
        if (motor.generatedSpeed.getValue() != tier.getMaxRPM()) {
            helper.fail("Calibrated Creative Motor remained capped at Create's vanilla range");
        }

        motorTier.clearAttachedTier();
        if (Math.abs(motor.generatedSpeed.getValue()) > CreativeMotorBlockEntity.MAX_SPEED) {
            helper.fail("Cleared Creative Motor did not restore its vanilla RPM range");
        }
        helper.succeed();
    }
}
