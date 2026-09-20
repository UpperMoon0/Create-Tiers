package com.createtiers.mixin;

import com.createtiers.content.kinetics.TieredPoweredShaftBlock;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.simibubi.create.content.kinetics.steamEngine.PoweredShaftBlock;
import net.createmod.catnip.placement.PlacementOffset;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Function;
import java.util.function.Predicate;

/** Makes Create's steam-engine placement helper understand tiered shaft items/states. */
@Mixin(targets = "com.simibubi.create.content.kinetics.steamEngine.SteamEngineBlock$PlacementHelper", remap = false)
public abstract class SteamEnginePlacementHelperMixin {

    @Inject(method = "getItemPredicate", at = @At("RETURN"), cancellable = true)
    private void createtiers$acceptTieredShaftItems(CallbackInfoReturnable<Predicate<ItemStack>> cir) {
        Predicate<ItemStack> original = cir.getReturnValue();
        cir.setReturnValue(original.or(stack -> stack.getItem() instanceof BlockItem item
                && item.getBlock() instanceof TieredShaftBlock));
    }

    @Inject(method = "getOffset", at = @At("RETURN"), cancellable = true)
    private void createtiers$preserveTieredPlacement(Player player, Level level, BlockState engineState, BlockPos pos,
            BlockHitResult hit, CallbackInfoReturnable<PlacementOffset> cir) {
        PlacementOffset offset = cir.getReturnValue();
        if (!offset.isSuccessful()) {
            return;
        }

        Function<BlockState, BlockState> original = offset.getTransform();
        cir.setReturnValue(offset.withTransform(held -> {
            BlockState result = original.apply(held);
            if (!(held.getBlock() instanceof TieredShaftBlock shaft)) {
                return result;
            }

            TieredPoweredShaftBlock powered = TieredPoweredShaftBlock.forTier(shaft.getTier());
            if (powered == null || !result.hasProperty(PoweredShaftBlock.AXIS)) {
                return result;
            }

            BlockState replacement = powered.defaultBlockState()
                    .setValue(PoweredShaftBlock.AXIS, result.getValue(PoweredShaftBlock.AXIS));
            if (result.hasProperty(PoweredShaftBlock.WATERLOGGED)) {
                replacement = replacement.setValue(PoweredShaftBlock.WATERLOGGED,
                        result.getValue(PoweredShaftBlock.WATERLOGGED));
            }
            return replacement;
        }));
    }
}
