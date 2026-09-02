package com.createtiers.foundation.utility;

import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.gauge.GaugeBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Shared interaction policy for attaching tiers to Create kinetic components. */
public final class TierCalibration {

    private TierCalibration() {
    }

    /**
     * Handle sneak-use of a tiered shaft on an ordinary Create kinetic component.
     *
     * @return {@code true} when the interaction belongs to Create Tiers and normal
     *         BlockItem placement should be suppressed
     */
    public static boolean tryCalibrate(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return false;
        }

        ItemStack held = context.getItemInHand();
        if (!(held.getItem() instanceof BlockItem blockItem)
                || !(blockItem.getBlock() instanceof TieredShaftBlock shaft)) {
            return false;
        }

        Level level = context.getLevel();
        BlockEntity blockEntity = level.getBlockEntity(context.getClickedPos());
        if (!(blockEntity instanceof KineticBlockEntity kinetic)
                || !(blockEntity instanceof IAttachedTierBlockEntity attachable)) {
            return false;
        }

        // Gauges are observers. Create Tiers deliberately lets them observe any RPM,
        // so attaching a tier to them would be misleading and mechanically meaningless.
        if (kinetic.getBlockState().getBlock() instanceof GaugeBlock) {
            return false;
        }

        Tier attached = attachable.getAttachedTier();
        Tier effective = attachable.getTier();

        // Native Create Tiers blocks already provide an intrinsic tier through their
        // own block entity subclass. Do not mask or stack another runtime attachment.
        if (effective != null && attached == null) {
            return false;
        }

        Tier selected = shaft.getTier();
        boolean clearing = selected.equals(attached);

        if (!level.isClientSide) {
            if (clearing) {
                attachable.clearAttachedTier();
                player.displayClientMessage(Component.translatable("createtiers.message.tier_cleared"), true);
            } else {
                attachable.setAttachedTier(selected);
                player.displayClientMessage(
                        Component.translatable("createtiers.message.tier_attached", selected.getDisplayName()), true);
            }
        }

        return true;
    }
}
