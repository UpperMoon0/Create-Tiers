package com.createtiers.content.kinetics;

import com.createtiers.PlatformHelper;
import com.createtiers.api.Tier;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.girder.GirderEncasedShaftBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.kinetics.simpleRelays.AbstractSimpleShaftBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import com.simibubi.create.foundation.placement.PoleHelper;
import net.createmod.catnip.placement.IPlacementHelper;
import net.createmod.catnip.placement.PlacementHelpers;
import net.minecraft.MethodsReturnNonnullByDefault;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import java.util.function.Predicate;

public class TieredShaftBlock extends ShaftBlock {

    public static final int placementHelperId = PlacementHelpers.register(new PlacementHelper());

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

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() == state.getValue(AXIS);
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() || !player.mayBuild())
            return InteractionResult.PASS;

        if (AllBlocks.METAL_GIRDER.isIn(stack) && state.getValue(AXIS) != Direction.Axis.Y) {
            Block girder = PlatformHelper.get().getGirderEncasedShafts().stream()
                    .filter(block -> block instanceof TieredGirderEncasedShaftBlock tiered
                            && tiered.getTier().equals(tier))
                    .findFirst()
                    .orElse(null);
            if (girder instanceof TieredGirderEncasedShaftBlock) {
                KineticBlockEntity.switchToBlockState(level, pos, girder.defaultBlockState()
                        .setValue(GirderEncasedShaftBlock.HORIZONTAL_AXIS,
                                state.getValue(AXIS) == Direction.Axis.Z ? Direction.Axis.Z : Direction.Axis.X));
                if (!level.isClientSide && !player.isCreative()) {
                    stack.shrink(1);
                }
                return InteractionResult.SUCCESS;
            }
        }

        IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
        if (helper.matchesItem(stack) && helper.matchesState(state))
            return helper.getOffset(player, level, state, pos, hitResult)
                    .placeInWorld(level, (BlockItem) stack.getItem(), player, hand, hitResult);

        return super.use(state, level, pos, player, hand, hitResult);
    }

    @MethodsReturnNonnullByDefault
    private static class PlacementHelper extends PoleHelper<Direction.Axis> {
        private PlacementHelper() {
            super(state -> state.getBlock() instanceof AbstractSimpleShaftBlock, state -> state.getValue(BlockStateProperties.AXIS), BlockStateProperties.AXIS);
        }

        @Override
        public Predicate<ItemStack> getItemPredicate() {
            return i -> i.getItem() instanceof BlockItem
                    && ((BlockItem) i.getItem()).getBlock() instanceof TieredShaftBlock;
        }

        @Override
        public Predicate<BlockState> getStatePredicate() {
            return state -> state.getBlock() instanceof AbstractSimpleShaftBlock;
        }
    }
}
