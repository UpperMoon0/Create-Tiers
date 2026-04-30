package com.createtiers.content.kinetics;

import com.createtiers.PlatformHelper;
import com.createtiers.api.Tier;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TieredShaftBlock extends ShaftBlock {

    private final Tier tier;

    public TieredShaftBlock(Block.Properties properties, Tier tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TieredShaftBlockEntity(pos, state);
    }

    public Tier getTier() {
        return tier;
    }

    @Override
    public BlockEntityType<? extends TieredShaftBlockEntity> getBlockEntityType() {
        return (BlockEntityType<? extends TieredShaftBlockEntity>) PlatformHelper.get().getTieredShaftType();
    }
}
