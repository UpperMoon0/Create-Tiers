package com.createtiers.content.kinetics;

import com.createtiers.PlatformHelper;
import com.createtiers.api.Tier;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.girder.GirderEncasedShaftBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;

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

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }

    public Tier getTier() {
        return tier;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (player.isShiftKeyDown() || !player.mayBuild()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }

        if (AllBlocks.METAL_GIRDER.isIn(stack) && state.getValue(AXIS) != Direction.Axis.Y) {
            Block girder = PlatformHelper.get().getGirderEncasedShafts().stream()
                    .filter(block -> block instanceof TieredGirderEncasedShaftBlock tiered
                            && tiered.getTier().equals(tier))
                    .findFirst()
                    .orElse(null);
            if (girder instanceof TieredGirderEncasedShaftBlock) {
                KineticBlockEntity.switchToBlockState(level, pos, girder.defaultBlockState()
                        .setValue(BlockStateProperties.WATERLOGGED, state.getValue(BlockStateProperties.WATERLOGGED))
                        .setValue(GirderEncasedShaftBlock.HORIZONTAL_AXIS,
                                state.getValue(AXIS) == Direction.Axis.Z ? Direction.Axis.Z : Direction.Axis.X));
                if (!level.isClientSide && !player.isCreative()) {
                    stack.shrink(1);
                    if (stack.isEmpty()) {
                        player.setItemInHand(hand, ItemStack.EMPTY);
                    }
                }
                return ItemInteractionResult.SUCCESS;
            }
        }

        return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
    }

    @Override
    public BlockEntityType<? extends TieredShaftBlockEntity> getBlockEntityType() {
        return (BlockEntityType<? extends TieredShaftBlockEntity>) PlatformHelper.get().getTieredShaftType();
    }
}
