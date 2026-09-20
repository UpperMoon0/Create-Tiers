package com.createtiers.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TierLimitPolicyTest {

    @Test
    void untieredComponentKeepsCreateLimit() {
        assertEquals(256, TierLimitPolicy.allowedRPM(null, 256, false));
    }

    @Test
    void tieredComponentUsesItsOwnLimit() {
        Tier tier = new Tier(2, "advanced", 1024, 4096);
        assertEquals(1024, TierLimitPolicy.allowedRPM(tier, 256, false));
    }

    @Test
    void gaugeBypassRemainsUnlimited() {
        Tier tier = new Tier(1, "basic", 128, 512);
        assertEquals(Integer.MAX_VALUE, TierLimitPolicy.allowedRPM(tier, 256, true));
        assertEquals(Integer.MAX_VALUE, TierLimitPolicy.allowedRPM(null, 256, true));
    }
}
