package com.createtiers.content.kinetics;

import com.createtiers.api.Tier;
import com.createtiers.registry.ModBlocks;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
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
        return ModBlocks.TIERED_SHAFT.get();
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() || !player.mayBuild())
            return InteractionResult.PASS;

        IPlacementHelper helper = PlacementHelpers.get(placementHelperId);
        if (helper.matchesItem(stack))
            return helper.getOffset(player, level, state, pos, hitResult)
                    .placeInWorld(level, (BlockItem) stack.getItem(), player, hand, hitResult);

        return super.use(state, level, pos, player, hand, hitResult);
    }

    @MethodsReturnNonnullByDefault
    private static class PlacementHelper extends PoleHelper<Direction.Axis> {
        private PlacementHelper() {
            super(state -> state.getBlock() instanceof TieredShaftBlock, state -> state.getValue(BlockStateProperties.AXIS), BlockStateProperties.AXIS);
        }

        @Override
        public Predicate<ItemStack> getItemPredicate() {
            return i -> i.getItem() instanceof BlockItem
                    && ((BlockItem) i.getItem()).getBlock() instanceof TieredShaftBlock;
        }

        @Override
        public Predicate<BlockState> getStatePredicate() {
            return state -> state.getBlock() instanceof TieredShaftBlock;
        }
    }
}
