package com.createtiers.registry;

import com.createtiers.CreateTiers;
import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import com.createtiers.content.kinetics.TieredCogwheelBlock;
import com.createtiers.content.kinetics.TieredCogwheelBlockEntity;
import com.createtiers.content.kinetics.TieredCogwheelBlockItem;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.createtiers.content.kinetics.TieredShaftBlockEntity;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegisterEvent;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.List;

public class ModBlocks {
    
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CreateTiers.MOD_ID);
    
    // We will populate these during RegisterEvent
    public static final List<Block> SHAFTS = new ArrayList<>();
    public static final List<Item> SHAFT_ITEMS = new ArrayList<>();
    
    public static final List<Block> COGWHEELS = new ArrayList<>();
    public static final List<Item> COGWHEEL_ITEMS = new ArrayList<>();
    
    public static final List<Block> LARGE_COGWHEELS = new ArrayList<>();
    public static final List<Item> LARGE_COGWHEEL_ITEMS = new ArrayList<>();
    
    public static RegistryObject<BlockEntityType<TieredShaftBlockEntity>> TIERED_SHAFT;
    public static RegistryObject<BlockEntityType<TieredCogwheelBlockEntity>> TIERED_COGWHEEL;
    
    public static void register(IEventBus eventBus) {
        // Register block entity type - we need to do this in a way that includes all dynamic blocks
        // We use a supplier for the blocks array
        TIERED_SHAFT = BLOCK_ENTITIES.register("tiered_shaft", () -> {
            return BlockEntityType.Builder.of(TieredShaftBlockEntity::new, SHAFTS.toArray(new Block[0])).build(null);
        });
        
        TIERED_COGWHEEL = BLOCK_ENTITIES.register("tiered_cogwheel", () -> {
            List<Block> allCogwheels = new ArrayList<>(COGWHEELS);
            allCogwheels.addAll(LARGE_COGWHEELS);
            return BlockEntityType.Builder.of(TieredCogwheelBlockEntity::new, allCogwheels.toArray(new Block[0])).build(null);
        });
        
        BLOCK_ENTITIES.register(eventBus);
        
        // Subscribe to the RegisterEvent for blocks and items
        eventBus.addListener(ModBlocks::onRegister);
    }
    
    private static void onRegister(RegisterEvent event) {
        if (event.getRegistryKey().equals(Registries.BLOCK)) {
            registerBlocks(event);
        } else if (event.getRegistryKey().equals(Registries.ITEM)) {
            registerItems(event);
        }
    }
    
    private static void registerBlocks(RegisterEvent event) {
        if (SHAFTS.isEmpty()) {
            for (Tier tier : TierRegistry.getAllTiers()) {
                // Register Shaft
                Block shaftBlock = new TieredShaftBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .noOcclusion()
                        .strength(3.0f, 4.8f)
                        .requiresCorrectToolForDrops(), tier);

                SHAFTS.add(shaftBlock);

                // Register Cogwheel
                Block cogwheelBlock = new TieredCogwheelBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .noOcclusion()
                        .strength(3.0f, 4.8f)
                        .requiresCorrectToolForDrops(), false, tier);

                COGWHEELS.add(cogwheelBlock);

                // Register Large Cogwheel
                Block largeCogwheelBlock = new TieredCogwheelBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .noOcclusion()
                        .strength(3.0f, 4.8f)
                        .requiresCorrectToolForDrops(), true, tier);

                LARGE_COGWHEELS.add(largeCogwheelBlock);
            }
        }

        List<Tier> tiers = new ArrayList<>(TierRegistry.getAllTiers());
        for (int i = 0; i < SHAFTS.size(); i++) {
            final int index = i;
            Tier tier = tiers.get(i);
            event.register(Registries.BLOCK, CreateTiers.asResource("shaft_" + tier.getName()), () -> SHAFTS.get(index));
            event.register(Registries.BLOCK, CreateTiers.asResource("cogwheel_" + tier.getName()), () -> COGWHEELS.get(index));
            event.register(Registries.BLOCK, CreateTiers.asResource("large_cogwheel_" + tier.getName()), () -> LARGE_COGWHEELS.get(index));
        }

        // Freeze registry after blocks are registered
        TierRegistry.freeze();
    }
    
    private static void registerItems(RegisterEvent event) {
        for (Tier tier : TierRegistry.getAllTiers()) {
            // Shaft Items
            String shaftName = "shaft_" + tier.getName();
            Block shaftBlock = SHAFTS.stream()
                    .filter(b -> b instanceof TieredShaftBlock && ((TieredShaftBlock)b).getTier().equals(tier))
                    .findFirst()
                    .orElse(null);
            
            if (shaftBlock != null) {
                Item item = new BlockItem(shaftBlock, new Item.Properties());
                event.register(Registries.ITEM, CreateTiers.asResource(shaftName), () -> item);
                SHAFT_ITEMS.add(item);
            }
            
            // Cogwheel Items
            String cogwheelName = "cogwheel_" + tier.getName();
            Block cogwheelBlock = COGWHEELS.stream()
                    .filter(b -> b instanceof TieredCogwheelBlock && !((TieredCogwheelBlock)b).isLargeCogwheel() && ((TieredCogwheelBlock)b).getTier().equals(tier))
                    .findFirst()
                    .orElse(null);
            
            if (cogwheelBlock != null) {
                Item item = new TieredCogwheelBlockItem((TieredCogwheelBlock) cogwheelBlock, new Item.Properties());
                event.register(Registries.ITEM, CreateTiers.asResource(cogwheelName), () -> item);
                COGWHEEL_ITEMS.add(item);
            }
            
            // Large Cogwheel Items
            String largeCogwheelName = "large_cogwheel_" + tier.getName();
            Block largeCogwheelBlock = LARGE_COGWHEELS.stream()
                    .filter(b -> b instanceof TieredCogwheelBlock && ((TieredCogwheelBlock)b).isLargeCogwheel() && ((TieredCogwheelBlock)b).getTier().equals(tier))
                    .findFirst()
                    .orElse(null);
            
            if (largeCogwheelBlock != null) {
                Item item = new TieredCogwheelBlockItem((TieredCogwheelBlock) largeCogwheelBlock, new Item.Properties());
                event.register(Registries.ITEM, CreateTiers.asResource(largeCogwheelName), () -> item);
                LARGE_COGWHEEL_ITEMS.add(item);
            }
        }
    }
}
