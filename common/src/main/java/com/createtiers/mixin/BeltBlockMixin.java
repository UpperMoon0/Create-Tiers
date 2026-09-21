package com.createtiers.mixin;

import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.IReplacementSourceBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.createtiers.foundation.utility.AttachedTierTransfer;
import com.createtiers.foundation.utility.BeltPulleySourcePolicy;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.kinetics.simpleRelays.ShaftBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Restores the original tiered pulley shaft when a Create belt chain is removed. */
@Mixin(value = BeltBlock.class, remap = false)
public abstract class BeltBlockMixin {

    @Unique
    private record PulleySource(Tier tier, ResourceLocation blockId) {
    }

    @Unique
    private static final ThreadLocal<Map<BlockPos, PulleySource>> CREATETIERS$REMOVED_PULLEY_SOURCES =
            ThreadLocal.withInitial(HashMap::new);

    @Inject(method = "onWrenched", at = @At("HEAD"), cancellable = true)
    private void createtiers$removeTieredPulley(BlockState state, UseOnContext context,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (state.getValue(BeltBlock.CASING) || state.getValue(BeltBlock.PART) != BeltPart.PULLEY) {
            return;
        }

        Level level = context.getLevel();
        if (level.isClientSide) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            return;
        }

        BlockPos pos = context.getClickedPos();
        ItemStack returnedShaft = BeltPulleySourcePolicy.sourceStack(level.getBlockEntity(pos));
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof IAttachedTierBlockEntity attachable
                && attachable.getAttachedTier() != null) {
            attachable.clearAttachedTier();
        }
        if (blockEntity instanceof IReplacementSourceBlockEntity provenance
                && provenance.getCreateTiersReplacementSourceBlockId() != null) {
            provenance.clearCreateTiersReplacementSourceBlockId();
        }

        com.simibubi.create.content.kinetics.base.KineticBlockEntity.switchToBlockState(
                level, pos, state.setValue(BeltBlock.PART, BeltPart.MIDDLE));

        Player player = context.getPlayer();
        if (player != null && !player.isCreative()) {
            player.getInventory().placeItemBackInInventory(returnedShaft);
        }
        cir.setReturnValue(InteractionResult.SUCCESS);
    }

    @Inject(method = "getDrops", at = @At("RETURN"), cancellable = true)
    private void createtiers$replacePulleyShaftDrop(
            BlockState state, LootParams.Builder builder, CallbackInfoReturnable<List<ItemStack>> cir) {
        if (!state.hasProperty(BeltBlock.PART)
                || state.getValue(BeltBlock.PART) != BeltPart.PULLEY) {
            return;
        }

        BlockEntity blockEntity = builder.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (!(blockEntity instanceof IReplacementSourceBlockEntity provenance)
                || provenance.getCreateTiersReplacementSourceBlockId() == null) {
            return;
        }

        ItemStack sourceStack = BeltPulleySourcePolicy.sourceStack(blockEntity);
        List<ItemStack> drops = new ArrayList<>(cir.getReturnValue());
        for (int i = drops.size() - 1; i >= 0; i--) {
            if (drops.get(i).is(AllBlocks.SHAFT.get().asItem())) {
                drops.set(i, sourceStack);
                cir.setReturnValue(drops);
                return;
            }
        }
    }

    @Redirect(
            method = "onRemove",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;removeBlockEntity(Lnet/minecraft/core/BlockPos;)V"))
    private void createtiers$captureTierBeforeBeltEntityRemoval(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity instanceof IAttachedTierBlockEntity tiered) {
            ResourceLocation sourceBlockId = blockEntity instanceof IReplacementSourceBlockEntity source
                    ? source.getCreateTiersReplacementSourceBlockId()
                    : null;
            Tier effectiveTier = tiered.getTier();
            if (effectiveTier != null || sourceBlockId != null) {
                CREATETIERS$REMOVED_PULLEY_SOURCES.get().put(
                        pos.immutable(), new PulleySource(effectiveTier, sourceBlockId));
            }
        }
        level.removeBlockEntity(pos);
    }

    @Redirect(
            method = "onRemove",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private boolean createtiers$restoreTieredPulley(Level level, BlockPos pos, BlockState requestedState, int flags) {
        PulleySource source = CREATETIERS$REMOVED_PULLEY_SOURCES.get().remove(pos);
        if (source == null || !AllBlocks.SHAFT.has(requestedState)) {
            return level.setBlock(pos, requestedState, flags);
        }

        Block sourceBlock = createtiers$resolveSourceBlock(source.blockId());
        if (sourceBlock == AllBlocks.SHAFT.get()) {
            boolean changed = level.setBlock(pos, requestedState, flags);
            AttachedTierTransfer.restore(level, pos, source.tier(), source.blockId());
            return changed;
        }

        if (sourceBlock instanceof TieredShaftBlock shaft) {
            return level.setBlock(pos, createtiers$copyShaftState(shaft.defaultBlockState(), requestedState), flags);
        }

        boolean changed = level.setBlock(pos, requestedState, flags);
        AttachedTierTransfer.restore(level, pos, source.tier(), source.blockId());
        return changed;
    }

    @Inject(method = "onRemove", at = @At("RETURN"))
    private void createtiers$clearRemovedPulleyTiers(BlockState state, Level level, BlockPos pos,
            BlockState newState, boolean movedByPiston, CallbackInfo ci) {
        CREATETIERS$REMOVED_PULLEY_SOURCES.remove();
    }

    @Unique
    private static Block createtiers$resolveSourceBlock(ResourceLocation id) {
        if (id == null) {
            return null;
        }
        Block block = BuiltInRegistries.BLOCK.get(id);
        return id.equals(BuiltInRegistries.BLOCK.getKey(block)) ? block : null;
    }

    @Unique
    private static BlockState createtiers$copyShaftState(BlockState target, BlockState requestedState) {
        BlockState replacement = target.setValue(
                ShaftBlock.AXIS, requestedState.getValue(ShaftBlock.AXIS));
        if (requestedState.hasProperty(ShaftBlock.WATERLOGGED)
                && replacement.hasProperty(ShaftBlock.WATERLOGGED)) {
            replacement = replacement.setValue(
                    ShaftBlock.WATERLOGGED, requestedState.getValue(ShaftBlock.WATERLOGGED));
        }
        return replacement;
    }

}
