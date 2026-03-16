package com.createtiers;

import com.createtiers.api.TierRegistry;
import com.createtiers.registry.ModBlocks;
import com.createtiers.registry.ModCreativeTabs;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("createtiers")
public class CreateTiers {

    public static final String MOD_ID = "createtiers";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public CreateTiers(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        // Register creative tabs
        ModCreativeTabs.register(modEventBus);

        // Register blocks and items (via RegisterEvent)
        ModBlocks.register(modEventBus);

        // Register ourselves for server and other game events
        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("Create Tiers initialized with {} tiers", TierRegistry.size());
    }

    public static ResourceLocation asResource(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
