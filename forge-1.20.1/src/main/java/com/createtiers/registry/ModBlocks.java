package com.createtiers.registry;

import com.createtiers.PlatformHelper;
import com.createtiers.CreateTiers;
import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import com.createtiers.content.kinetics.TieredCogwheelBlock;
import com.createtiers.content.kinetics.TieredCogwheelBlockEntity;
import com.createtiers.content.kinetics.TieredCogwheelBlockItem;
import com.createtiers.content.kinetics.TieredEncasedCogwheelBlock;
import com.createtiers.content.kinetics.TieredEncasedShaftBlock;
import com.createtiers.content.kinetics.TieredGearboxBlock;
import com.createtiers.content.kinetics.TieredGearboxBlockEntity;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.createtiers.content.kinetics.TieredShaftBlockEntity;
import com.createtiers.foundation.item.TieredVerticalGearboxItem;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.encasing.EncasingRegistry;
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
import java.util.Collections;
import java.util.List;

public class ModBlocks implements PlatformHelper {

    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, CreateTiers.MOD_ID);

    public static final List<Block> SHAFTS = new ArrayList<>();
    public static final List<Item> SHAFT_ITEMS = new ArrayList<>();

    public static final List<Block> COGWHEELS = new ArrayList<>();
    public static final List<Item> COGWHEEL_ITEMS = new ArrayList<>();

    public static final List<Block> LARGE_COGWHEELS = new ArrayList<>();
    public static final List<Item> LARGE_COGWHEEL_ITEMS = new ArrayList<>();

    public static final List<Block> ENCASED_SHAFTS = new ArrayList<>();
    public static final List<Item> ENCASED_SHAFT_ITEMS = new ArrayList<>();

    public static final List<Block> ENCASED_COGWHEELS = new ArrayList<>();
    public static final List<Item> ENCASED_COGWHEEL_ITEMS = new ArrayList<>();

    public static final List<Block> ENCASED_LARGE_COGWHEELS = new ArrayList<>();
    public static final List<Item> ENCASED_LARGE_COGWHEEL_ITEMS = new ArrayList<>();

    public static final List<Block> GEARBOXES = new ArrayList<>();
    public static final List<Item> GEARBOX_ITEMS = new ArrayList<>();

    public static RegistryObject<BlockEntityType<TieredShaftBlockEntity>> TIERED_SHAFT;
    public static RegistryObject<BlockEntityType<TieredCogwheelBlockEntity>> TIERED_COGWHEEL;
    public static RegistryObject<BlockEntityType<TieredGearboxBlockEntity>> TIERED_GEARBOX;

    public static void register(IEventBus eventBus) {
        PlatformHelper.Holder.INSTANCE = new ModBlocks();

        TIERED_SHAFT = BLOCK_ENTITIES.register("tiered_shaft", () -> {
            List<Block> allShaftBlocks = new ArrayList<>(SHAFTS);
            allShaftBlocks.addAll(ENCASED_SHAFTS);
            return BlockEntityType.Builder.of(TieredShaftBlockEntity::new, allShaftBlocks.toArray(new Block[0])).build(null);
        });

        TIERED_COGWHEEL = BLOCK_ENTITIES.register("tiered_cogwheel", () -> {
            List<Block> allCogwheelBlocks = new ArrayList<>(COGWHEELS);
            allCogwheelBlocks.addAll(LARGE_COGWHEELS);
            allCogwheelBlocks.addAll(ENCASED_COGWHEELS);
            allCogwheelBlocks.addAll(ENCASED_LARGE_COGWHEELS);
            return BlockEntityType.Builder.of(TieredCogwheelBlockEntity::new, allCogwheelBlocks.toArray(new Block[0])).build(null);
        });

        TIERED_GEARBOX = BLOCK_ENTITIES.register("tiered_gearbox", () -> {
            return BlockEntityType.Builder.of(TieredGearboxBlockEntity::new, GEARBOXES.toArray(new Block[0])).build(null);
        });

        BLOCK_ENTITIES.register(eventBus);

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
                Block shaftBlock = new TieredShaftBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .noOcclusion()
                        .strength(3.0f, 4.8f)
                        .requiresCorrectToolForDrops(), tier);

                SHAFTS.add(shaftBlock);

                Block cogwheelBlock = new TieredCogwheelBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .noOcclusion()
                        .strength(3.0f, 4.8f)
                        .requiresCorrectToolForDrops(), false, tier);

                COGWHEELS.add(cogwheelBlock);

                Block largeCogwheelBlock = new TieredCogwheelBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.METAL)
                        .noOcclusion()
                        .strength(3.0f, 4.8f)
                        .requiresCorrectToolForDrops(), true, tier);

                LARGE_COGWHEELS.add(largeCogwheelBlock);

                Block andesiteEncasedShaft = new TieredEncasedShaftBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.PODZOL)
                        .noOcclusion()
                        .strength(3.0f, 4.8f)
                        .requiresCorrectToolForDrops(), AllBlocks.ANDESITE_CASING::get, tier);
                ENCASED_SHAFTS.add(andesiteEncasedShaft);

                Block brassEncasedShaft = new TieredEncasedShaftBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.TERRACOTTA_YELLOW)
                        .noOcclusion()
                        .strength(3.0f, 4.8f)
                        .requiresCorrectToolForDrops(), AllBlocks.BRASS_CASING::get, tier);
                ENCASED_SHAFTS.add(brassEncasedShaft);

                Block andesiteEncasedCogwheel = new TieredEncasedCogwheelBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.PODZOL)
                        .noOcclusion()
                        .strength(3.0f, 4.8f)
                        .requiresCorrectToolForDrops(), false, AllBlocks.ANDESITE_CASING::get, tier);
                ENCASED_COGWHEELS.add(andesiteEncasedCogwheel);

                Block brassEncasedCogwheel = new TieredEncasedCogwheelBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.TERRACOTTA_YELLOW)
                        .noOcclusion()
                        .strength(3.0f, 4.8f)
                        .requiresCorrectToolForDrops(), false, AllBlocks.BRASS_CASING::get, tier);
                ENCASED_COGWHEELS.add(brassEncasedCogwheel);

                Block andesiteEncasedLargeCogwheel = new TieredEncasedCogwheelBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.PODZOL)
                        .noOcclusion()
                        .strength(3.0f, 4.8f)
                        .requiresCorrectToolForDrops(), true, AllBlocks.ANDESITE_CASING::get, tier);
                ENCASED_LARGE_COGWHEELS.add(andesiteEncasedLargeCogwheel);

                Block brassEncasedLargeCogwheel = new TieredEncasedCogwheelBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.TERRACOTTA_YELLOW)
                        .noOcclusion()
                        .strength(3.0f, 4.8f)
                        .requiresCorrectToolForDrops(), true, AllBlocks.BRASS_CASING::get, tier);
                ENCASED_LARGE_COGWHEELS.add(brassEncasedLargeCogwheel);

                Block gearboxBlock = new TieredGearboxBlock(BlockBehaviour.Properties.of()
                        .mapColor(MapColor.PODZOL)
                        .noOcclusion()
                        .strength(3.0f, 4.8f)
                        .requiresCorrectToolForDrops(), tier);
                GEARBOXES.add(gearboxBlock);
            }
        }

        List<Tier> tiers = new ArrayList<>(TierRegistry.getAllTiers());
        for (int i = 0; i < SHAFTS.size(); i++) {
            final int index = i;
            Tier tier = tiers.get(i);
            event.register(Registries.BLOCK, CreateTiers.asResource("shaft_" + tier.getName()), () -> SHAFTS.get(index));
            event.register(Registries.BLOCK, CreateTiers.asResource("cogwheel_" + tier.getName()), () -> COGWHEELS.get(index));
            event.register(Registries.BLOCK, CreateTiers.asResource("large_cogwheel_" + tier.getName()), () -> LARGE_COGWHEELS.get(index));

            event.register(Registries.BLOCK, CreateTiers.asResource("andesite_encased_shaft_" + tier.getName()), () -> ENCASED_SHAFTS.get(index * 2));
            event.register(Registries.BLOCK, CreateTiers.asResource("brass_encased_shaft_" + tier.getName()), () -> ENCASED_SHAFTS.get(index * 2 + 1));

            event.register(Registries.BLOCK, CreateTiers.asResource("andesite_encased_cogwheel_" + tier.getName()), () -> ENCASED_COGWHEELS.get(index * 2));
            event.register(Registries.BLOCK, CreateTiers.asResource("brass_encased_cogwheel_" + tier.getName()), () -> ENCASED_COGWHEELS.get(index * 2 + 1));

            event.register(Registries.BLOCK, CreateTiers.asResource("andesite_encased_large_cogwheel_" + tier.getName()), () -> ENCASED_LARGE_COGWHEELS.get(index * 2));
            event.register(Registries.BLOCK, CreateTiers.asResource("brass_encased_large_cogwheel_" + tier.getName()), () -> ENCASED_LARGE_COGWHEELS.get(index * 2 + 1));

            event.register(Registries.BLOCK, CreateTiers.asResource("gearbox_" + tier.getName()), () -> GEARBOXES.get(index));
        }

        TierRegistry.freeze();

        registerEncasingVariants();
    }

    private static void registerItems(RegisterEvent event) {
        for (Tier tier : TierRegistry.getAllTiers()) {
            String shaftName = "shaft_" + tier.getName();
            Block shaftBlock = SHAFTS.stream()
                    .filter(b -> b instanceof TieredShaftBlock && ((TieredShaftBlock) b).getTier().equals(tier))
                    .findFirst()
                    .orElse(null);

            if (shaftBlock != null) {
                Item item = new BlockItem(shaftBlock, new Item.Properties());
                event.register(Registries.ITEM, CreateTiers.asResource(shaftName), () -> item);
                SHAFT_ITEMS.add(item);
            }

            String cogwheelName = "cogwheel_" + tier.getName();
            Block cogwheelBlock = COGWHEELS.stream()
                    .filter(b -> b instanceof TieredCogwheelBlock && !((TieredCogwheelBlock) b).isLargeCogwheel() && ((TieredCogwheelBlock) b).getTier().equals(tier))
                    .findFirst()
                    .orElse(null);

            if (cogwheelBlock != null) {
                Item item = new TieredCogwheelBlockItem((TieredCogwheelBlock) cogwheelBlock, new Item.Properties());
                event.register(Registries.ITEM, CreateTiers.asResource(cogwheelName), () -> item);
                COGWHEEL_ITEMS.add(item);
            }

            String largeCogwheelName = "large_cogwheel_" + tier.getName();
            Block largeCogwheelBlock = LARGE_COGWHEELS.stream()
                    .filter(b -> b instanceof TieredCogwheelBlock && ((TieredCogwheelBlock) b).isLargeCogwheel() && ((TieredCogwheelBlock) b).getTier().equals(tier))
                    .findFirst()
                    .orElse(null);

            if (largeCogwheelBlock != null) {
                Item item = new TieredCogwheelBlockItem((TieredCogwheelBlock) largeCogwheelBlock, new Item.Properties());
                event.register(Registries.ITEM, CreateTiers.asResource(largeCogwheelName), () -> item);
                LARGE_COGWHEEL_ITEMS.add(item);
            }

            registerEncasedShaftItem(event, tier, "andesite_encased_shaft_" + tier.getName(), ENCASED_SHAFTS, ENCASED_SHAFT_ITEMS, 0);
            registerEncasedShaftItem(event, tier, "brass_encased_shaft_" + tier.getName(), ENCASED_SHAFTS, ENCASED_SHAFT_ITEMS, 1);

            registerEncasedCogwheelItem(event, tier, "andesite_encased_cogwheel_" + tier.getName(), ENCASED_COGWHEELS, ENCASED_COGWHEEL_ITEMS, 0);
            registerEncasedCogwheelItem(event, tier, "brass_encased_cogwheel_" + tier.getName(), ENCASED_COGWHEELS, ENCASED_COGWHEEL_ITEMS, 1);

            registerEncasedCogwheelItem(event, tier, "andesite_encased_large_cogwheel_" + tier.getName(), ENCASED_LARGE_COGWHEELS, ENCASED_LARGE_COGWHEEL_ITEMS, 0);
            registerEncasedCogwheelItem(event, tier, "brass_encased_large_cogwheel_" + tier.getName(), ENCASED_LARGE_COGWHEELS, ENCASED_LARGE_COGWHEEL_ITEMS, 1);

            Block gearboxBlock = GEARBOXES.stream()
                    .filter(b -> b instanceof TieredGearboxBlock && ((TieredGearboxBlock) b).getTier().equals(tier))
                    .findFirst()
                    .orElse(null);
            if (gearboxBlock != null) {
                Item item = new BlockItem(gearboxBlock, new Item.Properties());
                event.register(Registries.ITEM, CreateTiers.asResource("gearbox_" + tier.getName()), () -> item);
                GEARBOX_ITEMS.add(item);

                Item verticalItem = new TieredVerticalGearboxItem((TieredGearboxBlock) gearboxBlock,
                        new Item.Properties());
                event.register(Registries.ITEM, CreateTiers.asResource("vertical_gearbox_" + tier.getName()),
                        () -> verticalItem);
                GEARBOX_ITEMS.add(verticalItem);
            }
        }
    }

    private static void registerEncasedShaftItem(RegisterEvent event, Tier tier, String name, List<Block> blockList, List<Item> itemList, int offset) {
        int tierIndex = new ArrayList<>(TierRegistry.getAllTiers()).indexOf(tier);
        int idx = tierIndex * 2 + offset;
        if (idx < blockList.size()) {
            Block block = blockList.get(idx);
            if (block instanceof TieredEncasedShaftBlock && ((TieredEncasedShaftBlock) block).getTier().equals(tier)) {
                Item item = new BlockItem(block, new Item.Properties());
                event.register(Registries.ITEM, CreateTiers.asResource(name), () -> item);
                itemList.add(item);
            }
        }
    }

    private static void registerEncasedCogwheelItem(RegisterEvent event, Tier tier, String name, List<Block> blockList, List<Item> itemList, int offset) {
        int tierIndex = new ArrayList<>(TierRegistry.getAllTiers()).indexOf(tier);
        int idx = tierIndex * 2 + offset;
        if (idx < blockList.size()) {
            Block block = blockList.get(idx);
            if (block instanceof TieredEncasedCogwheelBlock && ((TieredEncasedCogwheelBlock) block).getTier().equals(tier)) {
                Item item = new BlockItem(block, new Item.Properties());
                event.register(Registries.ITEM, CreateTiers.asResource(name), () -> item);
                itemList.add(item);
            }
        }
    }

    private static void registerEncasingVariants() {
        List<Tier> tiers = new ArrayList<>(TierRegistry.getAllTiers());
        for (int i = 0; i < tiers.size(); i++) {
            Tier tier = tiers.get(i);
            TieredShaftBlock shaftBlock = (TieredShaftBlock) SHAFTS.get(i);
            TieredCogwheelBlock cogwheelBlock = (TieredCogwheelBlock) COGWHEELS.get(i);
            TieredCogwheelBlock largeCogwheelBlock = (TieredCogwheelBlock) LARGE_COGWHEELS.get(i);

            TieredEncasedShaftBlock andesiteEncasedShaft = (TieredEncasedShaftBlock) ENCASED_SHAFTS.get(i * 2);
            TieredEncasedShaftBlock brassEncasedShaft = (TieredEncasedShaftBlock) ENCASED_SHAFTS.get(i * 2 + 1);

            TieredEncasedCogwheelBlock andesiteEncasedCogwheel = (TieredEncasedCogwheelBlock) ENCASED_COGWHEELS.get(i * 2);
            TieredEncasedCogwheelBlock brassEncasedCogwheel = (TieredEncasedCogwheelBlock) ENCASED_COGWHEELS.get(i * 2 + 1);

            TieredEncasedCogwheelBlock andesiteEncasedLargeCogwheel = (TieredEncasedCogwheelBlock) ENCASED_LARGE_COGWHEELS.get(i * 2);
            TieredEncasedCogwheelBlock brassEncasedLargeCogwheel = (TieredEncasedCogwheelBlock) ENCASED_LARGE_COGWHEELS.get(i * 2 + 1);

            EncasingRegistry.addVariant(shaftBlock, andesiteEncasedShaft);
            EncasingRegistry.addVariant(shaftBlock, brassEncasedShaft);

            EncasingRegistry.addVariant(cogwheelBlock, andesiteEncasedCogwheel);
            EncasingRegistry.addVariant(cogwheelBlock, brassEncasedCogwheel);

            EncasingRegistry.addVariant(largeCogwheelBlock, andesiteEncasedLargeCogwheel);
            EncasingRegistry.addVariant(largeCogwheelBlock, brassEncasedLargeCogwheel);
        }
    }

    @Override
    public BlockEntityType<?> getTieredShaftType() {
        return TIERED_SHAFT.get();
    }

    @Override
    public BlockEntityType<?> getTieredCogwheelType() {
        return TIERED_COGWHEEL.get();
    }

    @Override
    public BlockEntityType<?> getTieredGearboxType() {
        return TIERED_GEARBOX.get();
    }

    @Override
    public List<Block> getGearboxes() {
        return Collections.unmodifiableList(GEARBOXES);
    }

    @Override
    public List<Item> getGearboxItems() {
        return Collections.unmodifiableList(GEARBOX_ITEMS);
    }

    @Override
    public List<Block> getShafts() {
        return Collections.unmodifiableList(SHAFTS);
    }

    @Override
    public List<Item> getShaftItems() {
        return Collections.unmodifiableList(SHAFT_ITEMS);
    }

    @Override
    public List<Block> getCogwheels() {
        return Collections.unmodifiableList(COGWHEELS);
    }

    @Override
    public List<Item> getCogwheelItems() {
        return Collections.unmodifiableList(COGWHEEL_ITEMS);
    }

    @Override
    public List<Block> getLargeCogwheels() {
        return Collections.unmodifiableList(LARGE_COGWHEELS);
    }

    @Override
    public List<Item> getLargeCogwheelItems() {
        return Collections.unmodifiableList(LARGE_COGWHEEL_ITEMS);
    }
}
