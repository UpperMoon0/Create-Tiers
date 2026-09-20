package com.createtiers.foundation.utility;

import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.Tier;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Carries an attached runtime tier across Create block replacements that may
 * discard and recreate the kinetic block entity.
 */
public final class AttachedTierTransfer {

    private record Capture(BlockPos pos, Tier tier) {
    }

    private static final ThreadLocal<Deque<Capture>> CAPTURES =
            ThreadLocal.withInitial(ArrayDeque::new);

    private AttachedTierTransfer() {
    }

    public static Tier capture(Level level, BlockPos pos) {
        BlockEntity blockEntity = level.getBlockEntity(pos);
        return blockEntity instanceof IAttachedTierBlockEntity attachable
                ? attachable.getAttachedTier()
                : null;
    }

    public static void restore(Level level, BlockPos pos, Tier tier) {
        if (tier == null || level.isClientSide) {
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof IAttachedTierBlockEntity attachable)) {
            return;
        }

        // An intrinsically tiered replacement must never receive a second,
        // attached tier.
        if (attachable.getAttachedTier() == null && attachable.getTier() != null) {
            return;
        }

        if (!tier.equals(attachable.getAttachedTier())) {
            attachable.setAttachedTier(tier);
        }
    }

    public static void begin(Level level, BlockPos pos) {
        CAPTURES.get().push(new Capture(pos.immutable(), capture(level, pos)));
    }

    public static void end(Level level, BlockPos pos) {
        Deque<Capture> stack = CAPTURES.get();
        if (stack.isEmpty()) {
            return;
        }

        Capture capture = stack.pop();
        if (stack.isEmpty()) {
            CAPTURES.remove();
        }
        restore(level, capture.pos(), capture.tier());
    }
}
