package com.createtiers.foundation.utility;

import com.createtiers.api.IReplacementSourceBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.api.TierRegistry;
import com.createtiers.api.TierUpgradeRegistry;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;

public final class AttachedTierAuthorization {
    private AttachedTierAuthorization() {}

    public static boolean isRegisteredForBlock(Block block, Tier tier) {
        if (block == null || tier == null) return false;
        ResourceLocation tierId = TierRegistry.getId(tier);
        if (tierId == null) return false;
        Item item = block.asItem();
        if (item == Items.AIR) return false;
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
        return itemId != null && TierUpgradeRegistry.isRegistered(itemId, tierId);
    }

    public static boolean canCarry(KineticBlockEntity kinetic, Tier tier) {
        if (isRegisteredForBlock(kinetic.getBlockState().getBlock(), tier)) return true;
        if (!(kinetic instanceof IReplacementSourceBlockEntity source)) return false;
        ResourceLocation sourceId = source.getCreateTiersReplacementSourceBlockId();
        if (sourceId == null) return false;
        Block sourceBlock = BuiltInRegistries.BLOCK.get(sourceId);
        if (!sourceId.equals(BuiltInRegistries.BLOCK.getKey(sourceBlock))) return false;
        return isRegisteredForBlock(sourceBlock, tier);
    }
}
