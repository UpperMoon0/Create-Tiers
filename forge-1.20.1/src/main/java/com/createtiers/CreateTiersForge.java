package com.createtiers;

import com.createtiers.api.TierUpgradeRegistry;
import com.createtiers.registry.ModBlocks;
import com.createtiers.registry.ModCreativeTabs;
import com.createtiers.registry.ModRecipes;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(CreateTiers.MOD_ID)
public class CreateTiersForge {

    public CreateTiersForge(FMLJavaModLoadingContext context) {
        Compat.init(ResourceLocation::new);

        CreateTiers.PACK_FORMAT = 15;
        CreateTiers.SERVER_PACK_FORMAT = 10;

        IEventBus modEventBus = context.getModEventBus();

        ModCreativeTabs.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModRecipes.register(modEventBus);
        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        CreateTiers.LOGGER.info("Create Tiers (Forge 1.20.1) initialized");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(TierUpgradeRegistry::validateTargets);
    }
}
