package com.createtiers.foundation.item;

import java.util.List;

import com.createtiers.api.Tier;
import com.createtiers.content.kinetics.TieredCogwheelBlock;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.createtiers.foundation.utility.ModLang;
import com.simibubi.create.foundation.item.TooltipModifier;
import com.simibubi.create.foundation.utility.CreateLang;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;

public class TieredKineticStats implements TooltipModifier {

    @Override
    public void modify(ItemTooltipEvent context) {
        if (!(context.getItemStack().getItem() instanceof BlockItem blockItem))
            return;

        Block block = blockItem.getBlock();
        Tier tier = null;
        if (block instanceof TieredShaftBlock shaft) {
            tier = shaft.getTier();
        } else if (block instanceof TieredCogwheelBlock cog) {
            tier = cog.getTier();
        }

        if (tier == null)
            return;

        List<Component> tooltip = context.getToolTip();
        
        // Add Max RPM
        ModLang.translate("tooltip.tiered_max_rpm")
                .style(ChatFormatting.GRAY)
                .addTo(tooltip);
        ModLang.builder()
                .add(ModLang.number(tier.getMaxRPM()))
                .text(" ")
                .add(CreateLang.translate("generic.unit.rpm"))
                .style(ChatFormatting.AQUA)
                .addTo(tooltip);

        // Add Max SU
        ModLang.translate("tooltip.tiered_max_su")
                .style(ChatFormatting.GRAY)
                .addTo(tooltip);
        ModLang.builder()
                .add(ModLang.number(tier.getMaxSU()))
                .text(" ")
                .add(CreateLang.translate("generic.unit.stress"))
                .style(ChatFormatting.AQUA)
                .addTo(tooltip);
    }
}
