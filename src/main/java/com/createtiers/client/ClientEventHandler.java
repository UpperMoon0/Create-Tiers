package com.createtiers.client;

import com.createtiers.CreateTiers;
import com.createtiers.registry.ModBlocks;
import com.createtiers.content.kinetics.TieredCogwheelBlock;
import com.createtiers.content.kinetics.TieredShaftBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterColorHandlersEvent;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-side event handler for registering dynamic resources and models.
 */
@Mod.EventBusSubscriber(modid = CreateTiers.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientEventHandler {
    
    private static DynamicResourcePack dynamicResourcePack;
    
    /**
     * Register the dynamic resource pack and generate resources.
     * All tiers use Create's original textures - only models/blockstates are generated.
     */
    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            CreateTiers.LOGGER.info("Registering Create-Tiers dynamic resource pack...");
            
            // Clear any existing resources and reset generation flag
            DynamicResourcePack.clear();
            
            // Models and blockstates will be generated lazily by the DynamicResourcePack
            // when they are first requested by Minecraft.
            
            CreateTiers.LOGGER.info("Create-Tiers dynamic models generated successfully!");
            
            // Now register the dynamic resource pack
            dynamicResourcePack = new DynamicResourcePack();
            
            event.addRepositorySource((packConsumer) -> {
                // Define the pack info - must match the internal metadata
                Pack.Info packInfo = new Pack.Info(
                        Component.literal("Dynamic resources for Create Tiers"),
                        15, // pack format for 1.20.1 CLIENT_RESOURCES
                        FeatureFlagSet.of()
                );
                
                Pack pack = Pack.create(
                        "createtiers:dynamic",
                        Component.literal("Create Tiers Dynamic Resources"),
                        true, // REQUIRED - this ensures Minecraft enables it automatically
                        (string) -> dynamicResourcePack,
                        packInfo,
                        PackType.CLIENT_RESOURCES,
                        Pack.Position.TOP, // Put it on top to override any vanilla/mod defaults if needed
                        true, // Fixed position
                        PackSource.BUILT_IN
                );
                
                if (pack != null) {
                    packConsumer.accept(pack);
                }
            });
        }
    }
    
    /**
     * Client setup - no longer needed for resource generation
     * Resources are now generated in onAddPackFinders which runs earlier
     */
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        // Resources are already generated in onAddPackFinders
        // This event is kept for potential future client setup needs
    }

    @SubscribeEvent
    public static void registerBlockColors(RegisterColorHandlersEvent.Block event) {
        CreateTiers.LOGGER.info("Registering tiered block color handlers...");
        
        Block[] shafts = ModBlocks.SHAFTS.toArray(new Block[0]);
        event.register((state, level, pos, tintIndex) -> {
            if (state.getBlock() instanceof TieredShaftBlock shaft) {
                if (tintIndex == 0) return shaft.getTier().getShaftColor();
            }
            return -1;
        }, shafts);

        Block[] cogs = new Block[ModBlocks.COGWHEELS.size() + ModBlocks.LARGE_COGWHEELS.size()];
        int i = 0;
        for (Block b : ModBlocks.COGWHEELS) cogs[i++] = b;
        for (Block b : ModBlocks.LARGE_COGWHEELS) cogs[i++] = b;

        event.register((state, level, pos, tintIndex) -> {
            if (state.getBlock() instanceof TieredCogwheelBlock cog) {
                if (tintIndex == 0) return cog.getTier().getShaftColor();
                if (tintIndex == 1) return cog.getTier().getCogwheelColor();
            }
            return -1;
        }, cogs);
    }

    @SubscribeEvent
    public static void registerItemColors(RegisterColorHandlersEvent.Item event) {
        CreateTiers.LOGGER.info("Registering tiered item color handlers...");
        
        event.register((stack, tintIndex) -> {
            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof TieredShaftBlock shaft) {
                if (tintIndex == 0) return shaft.getTier().getShaftColor();
            }
            return -1;
        }, ModBlocks.SHAFT_ITEMS.toArray(new net.minecraft.world.item.Item[0]));

        net.minecraft.world.item.Item[] cogItems = new net.minecraft.world.item.Item[ModBlocks.COGWHEEL_ITEMS.size() + ModBlocks.LARGE_COGWHEEL_ITEMS.size()];
        int j = 0;
        for (net.minecraft.world.item.Item item : ModBlocks.COGWHEEL_ITEMS) cogItems[j++] = item;
        for (net.minecraft.world.item.Item item : ModBlocks.LARGE_COGWHEEL_ITEMS) cogItems[j++] = item;

        event.register((stack, tintIndex) -> {
            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof TieredCogwheelBlock cog) {
                if (tintIndex == 0) return cog.getTier().getShaftColor();
                if (tintIndex == 1) return cog.getTier().getCogwheelColor();
            }
            return -1;
        }, cogItems);
    }
}
