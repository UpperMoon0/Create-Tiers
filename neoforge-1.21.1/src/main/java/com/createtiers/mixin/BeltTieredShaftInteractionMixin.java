package com.createtiers.mixin;

import com.createtiers.foundation.utility.TieredBeltPulleyInteraction;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Extends Create's exact vanilla-shaft belt interaction to tier-aware shaft items. */
@Mixin(value = BeltBlock.class, remap = false)
public abstract class BeltTieredShaftInteractionMixin {

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void createtiers$addTieredPulley(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit,
            CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (TieredBeltPulleyInteraction.tryAddPulley(stack, state, level, pos, player)) {
            cir.setReturnValue(ItemInteractionResult.SUCCESS);
        }
    }
}
