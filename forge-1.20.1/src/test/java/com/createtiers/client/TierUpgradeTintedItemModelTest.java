package com.createtiers.client;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TierUpgradeTintedItemModelTest {

    @Test
    void fullModeAddsMechanicalTintToPreviouslyUntintedQuad() {
        BakedQuad untinted = new BakedQuad(new int[32], -1, Direction.NORTH, null, true, true);
        List<BakedQuad> result = TierUpgradeTintedItemModel.tintQuads(
                TierUpgradeItemTintPolicy.Mode.FULL, List.of(untinted));

        assertEquals(1, result.size());
        assertEquals(0, result.get(0).getTintIndex());
        assertTrue(result.get(0).isTinted());
        assertArrayEquals(untinted.getVertices(), result.get(0).getVertices());
        assertEquals(untinted.getDirection(), result.get(0).getDirection());
    }

    @Test
    void existingSelectiveTintChannelIsPreservedVerbatim() {
        BakedQuad preTinted = new BakedQuad(new int[32], 7, Direction.SOUTH, null, true, true);
        List<BakedQuad> result = TierUpgradeTintedItemModel.tintQuads(
                TierUpgradeItemTintPolicy.Mode.FULL, List.of(preTinted));

        assertSame(preTinted, result.get(0));
        assertEquals(7, result.get(0).getTintIndex());
    }
}
