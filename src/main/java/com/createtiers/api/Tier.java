package com.createtiers.api;

import java.util.Objects;

/**
 * Represents a tier for Create kinetic blocks.
 * Tiers can be customized via datapack or KubeJS.
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
     * Creates a new Tier
     * @param tier The tier number (must be unique)
     * @param name The internal name of the tier
     * @param maxRPM Maximum RPM for this tier
     * @param maxSU Maximum Stress Units for this tier
     * @param shaftColor The color of the shaft (hex RGB)
     * @param cogwheelColor The color of the cogwheel (hex RGB)
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
    
    /**
     * Creates a new Tier with default colors
     */
    public Tier(int tier, String name, int maxRPM, int maxSU) {
        this(tier, name, maxRPM, maxSU, 0xFFFFFF, 0xFFFFFF, null);
    }
    
    // Getters
    public int getTier() { return tier; }
    public String getName() { return name; }
    public int getMaxRPM() { return maxRPM; }
    public int getMaxSU() { return maxSU; }
    public int getShaftColor() { return shaftColor; }
    public int getCogwheelColor() { return cogwheelColor; }
    public String getDisplayName() { return displayName; }
    
    /**
     * Gets the primary color (for backward compatibility or UI)
     */
    public int getColor() { return cogwheelColor; }
    
    /**
     * Gets the maximum speed for processing recipes.
     * Default implementation returns maxRPM / 2.
     */
    public int getMaxSpeed() {
        return maxRPM / 2;
    }
    
    /**
     * Gets the maximum capacity for fluid handling.
     * Default implementation returns maxSU * 2.
     */
    public int getMaxCapacity() {
        return maxSU * 2;
    }
    
    /**
     * Create a new Tier builder.
     */
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
    
    /**
     * Builder class for creating Tier instances.
     * Useful for KubeJS and datapack integration.
     */
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
