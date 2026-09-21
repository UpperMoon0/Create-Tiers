package com.createtiers.mixin;

import com.createtiers.foundation.utility.BeltPulleySourcePolicy;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.kinetics.belt.BeltSlicer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps BeltSlicer's shortening refund consistent with the pulley source item.
 * Create hard-codes a vanilla shaft refund after replacing the adjacent pulley.
 */
@Mixin(value = BeltSlicer.class, remap = false)
public abstract class BeltSlicerMixin {

    @Unique
    private static final ThreadLocal<ItemStack> CREATETIERS$SHORTENED_PULLEY_REFUND =
            ThreadLocal.withInitial(() -> ItemStack.EMPTY);

    @Redirect(
            method = "useWrench",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;switchToBlockState(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
                    ordinal = 0))
    private static void createtiers$captureShortenedPulleySource(
            Level level, BlockPos pos, BlockState newState) {
        BlockState oldState = level.getBlockState(pos);
        ItemStack refund = ItemStack.EMPTY;

        if (AllBlocks.BELT.has(oldState)
                && oldState.hasProperty(BeltBlock.PART)
                && oldState.getValue(BeltBlock.PART) == BeltPart.PULLEY) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            refund = BeltPulleySourcePolicy.sourceStack(blockEntity);
        }

        CREATETIERS$SHORTENED_PULLEY_REFUND.set(refund);
        KineticBlockEntity.switchToBlockState(level, pos, newState);
    }

    @ModifyArg(
            method = "useWrench",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Inventory;placeItemBackInInventory(Lnet/minecraft/world/item/ItemStack;)V",
                    ordinal = 0),
            index = 0)
    private static ItemStack createtiers$refundActualPulleySource(ItemStack createRefund) {
        ItemStack source = CREATETIERS$SHORTENED_PULLEY_REFUND.get();
        CREATETIERS$SHORTENED_PULLEY_REFUND.remove();
        return source.isEmpty() ? createRefund : source;
    }

    @Inject(method = "useWrench", at = @At("RETURN"))
    private static void createtiers$clearShorteningRefund(CallbackInfoReturnable<?> cir) {
        CREATETIERS$SHORTENED_PULLEY_REFUND.remove();
    }
}
