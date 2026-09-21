package com.createtiers.foundation.utility;

import com.createtiers.api.IAttachedTierBlockEntity;
import com.createtiers.api.IReplacementSourceBlockEntity;
import com.createtiers.api.Tier;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.createtiers.foundation.item.TierUpgradeItemData;
import com.simibubi.create.AllBlocks;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Resolves the shaft item represented by a Create belt pulley's persisted
 * replacement-source provenance.
 */
public final class BeltPulleySourcePolicy {

    private BeltPulleySourcePolicy() {
    }

    /**
     * Return the shaft item that must be refunded/dropped for this pulley.
     * Untiered or invalid provenance intentionally falls back to Create's
     * ordinary shaft.
     */
    public static ItemStack sourceStack(BlockEntity blockEntity) {
        if (!(blockEntity instanceof IReplacementSourceBlockEntity provenance)) {
            return AllBlocks.SHAFT.asStack();
        }

        ResourceLocation sourceId = provenance.getCreateTiersReplacementSourceBlockId();
        Block sourceBlock = ReplacementSourcePolicy.resolveLegalSource(
                blockEntity.getBlockState(), sourceId);

        if (sourceBlock instanceof TieredShaftBlock) {
            return sourceBlock.asItem().getDefaultInstance();
        }

        if (sourceBlock == AllBlocks.SHAFT.get()
                && blockEntity instanceof IAttachedTierBlockEntity attachable) {
            Tier tier = attachable.getAttachedTier();
            if (tier != null) {
                return TierUpgradeItemData.upgradedCopy(AllBlocks.SHAFT.asStack(), tier);
            }
        }

        return AllBlocks.SHAFT.asStack();
    }
}
