package com.createtiers.registry;

import com.createtiers.CreateTiers;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {

    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, CreateTiers.MOD_ID);

    public static final RegistryObject<CreativeModeTab> CREATE_TIERS_TAB = CREATIVE_MODE_TABS.register("create_tiers",
            () -> com.createtiers.registry.CommonCreativeTab.createTabBuilder().build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}