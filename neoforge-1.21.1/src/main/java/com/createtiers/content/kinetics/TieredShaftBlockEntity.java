package com.createtiers.content.kinetics;

import com.createtiers.PlatformHelper;
import com.createtiers.api.ITieredBlockEntity;
import com.createtiers.api.Tier;
import com.simibubi.create.content.kinetics.simpleRelays.SimpleKineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TieredShaftBlockEntity extends SimpleKineticBlockEntity implements ITieredBlockEntity {

    public TieredShaftBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TieredShaftBlockEntity(BlockPos pos, BlockState state) {
        super(PlatformHelper.get().getTieredShaftType(), pos, state);
    }

    @Override
    public Tier getTier() {
        Block block = getBlockState().getBlock();
        if (block instanceof TieredShaftBlock shaft) {
            return shaft.getTier();
        }
        if (block instanceof TieredEncasedShaftBlock encasedShaft) {
            return encasedShaft.getTier();
        }
        return null;
    }
}
