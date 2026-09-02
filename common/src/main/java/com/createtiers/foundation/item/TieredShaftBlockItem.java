package com.createtiers.foundation.item;

import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.gauge.GaugeBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Tiered shaft item with a secondary calibration interaction.
 *
 * <p>Normal use places the shaft exactly like before. Sneak-use on an ordinary
 * Create kinetic block entity attaches this shaft's tier to that component.
 * Sneak-use with the same tier again removes the attachment. The shaft acts as
 * a reusable calibration key so breaking/replacing an upstream Create machine
 * cannot destroy an opaque upgrade item.</p>
 */
public class TieredShaftBlockItem extends BlockItem {

    public TieredShaftBlockItem(TieredShaftBlock block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isShiftKeyDown()) {
            return super.useOn(context);
        }

        Level level = context.getLevel();
        BlockEntity blockEntity = level.getBlockEntity(context.getClickedPos());
        if (!(blockEntity instanceof KineticBlockEntity kinetic)
                || !(blockEntity instanceof IAttachedTierBlockEntity attachable)) {
            return super.useOn(context);
        }

        // Gauges are observation devices and deliberately retain Create Tiers' unlimited
        // RPM observation exemption.
        if (kinetic.getBlockState().getBlock() instanceof GaugeBlock) {
            return InteractionResult.PASS;
        }

        Tier attached = attachable.getAttachedTier();
        Tier effective = attachable.getTier();

        // Native Create Tiers blocks already have an intrinsic tier supplied by their
        // own block entity implementation. Do not layer an attached tier over it.
        if (effective != null && attached == null) {
            return InteractionResult.PASS;
        }

        Tier selected = ((TieredShaftBlock) getBlock()).getTier();
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

        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
