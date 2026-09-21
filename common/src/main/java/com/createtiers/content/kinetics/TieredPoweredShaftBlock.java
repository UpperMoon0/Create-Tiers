package com.createtiers.content.kinetics;

import com.createtiers.PlatformHelper;
import com.createtiers.api.Tier;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;

import java.util.List;

/**
 * Powered shaft used internally by Create steam engines when the input shaft is tiered.
 *
 * <p>No item is registered for this block. Every recovery path resolves back to the
 * same tier's ordinary shaft, preventing Create's vanilla shaft from leaking around
 * tier limits.</p>
 */
public class TieredPoweredShaftBlock extends PoweredShaftBlock {

    private final Tier tier;

    public TieredPoweredShaftBlock(Properties properties, Tier tier) {
        super(properties);
        this.tier = tier;
    }

    public Tier getTier() {
        return tier;
    }

    @Override
    public BlockEntityType<? extends TieredPoweredShaftBlockEntity> getBlockEntityType() {
        return (BlockEntityType<? extends TieredPoweredShaftBlockEntity>) PlatformHelper.get()
                .getTieredPoweredShaftType();
    }

    public static TieredPoweredShaftBlock forTier(Tier tier) {
        for (Block block : PlatformHelper.get().getPoweredShafts()) {
            if (block instanceof TieredPoweredShaftBlock powered && powered.getTier().equals(tier)) {
                return powered;
            }
        }
        return null;
    }

    public BlockState getTieredShaftState() {
        for (Block block : PlatformHelper.get().getShafts()) {
            if (block instanceof TieredShaftBlock shaft && shaft.getTier().equals(tier)) {
                return shaft.defaultBlockState();
            }
        }
        return null;
    }

    public ItemStack getTieredShaftStack() {
        List<Block> shafts = PlatformHelper.get().getShafts();
        List<net.minecraft.world.item.Item> items = PlatformHelper.get().getShaftItems();
        for (int i = 0; i < shafts.size() && i < items.size(); i++) {
            Block block = shafts.get(i);
            if (block instanceof TieredShaftBlock shaft && shaft.getTier().equals(tier)) {
                return new ItemStack(items.get(i));
            }
        }
        return ItemStack.EMPTY;
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (PoweredShaftBlock.stillValid(state, level, pos)) {
            return;
        }

        BlockState shaft = getTieredShaftState();
        if (shaft == null) {
            super.tick(state, level, pos, random);
            return;
        }

        level.setBlock(pos, shaft
                .setValue(AXIS, state.getValue(AXIS))
                .setValue(WATERLOGGED, state.getValue(WATERLOGGED)), Block.UPDATE_ALL);
    }

    @Override
    public ItemStack getCloneItemStack(net.minecraft.world.level.BlockGetter level, BlockPos pos, BlockState state) {
        return getTieredShaftStack();
    }

    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        ItemStack stack = getTieredShaftStack();
        return stack.isEmpty() ? List.of() : List.of(stack);
    }
}
