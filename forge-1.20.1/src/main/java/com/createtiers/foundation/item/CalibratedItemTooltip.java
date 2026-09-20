package com.createtiers.foundation.item;

import com.createtiers.CreateTiers;
import com.createtiers.api.Tier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Tooltip for ordinary Create items carrying recipe-produced calibration data. */
@Mod.EventBusSubscriber(modid = CreateTiers.MOD_ID, value = Dist.CLIENT)
public final class CalibratedItemTooltip {
    private CalibratedItemTooltip() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Tier tier = CalibratedItemData.getTier(event.getItemStack());
        if (tier == null) {
            return;
        }

        event.getToolTip().add(Component.translatable(
                "createtiers.tooltip.calibrated_tier", tier.getDisplayName()).withStyle(ChatFormatting.AQUA));
        event.getToolTip().add(Component.translatable(
                "createtiers.jade.max_rpm", tier.getMaxRPM()).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.translatable(
                "createtiers.jade.max_su", tier.getMaxSU()).withStyle(ChatFormatting.GRAY));
    }
}
