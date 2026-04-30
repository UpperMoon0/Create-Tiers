package com.createtiers.content.kinetics;

import com.createtiers.api.Tier;
import com.createtiers.registry.ModBlocks;
import com.simibubi.create.content.kinetics.simpleRelays.CogWheelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TieredCogwheelBlock extends CogWheelBlock {
    
    private final Tier tier;
    private final boolean large;
    
    public TieredCogwheelBlock(Block.Properties properties, boolean large, Tier tier) {
        super(large, properties);
        this.tier = tier;
        this.large = large;
    }
    
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TieredCogwheelBlockEntity(pos, state);
    }
    
    public Tier getTier() {
        return tier;
    }
    
    public boolean isLargeCogwheel() {
        return large;
    }

    @Override
    public BlockEntityType<? extends TieredCogwheelBlockEntity> getBlockEntityType() {
        return ModBlocks.TIERED_COGWHEEL.get();
    }

    @Override
    public boolean isLargeCog() {
        return large;
    }

    @Override
    public boolean isSmallCog() {
        return !large;
    }

    @Override
    public boolean isDedicatedCogWheel() {
        return true;
    }
}
