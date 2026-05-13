package com.createtiers.content.kinetics;

import com.createtiers.PlatformHelper;
import com.createtiers.api.ITieredBlockEntity;
import com.createtiers.api.Tier;
import com.simibubi.create.content.kinetics.base.DirectionalShaftHalvesBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TieredGearboxBlockEntity extends DirectionalShaftHalvesBlockEntity implements ITieredBlockEntity {

    public TieredGearboxBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public TieredGearboxBlockEntity(BlockPos pos, BlockState state) {
        super(PlatformHelper.get().getTieredGearboxType(), pos, state);
    }

    @Override
    protected boolean isNoisy() {
        return false;
    }

    @Override
    public Tier getTier() {
        Block block = getBlockState().getBlock();
        if (block instanceof TieredGearboxBlock gearbox) {
            return gearbox.getTier();
        }
        return null;
    }
}