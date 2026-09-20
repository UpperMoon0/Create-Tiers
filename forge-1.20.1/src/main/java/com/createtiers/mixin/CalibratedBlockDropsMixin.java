package com.createtiers.mixin;

import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.foundation.item.CalibratedItemData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.annotation.Nullable;
import java.util.List;

/** Preserves attached tier data when an ordinary calibrated Create block is broken. */
@Mixin(Block.class)
public abstract class CalibratedBlockDropsMixin {

    @Inject(
            method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;)Ljava/util/List;",
            at = @At("RETURN"))
    private static void createtiers$preserveCalibrationSimple(BlockState state, ServerLevel level, BlockPos pos,
            @Nullable BlockEntity blockEntity, CallbackInfoReturnable<List<ItemStack>> cir) {
        createtiers$preserve(state, blockEntity, cir.getReturnValue());
    }

    @Inject(
            method = "getDrops(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/item/ItemStack;)Ljava/util/List;",
            at = @At("RETURN"))
    private static void createtiers$preserveCalibrationWithTool(BlockState state, ServerLevel level, BlockPos pos,
            @Nullable BlockEntity blockEntity, @Nullable Entity entity, ItemStack tool,
            CallbackInfoReturnable<List<ItemStack>> cir) {
        createtiers$preserve(state, blockEntity, cir.getReturnValue());
    }

    private static void createtiers$preserve(BlockState state, @Nullable BlockEntity blockEntity, List<ItemStack> drops) {
        if (!(blockEntity instanceof IAttachedTierBlockEntity attachable)) {
            return;
        }
        Tier tier = attachable.getAttachedTier();
        if (tier == null) {
            return;
        }

        for (ItemStack drop : drops) {
            if (drop.getItem() instanceof BlockItem blockItem && blockItem.getBlock() == state.getBlock()) {
                CalibratedItemData.setTier(drop, blockEntity.getType(), tier);
            }
        }
    }
}
