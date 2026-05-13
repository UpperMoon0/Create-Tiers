package com.createtiers.registry;

import com.simibubi.create.AllBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class CommonCreativeTab {

    public static CreativeModeTab.Builder createTabBuilder() {
        return CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.createtiers"))
                .icon(() -> {
                    if (!ModBlocks.LARGE_COGWHEEL_ITEMS.isEmpty()) {
                        return new ItemStack(ModBlocks.LARGE_COGWHEEL_ITEMS.get(0));
                    }
                    return new ItemStack(AllBlocks.LARGE_COGWHEEL.get());
                })
                .displayItems((parameters, output) -> {
                    ModBlocks.SHAFT_ITEMS.forEach(output::accept);
                    ModBlocks.COGWHEEL_ITEMS.forEach(output::accept);
                    ModBlocks.LARGE_COGWHEEL_ITEMS.forEach(output::accept);
                    ModBlocks.GEARBOX_ITEMS.forEach(output::accept);
                });
    }
}