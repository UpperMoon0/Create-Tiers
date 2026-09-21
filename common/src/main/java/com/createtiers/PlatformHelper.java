package com.createtiers;

import java.util.List;

import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public interface PlatformHelper {

    BlockEntityType<?> getTieredShaftType();

    BlockEntityType<?> getTieredPoweredShaftType();

    BlockEntityType<?> getTieredCogwheelType();

    BlockEntityType<?> getTieredGearboxType();

    List<Block> getShafts();

    List<Item> getShaftItems();

    List<Block> getPoweredShafts();

    List<Block> getCogwheels();

    List<Item> getCogwheelItems();

    List<Block> getLargeCogwheels();

    List<Item> getLargeCogwheelItems();

    List<Block> getGearboxes();

    List<Block> getGirderEncasedShafts();

    List<Item> getGearboxItems();

    static PlatformHelper get() {
        return Holder.INSTANCE;
    }

    class Holder {
        public static PlatformHelper INSTANCE;
    }
}
