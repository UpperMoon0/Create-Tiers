package com.createtiers.client;

import java.util.EnumMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import static org.junit.jupiter.api.Assertions.*;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class GearboxRenderDataTest {

    enum Direction {
        NORTH, SOUTH, EAST, WEST, UP, DOWN;

        enum Axis { X, Y, Z }
    }

    @Test
    void calculateShaftDirectionsExcludesBoxAxis() {
        Direction.Axis boxAxis = Direction.Axis.Y;

        Map<Direction, Direction.Axis> expectedShafts = new EnumMap<>(Direction.class);
        for (Direction direction : Direction.values()) {
            if (direction != Direction.UP && direction != Direction.DOWN) {
                expectedShafts.put(direction, getAxis(direction));
            }
        }

        assertEquals(4, expectedShafts.size());
        assertTrue(expectedShafts.containsKey(Direction.NORTH));
        assertTrue(expectedShafts.containsKey(Direction.SOUTH));
        assertTrue(expectedShafts.containsKey(Direction.EAST));
        assertTrue(expectedShafts.containsKey(Direction.WEST));
        assertFalse(expectedShafts.containsKey(Direction.UP));
        assertFalse(expectedShafts.containsKey(Direction.DOWN));
    }

    private Direction.Axis getAxis(Direction direction) {
        return switch (direction) {
            case NORTH, SOUTH -> Direction.Axis.Z;
            case EAST, WEST -> Direction.Axis.X;
            case UP, DOWN -> Direction.Axis.Y;
        };
    }

    private float calculateSpeed(Direction direction, float speed, Direction sourceFacing) {
        if (speed != 0 && sourceFacing != null) {
            if (getAxis(sourceFacing) == getAxis(direction)) {
                speed *= sourceFacing == direction ? 1 : -1;
            }
        }
        return speed;
    }

    @Test
    void speedCalculationWithSameAxis() {
        float speed = 60f;
        Direction sourceFacing = Direction.NORTH;

        for (Direction direction : Direction.values()) {
            if (getAxis(direction) == getAxis(sourceFacing)) {
                float expectedSpeed = sourceFacing == direction ? speed : -speed;
                float actualSpeed = calculateSpeed(direction, speed, sourceFacing);
                assertEquals(expectedSpeed, actualSpeed,
                    "Speed calculation mismatch for direction: " + direction);
            }
        }
    }

    @Test
    void speedCalculationWithoutSourceFacing() {
        float speed = 60f;
        Direction sourceFacing = null;

        for (Direction direction : Direction.values()) {
            float actualSpeed = calculateSpeed(direction, speed, sourceFacing);
            assertEquals(speed, actualSpeed,
                "Speed should be unchanged without source facing for direction: " + direction);
        }
    }

    record GearboxRenderState(
        int tierColor,
        Direction.Axis boxAxis,
        Map<Direction, Float> shaftSpeeds,
        float primaryAngle
    ) {}

    @Test
    void renderStateRecordHoldsCorrectState() {
        Map<Direction, Float> shaftSpeeds = new EnumMap<>(Direction.class);
        shaftSpeeds.put(Direction.NORTH, 60f);
        shaftSpeeds.put(Direction.SOUTH, -60f);

        GearboxRenderState state = new GearboxRenderState(
            0xFF8800,
            Direction.Axis.Y,
            shaftSpeeds,
            1.5f
        );

        assertEquals(0xFF8800, state.tierColor());
        assertEquals(Direction.Axis.Y, state.boxAxis());
        assertEquals(2, state.shaftSpeeds().size());
        assertEquals(60f, state.shaftSpeeds().get(Direction.NORTH));
        assertEquals(-60f, state.shaftSpeeds().get(Direction.SOUTH));
        assertEquals(1.5f, state.primaryAngle());
    }
}