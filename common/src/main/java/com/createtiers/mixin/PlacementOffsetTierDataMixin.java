package com.createtiers.mixin;

import com.createtiers.foundation.utility.TierPlacementTransfer;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Catnip's helper places blocks directly and skips BlockItem's BE-data path.
 * Replay only Create Tiers' registered tier after successful helper placement.
 */
@Mixin(value = PlacementOffset.class, remap = false)
public abstract class PlacementOffsetTierDataMixin {

    @Shadow
    public abstract BlockPos getBlockPos();

    @Inject(
            method = "placeInWorld",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/Block;setPlacedBy(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V",
                    shift = At.Shift.AFTER,
                    remap = true))
    private void createtiers$applyTierAfterHelperPlacement(
            Level level, BlockItem item, Player player, InteractionHand hand, BlockHitResult hit,
            CallbackInfoReturnable<?> cir) {
        TierPlacementTransfer.apply(level, getBlockPos(), player.getItemInHand(hand));
    }
}
