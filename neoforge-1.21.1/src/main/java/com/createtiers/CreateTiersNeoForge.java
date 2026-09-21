package com.createtiers;

import com.createtiers.api.TierUpgradeRegistry;
import com.createtiers.registry.ModBlocks;
import com.createtiers.registry.ModCreativeTabs;
import com.createtiers.registry.ModRecipes;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod(CreateTiers.MOD_ID)
public class CreateTiersNeoForge {

    public CreateTiersNeoForge(IEventBus modEventBus) {
        Compat.init(ResourceLocation::fromNamespaceAndPath);

        // Minecraft 1.21/1.21.1 resource-pack and data-pack formats.
        CreateTiers.PACK_FORMAT = 34;
        CreateTiers.SERVER_PACK_FORMAT = 48;

        ModCreativeTabs.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModRecipes.register(modEventBus);
        modEventBus.addListener(this::commonSetup);

        CreateTiers.LOGGER.info("Create Tiers (NeoForge 1.21.1) initialized");
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(TierUpgradeRegistry::validateTargets);
    }
}
