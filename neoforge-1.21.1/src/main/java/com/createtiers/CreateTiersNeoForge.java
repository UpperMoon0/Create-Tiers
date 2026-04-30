package com.createtiers;

import com.createtiers.registry.ModBlocks;
import com.createtiers.registry.ModCreativeTabs;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod(CreateTiers.MOD_ID)
public class CreateTiersNeoForge {

    public CreateTiersNeoForge(IEventBus modEventBus) {
        Compat.init(ResourceLocation::fromNamespaceAndPath);

        CreateTiers.PACK_FORMAT = 34;
        CreateTiers.SERVER_PACK_FORMAT = 78;

        ModCreativeTabs.register(modEventBus);
        ModBlocks.register(modEventBus);

        CreateTiers.LOGGER.info("Create Tiers (NeoForge 1.21.1) initialized");
    }
}
