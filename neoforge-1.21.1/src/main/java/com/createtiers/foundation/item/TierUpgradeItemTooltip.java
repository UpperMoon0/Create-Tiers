package com.createtiers.foundation.item;

import com.createtiers.CreateTiers;
import com.createtiers.api.Tier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/** Tooltip for ordinary Create items carrying recipe-produced tier-upgrade data. */
@EventBusSubscriber(modid = CreateTiers.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class TierUpgradeItemTooltip {
    private TierUpgradeItemTooltip() {
    }

    @SubscribeEvent
    public static void onTooltip(ItemTooltipEvent event) {
        Tier tier = TierUpgradeItemData.getTier(event.getItemStack());
        if (tier == null) {
            return;
        }

        event.getToolTip().add(Component.translatable(
                "createtiers.tooltip.upgraded_tier", tier.getDisplayName()).withStyle(ChatFormatting.AQUA));
        event.getToolTip().add(Component.translatable(
                "createtiers.jade.max_rpm", tier.getMaxRPM()).withStyle(ChatFormatting.GRAY));
        event.getToolTip().add(Component.translatable(
                "createtiers.jade.max_su", tier.getMaxSU()).withStyle(ChatFormatting.GRAY));
    }
}
