package com.createtiers.data;

import com.createtiers.CreateTiers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Common event handler for registering server-side dynamic data packs.
 * This handles tags and loot tables for dynamically registered tiered blocks.
 */
@Mod.EventBusSubscriber(modid = CreateTiers.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class DataEventHandler {
    
    private static DynamicServerPack dynamicServerPack;
    
    /**
     * Register the dynamic server data pack for tags and loot tables.
     * This runs on both client and server (dedicated server needs tags too).
     */
    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.SERVER_DATA) {
            CreateTiers.LOGGER.info("Registering Create-Tiers dynamic server data pack...");
            
            // Clear any existing resources
            DynamicServerPack.clear();
            
            // Note: Resources are generated lazily when requested, because KubeJS
            // scripts may not have run yet when this event fires.
            // DynamicServerPack.generateResources() will be called automatically
            // when the pack is queried and tiers are available.
            
            // Create and register the pack
            dynamicServerPack = new DynamicServerPack();
            
            event.addRepositorySource((packConsumer) -> {
                Pack.Info packInfo = new Pack.Info(
                        Component.literal("Dynamic server data for Create Tiers"),
                        10, // pack format for 1.20.1 SERVER_DATA
                        FeatureFlagSet.of()
                );
                
                Pack pack = Pack.create(
                        "createtiers:dynamic_server",
                        Component.literal("Create Tiers Dynamic Data"),
                        true, // REQUIRED - auto-enable
                        (string) -> dynamicServerPack,
                        packInfo,
                        PackType.SERVER_DATA,
                        Pack.Position.TOP,
                        true, // Fixed position
                        PackSource.BUILT_IN
                );
                
                if (pack != null) {
                    packConsumer.accept(pack);
                }
            });
            
            CreateTiers.LOGGER.info("Create-Tiers dynamic server data pack registered!");
        }
    }
}
