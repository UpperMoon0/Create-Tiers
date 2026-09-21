package com.createtiers.client;

import com.simibubi.create.content.kinetics.chainDrive.ChainDriveBlock;
import com.simibubi.create.content.kinetics.gearbox.GearboxBlock;
import com.simibubi.create.content.kinetics.simpleRelays.ICogWheel;
import com.simibubi.create.content.kinetics.simpleRelays.encased.EncasedShaftBlock;
import com.simibubi.create.content.kinetics.speedController.SpeedControllerBlock;
import com.simibubi.create.content.kinetics.transmission.ClutchBlock;
import com.simibubi.create.content.kinetics.transmission.GearshiftBlock;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;

/**
 * Item-model tint policy for recipe-produced tier upgrades.
 *
 * <p>Most Create machines have no Create Tiers-specific item tint contract, so
 * their whole baked item model receives the tier's mechanical color. Families
 * whose item model deliberately separates shaft/cogwheel material keep that
 * separation instead of recoloring their casing.</p>
 */
public final class TierUpgradeItemTintPolicy {

    public enum Mode {
        FULL,
        SHAFT_ONLY,
        COGWHEEL
    }

    private TierUpgradeItemTintPolicy() {
    }

    public static Mode forBlock(Block block) {
        if (block instanceof ICogWheel) {
            return Mode.COGWHEEL;
        }
        if (block instanceof GearboxBlock
                || block instanceof EncasedShaftBlock
                || block instanceof ClutchBlock
                || block instanceof GearshiftBlock
                || block instanceof ChainDriveBlock
                || block instanceof SpeedControllerBlock) {
            return Mode.SHAFT_ONLY;
        }
        return Mode.FULL;
    }

    /**
     * Resolve the tint channel for an otherwise-untinted quad.
     *
     * <p>Channel 0 is the tier mechanical/shaft color and channel 1 is the
     * cogwheel color. A negative result preserves the original quad unchanged.</p>
     */
    public static int tintIndex(Mode mode, ResourceLocation sprite) {
        if (mode == Mode.FULL) {
            return 0;
        }

        String namespace = sprite.getNamespace();
        String path = sprite.getPath();
        if (!"create".equals(namespace)) {
            return -1;
        }

        if (isShaftTexture(path)) {
            return 0;
        }
        if (mode == Mode.COGWHEEL && isCogwheelTexture(path)) {
            return 1;
        }
        return -1;
    }

    private static boolean isShaftTexture(String path) {
        return "block/axis".equals(path)
                || "block/axis_top".equals(path)
                || "block/cogwheel_axis".equals(path);
    }

    private static boolean isCogwheelTexture(String path) {
        return "block/cogwheel".equals(path)
                || "block/large_cogwheel".equals(path);
    }
}
