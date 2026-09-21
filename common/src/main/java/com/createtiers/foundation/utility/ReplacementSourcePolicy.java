package com.createtiers.foundation.utility;

import com.createtiers.api.TieredNativeKineticBlock;
import com.createtiers.content.kinetics.TieredShaftBlock;
import com.simibubi.create.AllBlocks;
import com.simibubi.create.content.decoration.girder.GirderEncasedShaftBlock;
import com.simibubi.create.content.kinetics.belt.BeltBlock;
import com.simibubi.create.content.kinetics.belt.BeltPart;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedCogwheelBlock;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedShaftBlock;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Defines the Create block replacements that may legitimately carry source
 * provenance. Persisted source ids are never trusted outside these relations.
 */
public final class ReplacementSourcePolicy {

    private ReplacementSourcePolicy() {
    }

    public static boolean isLegal(BlockState replacementState, ResourceLocation sourceId) {
        if (sourceId == null) {
            return false;
        }
        Block source = BuiltInRegistries.BLOCK.get(sourceId);
        return sourceId.equals(BuiltInRegistries.BLOCK.getKey(source))
                && isLegal(replacementState, source);
    }

    public static boolean isLegal(BlockState replacementState, Block source) {
        Block replacement = replacementState.getBlock();
        if (replacement instanceof TieredNativeKineticBlock) {
            return false;
        }

        if (AllBlocks.BELT.has(replacementState)) {
            return replacementState.hasProperty(BeltBlock.PART)
                    && replacementState.getValue(BeltBlock.PART) != BeltPart.MIDDLE
                    && isShaftSource(source);
        }

        if (replacement == AllBlocks.POWERED_SHAFT.get()) {
            return source == AllBlocks.SHAFT.get();
        }

        if (replacement instanceof EncasedShaftBlock
                || replacement instanceof GirderEncasedShaftBlock) {
            return source == AllBlocks.SHAFT.get();
        }

        if (replacement instanceof EncasedCogwheelBlock encased) {
            return encased.isLargeCog()
                    ? source == AllBlocks.LARGE_COGWHEEL.get()
                    : source == AllBlocks.COGWHEEL.get();
        }

        return false;
    }

    public static Block resolveLegalSource(BlockState replacementState, ResourceLocation sourceId) {
        if (!isLegal(replacementState, sourceId)) {
            return null;
        }
        return BuiltInRegistries.BLOCK.get(sourceId);
    }

    private static boolean isShaftSource(Block source) {
        return source == AllBlocks.SHAFT.get() || source instanceof TieredShaftBlock;
    }
}
