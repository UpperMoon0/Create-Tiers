package com.createtiers.client;

import com.createtiers.CreateTiers;
import com.createtiers.api.Tier;
import com.createtiers.content.kinetics.TieredCogwheelBlock;
import com.createtiers.content.kinetics.TieredEncasedCogwheelBlock;
import com.createtiers.content.kinetics.TieredEncasedShaftBlock;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.createtiers.registry.ModBlocks;
import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.PackLocationInfo;
import net.minecraft.server.packs.PackSelectionConfig;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.event.AddPackFindersEvent;

import java.util.List;

@EventBusSubscriber(modid = CreateTiers.MOD_ID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class ClientEventHandler {

    private static DynamicResourcePack dynamicResourcePack;

    @SubscribeEvent
    public static void onAddPackFinders(AddPackFindersEvent event) {
        if (event.getPackType() == PackType.CLIENT_RESOURCES) {
            CreateTiers.LOGGER.info("Registering Create-Tiers dynamic resource pack...");

            DynamicResourcePack.clear();

            CreateTiers.LOGGER.info("Create-Tiers dynamic models generated successfully!");

            dynamicResourcePack = new DynamicResourcePack();

            event.addRepositorySource((packConsumer) -> {
                PackLocationInfo packLocation = new PackLocationInfo(
                    "createtiers:dynamic",
                    Component.literal("Create Tiers Dynamic Resources"),
                    PackSource.BUILT_IN,
                    java.util.Optional.empty()
                );
                PackSelectionConfig selectionConfig = new PackSelectionConfig(true, Pack.Position.TOP, true);
                Pack.ResourcesSupplier resourcesSupplier = new Pack.ResourcesSupplier() {
                    @Override
                    public net.minecraft.server.packs.PackResources openPrimary(PackLocationInfo loc) {
                        return dynamicResourcePack;
                    }
                    @Override
                    public net.minecraft.server.packs.PackResources openFull(PackLocationInfo loc, Pack.Metadata meta) {
                        return dynamicResourcePack;
                    }
                };
                Pack pack = Pack.readMetaAndCreate(packLocation, resourcesSupplier, PackType.CLIENT_RESOURCES, selectionConfig);

                if (pack != null) {
                    packConsumer.accept(pack);
                }
            });
        }
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

        Block[] encasedShafts = ModBlocks.ENCASED_SHAFTS.toArray(new Block[0]);
        event.register((state, level, pos, tintIndex) -> {
            if (state.getBlock() instanceof TieredEncasedShaftBlock encased) {
                if (tintIndex == 0) return encased.getTier().getShaftColor();
            }
            return -1;
        }, encasedShafts);

        Block[] encasedCogs = new Block[ModBlocks.ENCASED_COGWHEELS.size() + ModBlocks.ENCASED_LARGE_COGWHEELS.size()];
        int j = 0;
        for (Block b : ModBlocks.ENCASED_COGWHEELS) encasedCogs[j++] = b;
        for (Block b : ModBlocks.ENCASED_LARGE_COGWHEELS) encasedCogs[j++] = b;

        event.register((state, level, pos, tintIndex) -> {
            if (state.getBlock() instanceof TieredEncasedCogwheelBlock encased) {
                if (tintIndex == 0) return encased.getTier().getShaftColor();
                if (tintIndex == 1) return encased.getTier().getCogwheelColor();
            }
            return -1;
        }, encasedCogs);
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
        int k = 0;
        for (net.minecraft.world.item.Item item : ModBlocks.COGWHEEL_ITEMS) cogItems[k++] = item;
        for (net.minecraft.world.item.Item item : ModBlocks.LARGE_COGWHEEL_ITEMS) cogItems[k++] = item;

        event.register((stack, tintIndex) -> {
            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof TieredCogwheelBlock cog) {
                if (tintIndex == 0) return cog.getTier().getShaftColor();
                if (tintIndex == 1) return cog.getTier().getCogwheelColor();
            }
            return -1;
        }, cogItems);

        event.register((stack, tintIndex) -> {
            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof TieredEncasedShaftBlock encased) {
                if (tintIndex == 0) return encased.getTier().getShaftColor();
            }
            return -1;
        }, ModBlocks.ENCASED_SHAFT_ITEMS.toArray(new net.minecraft.world.item.Item[0]));

        net.minecraft.world.item.Item[] encasedCogItems = new net.minecraft.world.item.Item[ModBlocks.ENCASED_COGWHEEL_ITEMS.size() + ModBlocks.ENCASED_LARGE_COGWHEEL_ITEMS.size()];
        int m = 0;
        for (net.minecraft.world.item.Item item : ModBlocks.ENCASED_COGWHEEL_ITEMS) encasedCogItems[m++] = item;
        for (net.minecraft.world.item.Item item : ModBlocks.ENCASED_LARGE_COGWHEEL_ITEMS) encasedCogItems[m++] = item;

        event.register((stack, tintIndex) -> {
            if (stack.getItem() instanceof BlockItem blockItem && blockItem.getBlock() instanceof TieredEncasedCogwheelBlock encased) {
                if (tintIndex == 0) return encased.getTier().getShaftColor();
                if (tintIndex == 1) return encased.getTier().getCogwheelColor();
            }
            return -1;
        }, encasedCogItems);
    }
}
