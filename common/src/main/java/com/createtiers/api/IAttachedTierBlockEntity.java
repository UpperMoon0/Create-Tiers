package com.createtiers.api;

import net.minecraft.resources.ResourceLocation;

/**
 * Runtime tier state attached to an otherwise ordinary Create kinetic block entity.
 *
 * <p>The intrinsic tiered blocks provided by Create Tiers still override
 * {@link ITieredBlockEntity#getTier()} directly. This interface is for opt-in
 * tiering of Create's existing kinetic components without replacing their block
 * or block-entity classes.</p>
 */
public interface IAttachedTierBlockEntity extends ITieredBlockEntity {

    /** Returns the persisted registered tier id, or {@code null} when untiered. */
    ResourceLocation getAttachedTierId();

    /** Returns only the attached runtime tier, excluding intrinsic tier implementations. */
    Tier getAttachedTier();

    /** Attach a registered tier and rebuild this component's kinetic connection. */
    void setAttachedTier(Tier tier);

    /** Remove an attached tier and restore ordinary Create kinetic limits. */
    void clearAttachedTier();
}
