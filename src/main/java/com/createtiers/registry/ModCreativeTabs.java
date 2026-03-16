package com.createtiers.registry;

import com.createtiers.CreateTiers;
import com.simibubi.create.AllBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateTiers.MOD_ID);
    
    public static final RegistryObject<CreativeModeTab> CREATE_TIERS_TAB = CREATIVE_MODE_TABS.register("create_tiers",
            () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.createtiers"))
                    .icon(() -> {
                        if (!ModBlocks.LARGE_COGWHEEL_ITEMS.isEmpty()) {
                            return new ItemStack(ModBlocks.LARGE_COGWHEEL_ITEMS.get(0));
                        }
                        return new ItemStack(AllBlocks.LARGE_COGWHEEL.get());
                    })
                    .displayItems((parameters, output) -> {
                        // Add all dynamically registered shafts to the tab
                        ModBlocks.SHAFT_ITEMS.forEach(output::accept);
                        // Add cogwheels
                        ModBlocks.COGWHEEL_ITEMS.forEach(output::accept);
                        // Add large cogwheels
                        ModBlocks.LARGE_COGWHEEL_ITEMS.forEach(output::accept);
                    })
                    .build());
    
    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
