package com.createtiers.foundation.utility;

import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.IReplacementSourceBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.gauge.GaugeBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * In-world tier-upgrade interaction for Create kinetics that have no item form.
 *
 * <p>Normal item-backed machines are upgraded exclusively through tier-upgrade
 * recipes. A tiered shaft is only an interaction token for components such as
 * belt segments that cannot be crafted as standalone items.</p>
 */
public final class InWorldTierUpgrade {

    private InWorldTierUpgrade() {
    }

    /**
     * Item-backed Create blocks must always use their configured recipe. Only
     * genuinely non-itemized kinetics may be changed with a tiered shaft.
     */
    public static boolean canApplyWithShaft(KineticBlockEntity kinetic, Tier attached, Tier selected) {
        if (hasIntrinsicReplacementSource(kinetic)) {
            return false;
        }
        return kinetic.getBlockState().getBlock().asItem() == Items.AIR;
    }

    private static boolean hasIntrinsicReplacementSource(KineticBlockEntity kinetic) {
        if (!(kinetic instanceof IReplacementSourceBlockEntity source)) {
            return false;
        }
        var id = source.getCreateTiersReplacementSourceBlockId();
        if (id == null) {
            return false;
        }
        var block = BuiltInRegistries.BLOCK.get(id);
        return id.equals(BuiltInRegistries.BLOCK.getKey(block))
                && block instanceof TieredShaftBlock;
    }

    public static boolean tryApply(UseOnContext context) {
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

        // Native Create Tiers blocks already own their tier; their transformed forms
        // preserve source provenance instead of accepting an attached tier.
        if (effective != null && attached == null) {
            return false;
        }

        Tier selected = shaft.getTier();
        if (!canApplyWithShaft(kinetic, attached, selected)) {
            return false;
        }

        boolean clearing = selected.equals(attached);
        if (!level.isClientSide) {
            if (clearing) {
                attachable.clearAttachedTier();
                player.displayClientMessage(Component.translatable("createtiers.message.tier_cleared"), true);
            } else {
                attachable.setAttachedTier(selected);
                player.displayClientMessage(
                        Component.translatable("createtiers.message.in_world_tier_applied", selected.getDisplayName()),
                        true);
            }
        }

        return true;
    }
}
