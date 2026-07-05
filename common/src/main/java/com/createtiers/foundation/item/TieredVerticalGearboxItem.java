package com.createtiers.foundation.item;

import com.createtiers.content.kinetics.TieredGearboxBlock;
import com.simibubi.create.content.kinetics.base.IRotate;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

public class TieredVerticalGearboxItem extends BlockItem {

    public TieredVerticalGearboxItem(TieredGearboxBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public void registerBlocks(java.util.Map<Block, Item> blockToItemMap, Item item) {
        // Keep the regular tiered gearbox as the block's canonical item.
    }

    @Override
    protected boolean updateCustomBlockEntityTag(BlockPos pos, Level level, Player player, ItemStack stack,
            BlockState state) {
        Axis preferredAxis = null;
        for (Direction side : Iterate.horizontalDirections) {
            BlockState adjacent = level.getBlockState(pos.relative(side));
            if (adjacent.getBlock() instanceof IRotate rotating
                    && rotating.hasShaftTowards(level, pos.relative(side), adjacent, side.getOpposite())) {
                if (preferredAxis != null && preferredAxis != side.getAxis()) {
                    preferredAxis = null;
                    break;
                }
                preferredAxis = side.getAxis();
            }
        }

        Axis axis = preferredAxis == null
                ? player.getDirection().getClockWise().getAxis()
                : preferredAxis == Axis.X ? Axis.Z : Axis.X;
        level.setBlockAndUpdate(pos, state.setValue(BlockStateProperties.AXIS, axis));
        return super.updateCustomBlockEntityTag(pos, level, player, stack, state);
    }
}
