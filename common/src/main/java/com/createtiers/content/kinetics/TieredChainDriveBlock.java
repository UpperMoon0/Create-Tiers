package com.createtiers.content.kinetics;

import com.createtiers.Compat;
import com.createtiers.api.Tier;
import com.createtiers.api.TieredNativeKineticBlock;
import com.simibubi.create.content.kinetics.chainDrive.ChainDriveBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class TieredChainDriveBlock extends ChainDriveBlock implements TieredNativeKineticBlock {
    private final Tier tier;

    public TieredChainDriveBlock(Block.Properties properties, Tier tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public Tier getTier() {
        return tier;
    }

    @Override
    public ResourceLocation getBaseBlockId() {
        return Compat.rl("create", "encased_chain_drive");
    }

    @Override
    public BlockEntityType<?> getExpectedBlockEntityType() {
        return getBlockEntityType();
    }
}
