package com.createtiers.api;

/**
 * Shared policy for tier-aware kinetic limits.
 */
public final class TierLimitPolicy {

    private TierLimitPolicy() {
    }

    /**
     * Resolve the maximum RPM a receiving kinetic component may accept.
     * Untiered Create components always retain Create's configured limit.
     * Tiered components use their own explicit tier limit.
     */
    public static int allowedRPM(Tier tier, int createDefault, boolean bypassLimit) {
        if (bypassLimit) {
            return Integer.MAX_VALUE;
        }
        return tier != null ? tier.getMaxRPM() : createDefault;
    }
}
