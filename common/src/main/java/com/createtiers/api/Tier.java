package com.createtiers.api;

import java.util.Objects;

/**
 * Represents a tier for Create kinetic blocks.
 * Tiers are registered during startup via KubeJS or another mod integration.
 *
 * <p>Tier progression is derived from kinetic capability. There is no separate
 * numeric level: higher-capability tiers have non-decreasing Max RPM and Max SU.</p>
 */
public class Tier implements Comparable<Tier> {

    private final String name;
    private final int maxRPM;
    private final int maxSU;
    private final int shaftColor;
    private final int cogwheelColor;
    private final String displayName;

    /**
     * Creates a new Tier.
     *
     * @param name The internal/generated component name of the tier
     * @param maxRPM Maximum RPM this tiered component may receive
     * @param maxSU Hard stress-cap for a connected kinetic network containing this tier
     * @param shaftColor The color of the shaft (24-bit RGB)
     * @param cogwheelColor The color of the cogwheel (24-bit RGB)
     * @param displayName Optional display name for the tier
     */
    public Tier(String name, int maxRPM, int maxSU, int shaftColor, int cogwheelColor, String displayName) {
        this.name = name;
        this.maxRPM = maxRPM;
        this.maxSU = maxSU;
        this.shaftColor = shaftColor;
        this.cogwheelColor = cogwheelColor;
        this.displayName = displayName != null ? displayName : name;
    }

    public Tier(String name, int maxRPM, int maxSU) {
        this(name, maxRPM, maxSU, 0xFFFFFF, 0xFFFFFF, null);
    }

    public String getName() { return name; }
    public int getMaxRPM() { return maxRPM; }
    public int getMaxSU() { return maxSU; }
    public int getShaftColor() { return shaftColor; }
    public int getCogwheelColor() { return cogwheelColor; }
    public String getDisplayName() { return displayName; }

    /** Primary visual color retained for API compatibility. */
    public int getColor() { return cogwheelColor; }

    /**
     * Legacy derived processing-speed helper retained for compatibility.
     * This is not the kinetic RPM limit; use {@link #getMaxRPM()} for that.
     */
    public int getMaxSpeed() {
        return maxRPM / 2;
    }

    /**
     * Legacy derived capacity helper retained for compatibility.
     * This is not the connected-network stress limit; use {@link #getMaxSU()} for that.
     */
    public int getMaxCapacity() {
        return maxSU * 2;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Natural order is capability-derived. Registered tiers are guaranteed not to
     * cross (higher RPM with lower SU), so RPM then SU is a valid progression order.
     * Name is only a deterministic tie-breaker for equal-capability tiers.
     */
    @Override
    public int compareTo(Tier other) {
        int rpm = Integer.compare(this.maxRPM, other.maxRPM);
        if (rpm != 0) return rpm;
        int su = Integer.compare(this.maxSU, other.maxSU);
        if (su != 0) return su;
        return this.name.compareTo(other.name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Tier other = (Tier) obj;
        return maxRPM == other.maxRPM && maxSU == other.maxSU && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, maxRPM, maxSU);
    }

    @Override
    public String toString() {
        return "Tier{" +
                "name='" + name + '\'' +
                ", maxRPM=" + maxRPM +
                ", maxSU=" + maxSU +
                ", shaftColor=" + String.format("#%06X", shaftColor) +
                ", cogwheelColor=" + String.format("#%06X", cogwheelColor) +
                '}';
    }

    /** Builder for startup integrations such as KubeJS and other mods. */
    public static class Builder {
        private String name = "tier";
        private int maxRPM = 256;
        private int maxSU = 1024;
        private int shaftColor = 0xFFFFFF;
        private int cogwheelColor = 0xFFFFFF;
        private String displayName = null;

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder maxRPM(int maxRPM) {
            this.maxRPM = maxRPM;
            return this;
        }

        public Builder maxSU(int maxSU) {
            this.maxSU = maxSU;
            return this;
        }

        public Builder shaftColor(int color) {
            this.shaftColor = color;
            return this;
        }

        public Builder cogwheelColor(int color) {
            this.cogwheelColor = color;
            return this;
        }

        public Builder color(int color) {
            this.shaftColor = color;
            this.cogwheelColor = color;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Tier build() {
            return new Tier(name, maxRPM, maxSU, shaftColor, cogwheelColor, displayName);
        }
    }
}
