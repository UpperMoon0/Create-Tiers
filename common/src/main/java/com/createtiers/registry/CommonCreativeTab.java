package com.createtiers.registry;

import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import com.createtiers.api.TierUpgradeRegistry;
import com.createtiers.foundation.item.CalibratedItemData;
import com.simibubi.create.AllBlocks;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

public class CommonCreativeTab {

    public static CreativeModeTab.Builder createTabBuilder() {
        return CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.createtiers"))
                .icon(() -> {
                    if (!ModBlocks.LARGE_COGWHEEL_ITEMS.isEmpty()) {
                        return new ItemStack(ModBlocks.LARGE_COGWHEEL_ITEMS.get(0));
                    }
                    return new ItemStack(AllBlocks.LARGE_COGWHEEL.get());
                })
                .displayItems((parameters, output) -> {
                    ModBlocks.SHAFT_ITEMS.forEach(output::accept);
                    ModBlocks.COGWHEEL_ITEMS.forEach(output::accept);
                    ModBlocks.LARGE_COGWHEEL_ITEMS.forEach(output::accept);
                    ModBlocks.GEARBOX_ITEMS.forEach(output::accept);
                    ModBlocks.CLUTCH_ITEMS.forEach(output::accept);
                    ModBlocks.GEARSHIFT_ITEMS.forEach(output::accept);
                    ModBlocks.CHAIN_DRIVE_ITEMS.forEach(output::accept);
                    ModBlocks.CHAIN_GEARSHIFT_ITEMS.forEach(output::accept);
                    ModBlocks.SPEED_CONTROLLER_ITEMS.forEach(output::accept);
                    tierUpgradeEntries().forEach(output::accept);
                });
    }

    /**
     * Materialize every registered tier-upgrade pair as the same calibrated item
     * stack used by recipe outputs. Registry insertion order is retained so the tab
     * is deterministic and pack authors can control grouping through registration.
     */
    public static List<ItemStack> tierUpgradeEntries() {
        List<ItemStack> entries = new ArrayList<>();
        for (TierUpgradeRegistry.Registration registration : TierUpgradeRegistry.getAll()) {
            Item item = BuiltInRegistries.ITEM.get(registration.itemId());
            if (!registration.itemId().equals(BuiltInRegistries.ITEM.getKey(item))) {
                throw new IllegalStateException(
                        "Creative tab tier upgrade references unknown item '" + registration.itemId() + "'");
            }

            Tier tier = TierRegistry.get(registration.tierId());
            if (tier == null) {
                throw new IllegalStateException(
                        "Creative tab tier upgrade references unknown tier '" + registration.tierId() + "'");
            }

            entries.add(CalibratedItemData.calibratedCopy(new ItemStack(item), tier));
        }
        return List.copyOf(entries);
    }
}
