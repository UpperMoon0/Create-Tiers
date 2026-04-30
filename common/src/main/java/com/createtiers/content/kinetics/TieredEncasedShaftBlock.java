package com.createtiers.content.kinetics;

import java.util.function.Supplier;

import com.createtiers.PlatformHelper;
import com.createtiers.api.Tier;
import com.simibubi.create.api.schematic.requirement.SpecialBlockItemRequirement;
import com.simibubi.create.content.decoration.encasing.EncasedBlock;
import com.simibubi.create.content.kinetics.base.AbstractEncasedShaftBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.RotatedPillarKineticBlock;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class TieredEncasedShaftBlock extends AbstractEncasedShaftBlock
        implements IBE<TieredShaftBlockEntity>, SpecialBlockItemRequirement, EncasedBlock {

    private final Supplier<Block> casing;
    private final Tier tier;

    public TieredEncasedShaftBlock(Properties properties, Supplier<Block> casing, Tier tier) {
        super(properties);
        this.casing = casing;
        this.tier = tier;
    }

    @Override
    public InteractionResult onSneakWrenched(BlockState state, UseOnContext context) {
        if (context.getLevel().isClientSide)
            return InteractionResult.SUCCESS;
        context.getLevel()
                .levelEvent(2001, context.getClickedPos(), Block.getId(state));
        KineticBlockEntity.switchToBlockState(context.getLevel(), context.getClickedPos(),
                getDefaultShaftState()
                        .setValue(AXIS, state.getValue(AXIS)));
        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemStack getCloneItemStack(BlockState state, HitResult target, BlockGetter world, BlockPos pos, Player player) {
        if (target instanceof BlockHitResult)
            return ((BlockHitResult) target).getDirection()
                    .getAxis() == getRotationAxis(state) ? getDefaultShaftStack() : getCasing().asItem().getDefaultInstance();
        return super.getCloneItemStack(state, target, world, pos, player);
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, BlockEntity be) {
        return ItemRequirement.of(getDefaultShaftState(), be);
    }

    @Override
    public Class<TieredShaftBlockEntity> getBlockEntityClass() {
        return TieredShaftBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends TieredShaftBlockEntity> getBlockEntityType() {
        return (BlockEntityType<? extends TieredShaftBlockEntity>) PlatformHelper.get().getTieredShaftType();
    }

    @Override
    public Block getCasing() {
        return casing.get();
    }

    @Override
    public void handleEncasing(BlockState state, Level level, BlockPos pos, ItemStack heldItem, Player player, InteractionHand hand,
                               BlockHitResult ray) {
        KineticBlockEntity.switchToBlockState(level, pos, defaultBlockState()
                .setValue(RotatedPillarKineticBlock.AXIS, state.getValue(RotatedPillarKineticBlock.AXIS)));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TieredShaftBlockEntity(pos, state);
    }

    public Tier getTier() {
        return tier;
    }

    private BlockState getDefaultShaftState() {
        for (Block block : PlatformHelper.get().getShafts()) {
            if (block instanceof TieredShaftBlock shaft && shaft.getTier().equals(tier)) {
                return shaft.defaultBlockState();
            }
        }
        return defaultBlockState();
    }

    private ItemStack getDefaultShaftStack() {
        java.util.List<Block> shafts = PlatformHelper.get().getShafts();
        java.util.List<Item> shaftItems = PlatformHelper.get().getShaftItems();
        for (int i = 0; i < shafts.size(); i++) {
            Block block = shafts.get(i);
            if (block instanceof TieredShaftBlock shaft && shaft.getTier().equals(tier)) {
                return new ItemStack(shaftItems.get(i));
            }
        }
        return ItemStack.EMPTY;
    }
}
