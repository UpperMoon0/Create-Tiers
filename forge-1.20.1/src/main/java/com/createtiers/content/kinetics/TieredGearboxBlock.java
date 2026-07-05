package com.createtiers.content.kinetics;

import com.createtiers.PlatformHelper;
import com.createtiers.api.Tier;
import com.createtiers.foundation.item.TieredVerticalGearboxItem;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.HitResult;

import java.util.List;

public class TieredGearboxBlock extends RotatedPillarKineticBlock implements IBE<TieredGearboxBlockEntity> {

    private final Tier tier;

    public TieredGearboxBlock(Properties properties, Tier tier) {
        super(properties);
        this.tier = tier;
    }

    public Tier getTier() {
        return tier;
    }

    @Override
    public Class<TieredGearboxBlockEntity> getBlockEntityClass() {
        return TieredGearboxBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TieredGearboxBlockEntity> getBlockEntityType() {
        return (BlockEntityType<? extends TieredGearboxBlockEntity>) PlatformHelper.get().getTieredGearboxType();
    }

    @Override
    public PushReaction getPistonPushReaction(BlockState state) {
        return PushReaction.PUSH_ONLY;
    }

    @SuppressWarnings("deprecation")
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder builder) {
        if (state.getValue(AXIS).isVertical())
            return super.getDrops(state, builder);
        return List.of(getVerticalItem());
    }

    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        if (state.getValue(AXIS).isVertical())
            return super.getCloneItemStack(level, pos, state);
        return getVerticalItem();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(AXIS, Axis.Y);
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face.getAxis() != state.getValue(AXIS);
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(AXIS);
    }

    private ItemStack getVerticalItem() {
        return PlatformHelper.get().getGearboxItems().stream()
                .filter(TieredVerticalGearboxItem.class::isInstance)
                .map(TieredVerticalGearboxItem.class::cast)
                .filter(item -> item.getBlock() == this)
                .findFirst()
                .map(ItemStack::new)
                .orElse(ItemStack.EMPTY);
    }
}
