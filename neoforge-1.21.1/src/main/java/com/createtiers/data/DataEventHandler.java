package com.createtiers.data;

import com.createtiers.CreateTiers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.AddPackFindersEvent;

@EventBusSubscriber(modid = CreateTiers.MOD_ID, bus = EventBusSubscriber.Bus.MOD)
public class DataEventHandler {

    private static DynamicServerPack dynamicServerPack;

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.SERVER_DATA) {
            CreateTiers.LOGGER.info("Registering Create-Tiers dynamic server data pack...");

            DynamicServerPack.clear();

            dynamicServerPack = new DynamicServerPack();

            event.addRepositorySource((packConsumer) -> {
                PackLocationInfo packLocation = new PackLocationInfo(
                    "createtiers:dynamic_server",
                    Component.literal("Create Tiers Dynamic Data"),
                    PackSource.BUILT_IN,
                    java.util.Optional.empty()
                );
                PackSelectionConfig selectionConfig = new PackSelectionConfig(true, Pack.Position.TOP, true);
                Pack.ResourcesSupplier resourcesSupplier = new Pack.ResourcesSupplier() {
                    @Override
                    public net.minecraft.server.packs.PackResources openPrimary(PackLocationInfo loc) {
                        return dynamicServerPack;
                    }
                    @Override
                    public net.minecraft.server.packs.PackResources openFull(PackLocationInfo loc, Pack.Metadata meta) {
                        return dynamicServerPack;
                    }
                };
                Pack pack = Pack.readMetaAndCreate(packLocation, resourcesSupplier, PackType.SERVER_DATA, selectionConfig);

                if (pack != null) {
                    packConsumer.accept(pack);
                }
            });

            CreateTiers.LOGGER.info("Create-Tiers dynamic server data pack registered!");
        }
    }
}
