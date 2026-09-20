package com.createtiers.api;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntityType;

/**
 * Marker for native Create Tiers blocks that reuse an upstream Create block-entity type.
 *
 * <p>The marker lets the generic kinetic tier mixin resolve an intrinsic tier directly from
 * the block while keeping Create's own block entity implementation, behaviours and addon
 * compatibility intact.</p>
 */
public interface TieredNativeKineticBlock {

    Tier getTier();

    /** Upstream Create block id whose blockstate/item models this native variant mirrors. */
    ResourceLocation getBaseBlockId();

    /** Exact upstream Create block-entity type used by this generated native block. */
    BlockEntityType<?> getExpectedBlockEntityType();
}
