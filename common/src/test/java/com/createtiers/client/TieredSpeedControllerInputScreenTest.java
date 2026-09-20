package com.createtiers.client;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TieredSpeedControllerInputScreenTest {

    @Test
    void signedRpmValidationAcceptsBothDirectionsAtTierLimit() {
        assertEquals(2048, TieredSpeedControllerInputScreen.parseSignedRpm("2048", 2048));
        assertEquals(-2048, TieredSpeedControllerInputScreen.parseSignedRpm("-2048", 2048));
        assertEquals(1, TieredSpeedControllerInputScreen.parseSignedRpm("1", 2048));
        assertEquals(-1, TieredSpeedControllerInputScreen.parseSignedRpm("-1", 2048));
    }

    @Test
    void signedRpmValidationRejectsZeroOverflowAndNonNumbers() {
        assertNull(TieredSpeedControllerInputScreen.parseSignedRpm("0", 2048));
        assertNull(TieredSpeedControllerInputScreen.parseSignedRpm("2049", 2048));
        assertNull(TieredSpeedControllerInputScreen.parseSignedRpm("-2049", 2048));
        assertNull(TieredSpeedControllerInputScreen.parseSignedRpm("", 2048));
        assertNull(TieredSpeedControllerInputScreen.parseSignedRpm("-", 2048));
        assertNull(TieredSpeedControllerInputScreen.parseSignedRpm("12.5", 2048));
        assertNull(TieredSpeedControllerInputScreen.parseSignedRpm("999999999999", 2048));
    }

    @Test
    void editFilterAllowsOnlyPartialSignedIntegers() {
        assertTrue(TieredSpeedControllerInputScreen.isPartialSignedInteger(""));
        assertTrue(TieredSpeedControllerInputScreen.isPartialSignedInteger("-"));
        assertTrue(TieredSpeedControllerInputScreen.isPartialSignedInteger("-512"));
        assertTrue(TieredSpeedControllerInputScreen.isPartialSignedInteger("512"));
        assertFalse(TieredSpeedControllerInputScreen.isPartialSignedInteger("+512"));
        assertFalse(TieredSpeedControllerInputScreen.isPartialSignedInteger("5.12"));
        assertFalse(TieredSpeedControllerInputScreen.isPartialSignedInteger("rpm"));
    }
}
