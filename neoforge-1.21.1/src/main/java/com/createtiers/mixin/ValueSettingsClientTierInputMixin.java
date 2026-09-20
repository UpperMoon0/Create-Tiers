package com.createtiers.mixin;

import com.createtiers.api.ITieredBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.client.TieredSpeedControllerInputScreen;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsClient;
import com.simibubi.create.foundation.blockEntity.behaviour.ValueSettingsPacket;

import net.createmod.catnip.gui.ScreenOpener;
import net.createmod.catnip.platform.CatnipServices;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Replaces Create's width-scaling value board only for tiered speed controllers.
 * Untiered vanilla Create controllers keep the stock interaction unchanged.
 */
@Mixin(value = ValueSettingsClient.class, remap = false)
public abstract class ValueSettingsClientTierInputMixin {

    @Shadow
    private Minecraft mc;

    @Shadow
    public BlockPos interactHeldPos;

    @Redirect(
            method = "tick",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/createmod/catnip/gui/ScreenOpener;open(Lnet/minecraft/client/gui/screens/Screen;)V"))
    private void createtiers$openTieredSpeedInput(Screen original) {
        if (interactHeldPos == null || mc.level == null
                || !(mc.level.getBlockEntity(interactHeldPos) instanceof SpeedControllerBlockEntity controller)
                || !(controller instanceof ITieredBlockEntity tiered)) {
            ScreenOpener.open(original);
            return;
        }

        Tier tier = tiered.getTier();
        if (tier == null) {
            ScreenOpener.open(original);
            return;
        }

        int netId = controller.targetSpeed.netId();
        ScreenOpener.open(new TieredSpeedControllerInputScreen(
                controller.targetSpeed.getValue(),
                tier.getMaxRPM(),
                signedValue -> {
                    int row = signedValue < 0 ? 0 : 1;
                    int value = Math.abs(signedValue);
                    CatnipServices.NETWORK.sendToServer(new ValueSettingsPacket(
                            interactHeldPos, row, value, null, null, Direction.UP, false, netId));
                }));
    }
}