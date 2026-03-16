package com.createtiers.content.kinetics;

import com.createtiers.api.ITieredBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.registry.ModBlocks;
import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TieredShaftBlockEntity extends SimpleKineticBlockEntity implements ITieredBlockEntity {
    
    public TieredShaftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }
    
    public TieredShaftBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.TIERED_SHAFT.get(), pos, state);
    }
    
    @Override
    public Tier getTier() {
        if (getBlockState().getBlock() instanceof TieredShaftBlock shaft) {
            return shaft.getTier();
        }
        return null; // Should not happen for this BE
    }
}
