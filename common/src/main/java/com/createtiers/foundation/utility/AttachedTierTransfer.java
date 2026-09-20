package com.createtiers.foundation.utility;

import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.IReplacementSourceBlockEntity;
import com.createtiers.api.Tier;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayDeque;
import java.util.Deque;

public final class AttachedTierTransfer {
    private record Capture(BlockPos pos, Tier tier, ResourceLocation sourceBlockId) {}
    private static final ThreadLocal<Deque<Capture>> CAPTURES =
            ThreadLocal.withInitial(ArrayDeque::new);

    private AttachedTierTransfer() {}

    private static Capture capture(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof IAttachedTierBlockEntity attachable)) {
            return new Capture(pos.immutable(), null, null);
        }
        Tier tier = attachable.getAttachedTier();
        if (tier == null) return new Capture(pos.immutable(), null, null);

        ResourceLocation sourceId = blockEntity instanceof IReplacementSourceBlockEntity source
                ? source.getCreateTiersReplacementSourceBlockId() : null;
        if (sourceId == null) {
            sourceId = BuiltInRegistries.BLOCK.getKey(blockEntity.getBlockState().getBlock());
        }
        return new Capture(pos.immutable(), tier, sourceId);
    }

    public static void restore(Level level, BlockPos pos, Tier tier, ResourceLocation sourceBlockId) {
        if (tier == null || level.isClientSide) return;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof IAttachedTierBlockEntity attachable)
                || !(blockEntity instanceof KineticBlockEntity kinetic)) return;

        if (attachable.getAttachedTier() == null && attachable.getTier() != null) return;

        if (blockEntity instanceof IReplacementSourceBlockEntity source) {
            ResourceLocation currentId = BuiltInRegistries.BLOCK.getKey(blockEntity.getBlockState().getBlock());
            if (sourceBlockId != null && !sourceBlockId.equals(currentId)) {
                source.setCreateTiersReplacementSourceBlockId(sourceBlockId);
            } else {
                source.clearCreateTiersReplacementSourceBlockId();
            }
        }

        if (!AttachedTierAuthorization.canCarry(kinetic, tier)) return;
        if (!tier.equals(attachable.getAttachedTier())) attachable.setAttachedTier(tier);
    }

    public static void begin(Level level, BlockPos pos) {
        CAPTURES.get().push(capture(level, pos));
    }

    public static void end(Level level, BlockPos pos) {
        Deque<Capture> stack = CAPTURES.get();
        if (stack.isEmpty()) return;
        Capture capture = stack.pop();
        if (stack.isEmpty()) CAPTURES.remove();
        restore(level, capture.pos(), capture.tier(), capture.sourceBlockId());
    }
}
