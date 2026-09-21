package com.createtiers.registry;

import com.createtiers.PlatformHelper;
import com.createtiers.CreateTiers;
import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import com.createtiers.api.TierUpgradeRegistry;
import com.createtiers.content.kinetics.TieredCogwheelBlock;
import com.createtiers.content.kinetics.TieredCogwheelBlockEntity;
import com.createtiers.content.kinetics.TieredCogwheelBlockItem;
import com.createtiers.content.kinetics.TieredEncasedCogwheelBlock;
import com.createtiers.content.kinetics.TieredEncasedShaftBlock;
import com.createtiers.content.kinetics.TieredGearboxBlock;
import com.createtiers.content.kinetics.TieredGearboxBlockEntity;
import com.createtiers.content.kinetics.TieredClutchBlock;
import com.createtiers.content.kinetics.TieredGearshiftBlock;
import com.createtiers.content.kinetics.TieredChainDriveBlock;
import com.createtiers.content.kinetics.TieredChainGearshiftBlock;
import com.createtiers.content.kinetics.TieredSpeedControllerBlock;
import com.createtiers.content.kinetics.TieredGirderEncasedShaftBlock;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.createtiers.content.kinetics.TieredShaftBlockEntity;
import com.createtiers.content.kinetics.TieredPoweredShaftBlock;
import com.createtiers.content.kinetics.TieredPoweredShaftBlockEntity;
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
import net.minecraft.world.level.block.SoundType;
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

    public static final List<Block> POWERED_SHAFTS = new ArrayList<>();

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

    public static final List<Block> CLUTCHES = new ArrayList<>();
    public static final List<Item> CLUTCH_ITEMS = new ArrayList<>();
    public static final List<Block> GEARSHIFTS = new ArrayList<>();
    public static final List<Item> GEARSHIFT_ITEMS = new ArrayList<>();
    public static final List<Block> CHAIN_DRIVES = new ArrayList<>();
    public static final List<Item> CHAIN_DRIVE_ITEMS = new ArrayList<>();
    public static final List<Block> CHAIN_GEARSHIFTS = new ArrayList<>();
    public static final List<Item> CHAIN_GEARSHIFT_ITEMS = new ArrayList<>();
    public static final List<Block> SPEED_CONTROLLERS = new ArrayList<>();
    public static final List<Item> SPEED_CONTROLLER_ITEMS = new ArrayList<>();
    public static final List<Block> GIRDER_ENCASED_SHAFTS = new ArrayList<>();

    private static List<CreateEncasingVariants.Variant> SHAFT_ENCASINGS = List.of();
    private static List<CreateEncasingVariants.Variant> COG_ENCASINGS = List.of();
    private static List<CreateEncasingVariants.Variant> LARGE_COG_ENCASINGS = List.of();

    public static RegistryObject<BlockEntityType<TieredShaftBlockEntity>> TIERED_SHAFT;
    public static RegistryObject<BlockEntityType<TieredPoweredShaftBlockEntity>> TIERED_POWERED_SHAFT;
    public static RegistryObject<BlockEntityType<TieredCogwheelBlockEntity>> TIERED_COGWHEEL;
    public static RegistryObject<BlockEntityType<TieredGearboxBlockEntity>> TIERED_GEARBOX;

    public static void register(IEventBus eventBus) {
        PlatformHelper.Holder.INSTANCE = new ModBlocks();

        TIERED_SHAFT = BLOCK_ENTITIES.register("tiered_shaft", () -> {
            List<Block> allShaftBlocks = new ArrayList<>(SHAFTS);
            allShaftBlocks.addAll(ENCASED_SHAFTS);
            return BlockEntityType.Builder.of(TieredShaftBlockEntity::new, allShaftBlocks.toArray(new Block[0])).build(null);
        });

        TIERED_POWERED_SHAFT = BLOCK_ENTITIES.register("tiered_powered_shaft", () ->
                BlockEntityType.Builder.of(TieredPoweredShaftBlockEntity::new,
                        POWERED_SHAFTS.toArray(new Block[0])).build(null));

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
            SHAFT_ENCASINGS = CreateEncasingVariants.shaftVariants();
            COG_ENCASINGS = CreateEncasingVariants.cogwheelVariants();
            LARGE_COG_ENCASINGS = CreateEncasingVariants.largeCogwheelVariants();

            for (Tier tier : TierRegistry.getAllTiers()) {
                SHAFTS.add(new TieredShaftBlock(baseProperties(MapColor.METAL), tier));
                POWERED_SHAFTS.add(new TieredPoweredShaftBlock(baseProperties(MapColor.METAL), tier));
                COGWHEELS.add(new TieredCogwheelBlock(baseProperties(MapColor.METAL), false, tier));
                LARGE_COGWHEELS.add(new TieredCogwheelBlock(baseProperties(MapColor.METAL), true, tier));

                for (CreateEncasingVariants.Variant variant : SHAFT_ENCASINGS) {
                    ENCASED_SHAFTS.add(new TieredEncasedShaftBlock(baseProperties(MapColor.PODZOL),
                            variant::casingBlock, tier));
                }
                for (CreateEncasingVariants.Variant variant : COG_ENCASINGS) {
                    ENCASED_COGWHEELS.add(new TieredEncasedCogwheelBlock(baseProperties(MapColor.PODZOL),
                            false, variant::casingBlock, tier));
                }
                for (CreateEncasingVariants.Variant variant : LARGE_COG_ENCASINGS) {
                    ENCASED_LARGE_COGWHEELS.add(new TieredEncasedCogwheelBlock(baseProperties(MapColor.PODZOL),
                            true, variant::casingBlock, tier));
                }

                GEARBOXES.add(new TieredGearboxBlock(baseProperties(MapColor.PODZOL), tier));
                CLUTCHES.add(new TieredClutchBlock(baseProperties(MapColor.PODZOL), tier));
                GEARSHIFTS.add(new TieredGearshiftBlock(baseProperties(MapColor.PODZOL), tier));
                CHAIN_DRIVES.add(new TieredChainDriveBlock(baseProperties(MapColor.PODZOL), tier));
                CHAIN_GEARSHIFTS.add(new TieredChainGearshiftBlock(baseProperties(MapColor.NETHER), tier));
                SPEED_CONTROLLERS.add(new TieredSpeedControllerBlock(baseProperties(MapColor.TERRACOTTA_YELLOW), tier));
                GIRDER_ENCASED_SHAFTS.add(new TieredGirderEncasedShaftBlock(
                        baseProperties(MapColor.COLOR_GRAY).sound(SoundType.NETHERITE_BLOCK), tier));
            }
        }

        List<Tier> tiers = new ArrayList<>(TierRegistry.getAllTiers());
        for (int i = 0; i < tiers.size(); i++) {
            final int index = i;
            Tier tier = tiers.get(i);
            String suffix = "_" + tier.getName();

            event.register(Registries.BLOCK, CreateTiers.asResource("shaft" + suffix), () -> SHAFTS.get(index));
            event.register(Registries.BLOCK, CreateTiers.asResource("powered_shaft" + suffix), () -> POWERED_SHAFTS.get(index));
            event.register(Registries.BLOCK, CreateTiers.asResource("cogwheel" + suffix), () -> COGWHEELS.get(index));
            event.register(Registries.BLOCK, CreateTiers.asResource("large_cogwheel" + suffix), () -> LARGE_COGWHEELS.get(index));
            event.register(Registries.BLOCK, CreateTiers.asResource("gearbox" + suffix), () -> GEARBOXES.get(index));

            registerEncasedBlocks(event, tier, index, SHAFT_ENCASINGS, ENCASED_SHAFTS);
            registerEncasedBlocks(event, tier, index, COG_ENCASINGS, ENCASED_COGWHEELS);
            registerEncasedBlocks(event, tier, index, LARGE_COG_ENCASINGS, ENCASED_LARGE_COGWHEELS);

            event.register(Registries.BLOCK, CreateTiers.asResource("clutch" + suffix), () -> CLUTCHES.get(index));
            event.register(Registries.BLOCK, CreateTiers.asResource("gearshift" + suffix), () -> GEARSHIFTS.get(index));
            event.register(Registries.BLOCK, CreateTiers.asResource("encased_chain_drive" + suffix), () -> CHAIN_DRIVES.get(index));
            event.register(Registries.BLOCK, CreateTiers.asResource("adjustable_chain_gearshift" + suffix), () -> CHAIN_GEARSHIFTS.get(index));
            event.register(Registries.BLOCK, CreateTiers.asResource("rotation_speed_controller" + suffix), () -> SPEED_CONTROLLERS.get(index));
            event.register(Registries.BLOCK, CreateTiers.asResource("metal_girder_encased_shaft" + suffix),
                    () -> GIRDER_ENCASED_SHAFTS.get(index));
        }

        TierRegistry.freeze();
        TierUpgradeRegistry.freeze();
        registerEncasingVariants();
    }

    private static BlockBehaviour.Properties baseProperties(MapColor mapColor) {
        return BlockBehaviour.Properties.of()
                .mapColor(mapColor)
                .noOcclusion()
                .strength(3.0f, 4.8f)
                .requiresCorrectToolForDrops();
    }

    private static void registerEncasedBlocks(RegisterEvent event, Tier tier, int tierIndex,
            List<CreateEncasingVariants.Variant> variants, List<Block> blocks) {
        for (int variantIndex = 0; variantIndex < variants.size(); variantIndex++) {
            int blockIndex = tierIndex * variants.size() + variantIndex;
            CreateEncasingVariants.Variant variant = variants.get(variantIndex);
            String name = variant.sourcePath() + "_" + tier.getName();
            event.register(Registries.BLOCK, CreateTiers.asResource(name), () -> blocks.get(blockIndex));
        }
    }

    private static void registerItems(RegisterEvent event) {
        List<Tier> tiers = new ArrayList<>(TierRegistry.getAllTiers());
        for (int i = 0; i < tiers.size(); i++) {
            Tier tier = tiers.get(i);
            registerBlockItem(event, "shaft_" + tier.getName(), SHAFTS.get(i), SHAFT_ITEMS, false);
            registerBlockItem(event, "cogwheel_" + tier.getName(), COGWHEELS.get(i), COGWHEEL_ITEMS, true);
            registerBlockItem(event, "large_cogwheel_" + tier.getName(), LARGE_COGWHEELS.get(i), LARGE_COGWHEEL_ITEMS, true);

            registerEncasedItems(event, tier, i, SHAFT_ENCASINGS, ENCASED_SHAFTS, ENCASED_SHAFT_ITEMS);
            registerEncasedItems(event, tier, i, COG_ENCASINGS, ENCASED_COGWHEELS, ENCASED_COGWHEEL_ITEMS);
            registerEncasedItems(event, tier, i, LARGE_COG_ENCASINGS, ENCASED_LARGE_COGWHEELS, ENCASED_LARGE_COGWHEEL_ITEMS);

            Block gearboxBlock = GEARBOXES.get(i);
            Item gearboxItem = new BlockItem(gearboxBlock, new Item.Properties());
            event.register(Registries.ITEM, CreateTiers.asResource("gearbox_" + tier.getName()), () -> gearboxItem);
            GEARBOX_ITEMS.add(gearboxItem);

            Item verticalItem = new TieredVerticalGearboxItem((TieredGearboxBlock) gearboxBlock, new Item.Properties());
            event.register(Registries.ITEM, CreateTiers.asResource("vertical_gearbox_" + tier.getName()), () -> verticalItem);
            GEARBOX_ITEMS.add(verticalItem);

            registerBlockItem(event, "clutch_" + tier.getName(), CLUTCHES.get(i), CLUTCH_ITEMS, false);
            registerBlockItem(event, "gearshift_" + tier.getName(), GEARSHIFTS.get(i), GEARSHIFT_ITEMS, false);
            registerBlockItem(event, "encased_chain_drive_" + tier.getName(), CHAIN_DRIVES.get(i), CHAIN_DRIVE_ITEMS, false);
            registerBlockItem(event, "adjustable_chain_gearshift_" + tier.getName(), CHAIN_GEARSHIFTS.get(i), CHAIN_GEARSHIFT_ITEMS, false);
            registerBlockItem(event, "rotation_speed_controller_" + tier.getName(), SPEED_CONTROLLERS.get(i), SPEED_CONTROLLER_ITEMS, false);
        }
    }

    private static void registerBlockItem(RegisterEvent event, String name, Block block, List<Item> items,
            boolean cogwheelItem) {
        Item item = cogwheelItem && block instanceof TieredCogwheelBlock cog
                ? new TieredCogwheelBlockItem(cog, new Item.Properties())
                : new BlockItem(block, new Item.Properties());
        event.register(Registries.ITEM, CreateTiers.asResource(name), () -> item);
        items.add(item);
    }

    private static void registerEncasedItems(RegisterEvent event, Tier tier, int tierIndex,
            List<CreateEncasingVariants.Variant> variants, List<Block> blocks, List<Item> items) {
        for (int variantIndex = 0; variantIndex < variants.size(); variantIndex++) {
            int blockIndex = tierIndex * variants.size() + variantIndex;
            Block block = blocks.get(blockIndex);
            Item item = new BlockItem(block, new Item.Properties());
            String name = variants.get(variantIndex).sourcePath() + "_" + tier.getName();
            event.register(Registries.ITEM, CreateTiers.asResource(name), () -> item);
            items.add(item);
        }
    }

    private static void registerEncasingVariants() {
        List<Tier> tiers = new ArrayList<>(TierRegistry.getAllTiers());
        for (int tierIndex = 0; tierIndex < tiers.size(); tierIndex++) {
            registerEncasingFamily((TieredShaftBlock) SHAFTS.get(tierIndex), tierIndex,
                    SHAFT_ENCASINGS, ENCASED_SHAFTS);
            registerEncasingFamily((TieredCogwheelBlock) COGWHEELS.get(tierIndex), tierIndex,
                    COG_ENCASINGS, ENCASED_COGWHEELS);
            registerEncasingFamily((TieredCogwheelBlock) LARGE_COGWHEELS.get(tierIndex), tierIndex,
                    LARGE_COG_ENCASINGS, ENCASED_LARGE_COGWHEELS);
        }
    }

    private static void registerEncasingFamily(Block base, int tierIndex,
            List<CreateEncasingVariants.Variant> variants, List<Block> blocks) {
        for (int variantIndex = 0; variantIndex < variants.size(); variantIndex++) {
            Block encased = blocks.get(tierIndex * variants.size() + variantIndex);
            if (base instanceof TieredShaftBlock shaft && encased instanceof TieredEncasedShaftBlock tieredEncased) {
                EncasingRegistry.addVariant(shaft, tieredEncased);
            } else if (base instanceof TieredCogwheelBlock cog
                    && encased instanceof TieredEncasedCogwheelBlock tieredEncasedCog) {
                EncasingRegistry.addVariant(cog, tieredEncasedCog);
            }
        }
    }

    @Override
    public BlockEntityType<?> getTieredShaftType() {
        return TIERED_SHAFT.get();
    }

    @Override
    public BlockEntityType<?> getTieredPoweredShaftType() {
        return TIERED_POWERED_SHAFT.get();
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
    public List<Block> getGirderEncasedShafts() {
        return Collections.unmodifiableList(GIRDER_ENCASED_SHAFTS);
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
    public List<Block> getPoweredShafts() {
        return Collections.unmodifiableList(POWERED_SHAFTS);
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
