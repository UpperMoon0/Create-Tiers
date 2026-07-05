package com.createtiers.client;

import com.createtiers.api.Tier;
import com.createtiers.content.kinetics.TieredGearboxBlock;
import com.createtiers.content.kinetics.TieredGearboxBlockEntity;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityVisual;
import com.simibubi.create.content.kinetics.base.RotatingInstance;
import com.simibubi.create.foundation.render.AllInstanceTypes;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.BlockEntityVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.AbstractInstance;
import dev.engine_room.flywheel.lib.instance.FlatLit;
import dev.engine_room.flywheel.lib.model.Models;
import net.createmod.catnip.data.Iterate;
import net.createmod.catnip.theme.Color;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Consumer;

public class TieredGearboxVisual extends KineticBlockEntityVisual<TieredGearboxBlockEntity> {
    private final EnumMap<Direction, RotatingInstance> shafts = new EnumMap<>(Direction.class);
    private final Tier tier;
    private Direction sourceFacing;

    public static BlockEntityVisual<TieredGearboxBlockEntity> create(VisualizationContext context,
            TieredGearboxBlockEntity blockEntity, float partialTick) {
        return new TieredGearboxVisual(context, blockEntity, partialTick);
    }

    public TieredGearboxVisual(VisualizationContext context, TieredGearboxBlockEntity blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        tier = ((TieredGearboxBlock) blockEntity.getBlockState().getBlock()).getTier();
        updateSourceFacing();
        var instancer = instancerProvider().instancer(AllInstanceTypes.ROTATING,
                Models.partial(AllTieredPartialModels.forTier(tier).SHAFT_HALF));
        Direction.Axis boxAxis = blockState.getValue(BlockStateProperties.AXIS);
        for (Direction direction : Iterate.directions) {
            Direction.Axis axis = direction.getAxis();
            if (boxAxis == axis)
                continue;
            RotatingInstance shaft = instancer.createInstance();
            shaft.setup(blockEntity, axis, getSpeed(direction))
                    .setPosition(getVisualPosition())
                    .rotateToFace(Direction.SOUTH, direction)
                    .setChanged();
            shafts.put(direction, shaft);
        }
        applyTierColor();
    }

    public static float getDirectionalSpeed(float speed, Direction sourceFacing, Direction direction) {
        // Opposite-facing shaft-half models are already rotated 180 degrees. Use one
        // canonical direction per axis so that model transform, rather than a second
        // RPM sign inversion, supplies the visible opposite-face rotation.
        Direction canonicalDirection = Direction.get(Direction.AxisDirection.POSITIVE, direction.getAxis());
        if (speed != 0 && sourceFacing != null) {
            if (sourceFacing.getAxis() == canonicalDirection.getAxis())
                speed *= sourceFacing == canonicalDirection ? 1 : -1;
            else if (sourceFacing.getAxisDirection() == canonicalDirection.getAxisDirection())
                speed *= -1;
        }
        return speed;
    }

    private float getSpeed(Direction direction) {
        return getDirectionalSpeed(blockEntity.getSpeed(), sourceFacing, direction);
    }

    private void updateSourceFacing() {
        if (blockEntity.hasSource()) {
            BlockPos source = blockEntity.source.subtract(pos);
            sourceFacing = Direction.getNearest(source.getX(), source.getY(), source.getZ());
        } else {
            sourceFacing = null;
        }
    }

    private void applyTierColor() {
        for (RotatingInstance shaft : shafts.values()) {
            shaft.setColor(new Color(tier.getShaftColor()));
            shaft.setChanged();
        }
    }

    @Override
    public void update(float partialTick) {
        updateSourceFacing();
        for (Map.Entry<Direction, RotatingInstance> entry : shafts.entrySet()) {
            Direction direction = entry.getKey();
            entry.getValue().setup(blockEntity, direction.getAxis(), getSpeed(direction)).setChanged();
        }
        applyTierColor();
    }

    @Override
    public void updateLight(float partialTick) {
        relight(shafts.values().toArray(FlatLit[]::new));
    }

    @Override
    protected void _delete() {
        shafts.values().forEach(AbstractInstance::delete);
        shafts.clear();
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        shafts.values().forEach(consumer);
    }
}
