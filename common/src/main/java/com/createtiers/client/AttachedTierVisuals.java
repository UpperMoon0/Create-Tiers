package com.createtiers.client;

import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.mixin.KineticBlockEntityAccessor;
import com.createtiers.mixin.KineticEffectHandlerAccessor;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticEffectHandler;
import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import net.createmod.catnip.theme.Color;

/** Client-side color policy for ordinary Create kinetics with an attached tier. */
public final class AttachedTierVisuals {

    private AttachedTierVisuals() {
    }

    /** Returns only an attached runtime tier; intrinsic Create Tiers blocks are left to their own renderer. */
    public static Tier getAttachedTier(KineticBlockEntity blockEntity) {
        if (blockEntity instanceof IAttachedTierBlockEntity attached) {
            return attached.getAttachedTier();
        }
        return null;
    }

    /**
     * Create cogwheel blocks, including encased cogs, use the tier's cogwheel color. All other
     * ordinary Create kinetic components use the shaft/mechanical color so casing and machine
     * identity remain readable.
     */
    public static Color getBaseColor(KineticBlockEntity blockEntity) {
        Tier tier = getAttachedTier(blockEntity);
        if (tier == null) {
            return null;
        }

        boolean cogwheel = blockEntity.getBlockState().getBlock() instanceof ICogWheel;
        return new Color(cogwheel ? tier.getCogwheelColor() : tier.getShaftColor());
    }

    /**
     * Color for rotating models that are safe to tint as one material.
     *
     * Create's belt pulley is a mixed-material model: the shaft uses Create axis textures while
     * the pulley body uses dark-oak textures. Tinting its single RotatingInstance recolors the
     * wooden pulley into a solid tier-colored block, so belts deliberately keep the vanilla
     * rotating pulley materials. The scrolling belt itself still uses getRenderedColor() through
     * RotatingInstance.colorFromBE().
     */
    public static Color getWholeRotatingModelColor(KineticBlockEntity blockEntity) {
        if (blockEntity instanceof BeltBlockEntity) {
            return null;
        }
        return getRenderedColor(blockEntity);
    }

    /** Apply Create's red/green kinetic stress feedback on top of the tier color. */
    public static Color getRenderedColor(KineticBlockEntity blockEntity) {
        Color base = getBaseColor(blockEntity);
        if (base == null) {
            return null;
        }

        KineticEffectHandler effects = ((KineticBlockEntityAccessor) blockEntity).getEffects();
        if (effects == null) {
            return base;
        }

        float effect = ((KineticEffectHandlerAccessor) effects).getOverStressedEffect();
        if (effect == 0) {
            return base;
        }

        boolean overstressed = effect > 0;
        Color effectColor = overstressed ? Color.RED : Color.SPRING_GREEN;
        float weight = overstressed ? effect : -effect;
        return base.mixWith(effectColor, weight);
    }
}
