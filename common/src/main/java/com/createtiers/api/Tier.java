package com.createtiers.api;

import java.util.Objects;

/**
 * Represents a tier for Create kinetic blocks.
 * Tiers are registered during startup via KubeJS or another mod integration.
 */
public class Tier implements Comparable<Tier> {

    private final int tier;
    private final String name;
    private final int maxRPM;
    private final int maxSU;
    private final int shaftColor;
    private final int cogwheelColor;
    private final String displayName;

    /**
     * Creates a new Tier.
     *
     * @param tier The tier number (must be unique)
     * @param name The internal/generated component name of the tier
     * @param maxRPM Maximum RPM this tiered component may receive
     * @param maxSU Hard stress-cap for a connected kinetic network containing this tier
     * @param shaftColor The color of the shaft (24-bit RGB)
     * @param cogwheelColor The color of the cogwheel (24-bit RGB)
     * @param displayName Optional display name for the tier
     */
    public Tier(int tier, String name, int maxRPM, int maxSU, int shaftColor, int cogwheelColor, String displayName) {
        this.tier = tier;
        this.name = name;
        this.maxRPM = maxRPM;
        this.maxSU = maxSU;
        this.shaftColor = shaftColor;
        this.cogwheelColor = cogwheelColor;
        this.displayName = displayName != null ? displayName : name;
    }

    public Tier(int tier, String name, int maxRPM, int maxSU) {
        this(tier, name, maxRPM, maxSU, 0xFFFFFF, 0xFFFFFF, null);
    }

    public int getTier() { return tier; }
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

    @Override
    public int compareTo(Tier other) {
        return Integer.compare(this.tier, other.tier);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Tier other = (Tier) obj;
        return tier == other.tier && name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tier, name);
    }

    @Override
    public String toString() {
        return "Tier{" +
                "tier=" + tier +
                ", name='" + name + '\'' +
                ", maxRPM=" + maxRPM +
                ", maxSU=" + maxSU +
                ", shaftColor=" + String.format("#%06X", shaftColor) +
                ", cogwheelColor=" + String.format("#%06X", cogwheelColor) +
                '}';
    }

    /** Builder for startup integrations such as KubeJS and other mods. */
    public static class Builder {
        private int tier = 1;
        private String name = "tier_1";
        private int maxRPM = 256;
        private int maxSU = 1024;
        private int shaftColor = 0xFFFFFF;
        private int cogwheelColor = 0xFFFFFF;
        private String displayName = null;

        public Builder tier(int tier) {
            this.tier = tier;
            return this;
        }

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
            return new Tier(tier, name, maxRPM, maxSU, shaftColor, cogwheelColor, displayName);
        }
    }
}
