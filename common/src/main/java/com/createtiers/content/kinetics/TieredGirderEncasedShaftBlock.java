package com.createtiers.content.kinetics;

import com.createtiers.Compat;
import com.createtiers.PlatformHelper;
import com.createtiers.api.Tier;
import com.createtiers.api.TieredNativeKineticBlock;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.girder.GirderEncasedShaftBlock;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.schematics.requirement.ItemRequirement;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class TieredGirderEncasedShaftBlock extends GirderEncasedShaftBlock
        implements TieredNativeKineticBlock {

    private final Tier tier;

    public TieredGirderEncasedShaftBlock(Block.Properties properties, Tier tier) {
        super(properties);
        this.tier = tier;
    }

    @Override
    public Tier getTier() {
        return tier;
    }

    @Override
    public ResourceLocation getBaseBlockId() {
        return Compat.rl("create", "metal_girder_encased_shaft");
    }

    @Override
    public BlockEntityType<?> getExpectedBlockEntityType() {
        return getBlockEntityType();
    }

    @Override
    public InteractionResult onWrenched(BlockState state, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState rotated = getRotatedBlockState(state, context.getClickedFace());
        if (!rotated.canSurvive(level, pos)) {
            return InteractionResult.PASS;
        }

        KineticBlockEntity.switchToBlockState(level, pos, updateAfterWrenched(rotated, context));
        if (level.getBlockState(pos) != state) {
            IWrenchable.playRotateSound(level, pos);
        }

        Player player = context.getPlayer();
        if (player != null && !player.isCreative()) {
            ItemStack shaft = getTieredShaftStack();
            if (!shaft.isEmpty()) {
                player.getInventory().placeItemBackInInventory(shaft);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public ItemRequirement getRequiredItems(BlockState state, BlockEntity blockEntity) {
        BlockState shaft = getTieredShaftState();
        if (shaft == null) {
            return super.getRequiredItems(state, blockEntity);
        }
        return ItemRequirement.of(shaft, blockEntity)
                .union(ItemRequirement.of(AllBlocks.METAL_GIRDER.getDefaultState(), blockEntity));
    }

    private BlockState getTieredShaftState() {
        for (Block block : PlatformHelper.get().getShafts()) {
            if (block instanceof TieredShaftBlock shaft && shaft.getTier().equals(tier)) {
                return shaft.defaultBlockState();
            }
        }
        return null;
    }

    private ItemStack getTieredShaftStack() {
        var blocks = PlatformHelper.get().getShafts();
        var items = PlatformHelper.get().getShaftItems();
        for (int i = 0; i < Math.min(blocks.size(), items.size()); i++) {
            if (blocks.get(i) instanceof TieredShaftBlock shaft && shaft.getTier().equals(tier)) {
                return new ItemStack(items.get(i));
            }
        }
        return ItemStack.EMPTY;
    }
}
