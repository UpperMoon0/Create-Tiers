package com.createtiers.mixin;

import com.createtiers.PlatformHelper;
import com.createtiers.content.kinetics.TieredGirderEncasedShaftBlock;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.simibubi.create.content.decoration.girder.GirderBlock;
import com.simibubi.create.content.decoration.girder.GirderEncasedShaftBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction.Axis;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = GirderBlock.class, remap = false)
public abstract class GirderBlockTieredShaftMixin {

    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void createtiers$acceptTieredShaft(BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (player == null) return;
        ItemStack stack = player.getItemInHand(hand);
        if (!(stack.getItem() instanceof BlockItem item) || !(item.getBlock() instanceof TieredShaftBlock shaft)) {
            return;
        }

        Block target = PlatformHelper.get().getGirderEncasedShafts().stream()
                .filter(block -> block instanceof TieredGirderEncasedShaftBlock girder
                        && girder.getTier().equals(shaft.getTier()))
                .findFirst().orElse(null);
        if (!(target instanceof TieredGirderEncasedShaftBlock)) return;

        KineticBlockEntity.switchToBlockState(level, pos, target.defaultBlockState()
                .setValue(BlockStateProperties.WATERLOGGED, state.getValue(BlockStateProperties.WATERLOGGED))
                .setValue(GirderBlock.TOP, state.getValue(GirderBlock.TOP))
                .setValue(GirderBlock.BOTTOM, state.getValue(GirderBlock.BOTTOM))
                .setValue(GirderEncasedShaftBlock.HORIZONTAL_AXIS,
                        state.getValue(GirderBlock.X) || hit.getDirection().getAxis() == Axis.Z ? Axis.Z : Axis.X));

        level.playSound(null, pos, SoundEvents.NETHERITE_BLOCK_HIT, SoundSource.BLOCKS, 0.5f, 1.25f);
        if (!level.isClientSide && !player.isCreative()) {
            stack.shrink(1);
            if (stack.isEmpty()) player.setItemInHand(hand, ItemStack.EMPTY);
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }
}
