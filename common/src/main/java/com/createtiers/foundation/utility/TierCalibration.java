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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Fallback interaction for Create kinetics that have no item form.
 *
 * <p>Normal item-backed machines must be calibrated through recipes so packs can
 * assign a meaningful per-machine upgrade cost. A tiered shaft remains useful for
 * in-world-only kinetic components such as belt segments, which cannot be the
 * output of an item recipe.</p>
 */
public final class TierCalibration {

    private TierCalibration() {
    }

    /**
     * Handle sneak-use of a tiered shaft on a non-itemized Create kinetic component.
     *
     * <p>For an item-backed component the shaft may only clear the exact currently
     * attached tier. It can never apply or change a tier, preventing the reusable
     * shaft from bypassing the configured calibration recipe.</p>
     *
     * @return {@code true} when the interaction belongs to Create Tiers and normal
     *         BlockItem placement should be suppressed
     */

    /**
     * Policy seam used by runtime tests: item-backed kinetics may only be cleared
     * with the exact attached tier, while components with no normal item form may
     * still use the shaft fallback for applying/changing a tier.
     */
    public static boolean canMutateWithShaft(KineticBlockEntity kinetic, Tier attached, Tier selected) {
        boolean clearing = selected.equals(attached);
        boolean hasNormalItemForm = kinetic.getBlockState().getBlock().asItem() != Items.AIR;
        return !hasNormalItemForm || clearing;
    }

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

        if (kinetic.getBlockState().getBlock() instanceof GaugeBlock) {
            return false;
        }

        Tier attached = attachable.getAttachedTier();
        Tier effective = attachable.getTier();

        // Native Create Tiers blocks already provide an intrinsic tier.
        if (effective != null && attached == null) {
            return false;
        }

        Tier selected = shaft.getTier();
        boolean clearing = selected.equals(attached);

        // Item-backed machines must pay their recipe-defined calibration cost.
        // Keeping exact-tier clearing here also provides a clean escape hatch for
        // worlds/items created by earlier PR builds without reopening the free upgrade.
        if (!canMutateWithShaft(kinetic, attached, selected)) {
            return false;
        }

        if (!level.isClientSide) {
            if (clearing) {
                attachable.clearAttachedTier();
                player.displayClientMessage(Component.translatable("createtiers.message.tier_cleared"), true);
            } else {
                attachable.setAttachedTier(selected);
                player.displayClientMessage(
                        Component.translatable("createtiers.message.tier_attached_fallback", selected.getDisplayName()), true);
            }
        }

        return true;
    }
}
