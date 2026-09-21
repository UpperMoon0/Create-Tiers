package com.createtiers.mixin;

import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.IReplacementSourceBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.foundation.utility.AttachedTierTransfer;
import com.createtiers.foundation.utility.BeltPulleySourcePolicy;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.kinetics.belt.BeltSlicer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Keeps BeltSlicer's shortening ownership consistent with the shaft sources.
 * The old endpoint source moves inward; only an overwritten adjacent pulley
 * source is refunded.
 */
@Mixin(value = BeltSlicer.class, remap = false)
public abstract class BeltSlicerMixin {

    @Unique
    private record PulleySource(Tier attachedTier, ResourceLocation blockId) {
    }

    @Unique
    private static final ThreadLocal<PulleySource> CREATETIERS$SHORTENED_ENDPOINT_SOURCE = new ThreadLocal<>();

    @Unique
    private static final ThreadLocal<ItemStack> CREATETIERS$SHORTENED_PULLEY_REFUND =
            ThreadLocal.withInitial(() -> ItemStack.EMPTY);

    @Inject(method = "useWrench", at = @At("HEAD"))
    private static void createtiers$captureEndpointSource(
            BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit, BeltSlicer.Feedback feedback,
            CallbackInfoReturnable<?> cir) {
        CREATETIERS$SHORTENED_ENDPOINT_SOURCE.set(createtiers$captureSource(level.getBlockEntity(pos)));
    }

    @Redirect(
            method = "useWrench",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/simibubi/create/content/kinetics/base/KineticBlockEntity;switchToBlockState(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V",
                    ordinal = 0))
    private static void createtiers$moveEndpointSourceAndCapturePulleyRefund(
            Level level, BlockPos pos, BlockState newState) {
        BlockState oldState = level.getBlockState(pos);
        ItemStack refund = ItemStack.EMPTY;

        if (AllBlocks.BELT.has(oldState)
                && oldState.hasProperty(BeltBlock.PART)
                && oldState.getValue(BeltBlock.PART) == BeltPart.PULLEY) {
            refund = BeltPulleySourcePolicy.sourceStack(level.getBlockEntity(pos));
        }

        CREATETIERS$SHORTENED_PULLEY_REFUND.set(refund);
        KineticBlockEntity.switchToBlockState(level, pos, newState);
        createtiers$replaceSource(level, pos, CREATETIERS$SHORTENED_ENDPOINT_SOURCE.get());
    }

    @ModifyArg(
            method = "useWrench",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/player/Inventory;placeItemBackInInventory(Lnet/minecraft/world/item/ItemStack;)V",
                    ordinal = 0),
            index = 0)
    private static ItemStack createtiers$refundOverwrittenPulleySource(ItemStack createRefund) {
        ItemStack source = CREATETIERS$SHORTENED_PULLEY_REFUND.get();
        CREATETIERS$SHORTENED_PULLEY_REFUND.remove();
        return source.isEmpty() ? createRefund : source;
    }

    @Inject(method = "useWrench", at = @At("RETURN"))
    private static void createtiers$clearShorteningState(CallbackInfoReturnable<?> cir) {
        CREATETIERS$SHORTENED_ENDPOINT_SOURCE.remove();
        CREATETIERS$SHORTENED_PULLEY_REFUND.remove();
    }

    @Unique
    private static PulleySource createtiers$captureSource(BlockEntity blockEntity) {
        if (!(blockEntity instanceof IAttachedTierBlockEntity attachable)) {
            return new PulleySource(null, null);
        }
        Tier tier = attachable.getAttachedTier();
        ResourceLocation sourceId = blockEntity instanceof IReplacementSourceBlockEntity source
                ? source.getCreateTiersReplacementSourceBlockId()
                : null;
        return new PulleySource(tier, sourceId);
    }

    @Unique
    private static void createtiers$replaceSource(Level level, BlockPos pos, PulleySource source) {
        if (level.isClientSide) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof IAttachedTierBlockEntity attachable) {
            attachable.clearAttachedTier();
        }
        if (blockEntity instanceof IReplacementSourceBlockEntity provenance) {
            provenance.clearCreateTiersReplacementSourceBlockId();
        }

        if (source != null) {
            AttachedTierTransfer.restore(level, pos, source.attachedTier(), source.blockId());
        }
    }
}
