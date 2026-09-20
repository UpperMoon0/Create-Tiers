package com.createtiers.content.kinetics;

import com.createtiers.PlatformHelper;
import com.createtiers.api.ITieredBlockEntity;
import com.createtiers.api.Tier;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Create powered-shaft implementation that retains the tier of the shaft an engine consumed.
 */
public class TieredPoweredShaftBlockEntity extends PoweredShaftBlockEntity implements ITieredBlockEntity {

    public TieredPoweredShaftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TieredPoweredShaftBlockEntity(BlockPos pos, BlockState state) {
        super(PlatformHelper.get().getTieredPoweredShaftType(), pos, state);
    }

    @Override
    public Tier getTier() {
        Block block = getBlockState().getBlock();
        return block instanceof TieredPoweredShaftBlock powered ? powered.getTier() : null;
    }
}
