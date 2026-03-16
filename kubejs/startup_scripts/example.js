// Create Tiers - Example KubeJS Script
// This script demonstrates how to register custom tiers for Create Tiers
// Updated for KubeJS 6.x

// Register 4 tiers with dual colors (shaft, cogwheel)
// Tier 1: Basic - Entry level tier
CreateTiers.registerTier('basic', 1, 256, 1024, 0xAAAAAA, 0x777777, 'Basic');

// Tier 2: Advanced - Mid tier
CreateTiers.registerTier('advanced', 2, 512, 2048, 0xDDFF00, 0xFFAA00, 'Advanced');

// Tier 3: Elite - High tier
CreateTiers.registerTier('elite', 3, 1024, 4096, 0x00FFBB, 0x55FF55, 'Elite');

// Tier 4: Ultimate - End game tier
CreateTiers.registerTier('ultimate', 4, 2048, 8192, 0xFF00FF, 0xAA00AA, 'Ultimate');

// Console output to confirm registration
console.log('Create Tiers: Registered ' + CreateTiers.getAllTiers().size() + ' tiers via KubeJS');

// Note: Blockstates, models, and textures are generated dynamically by the Java mod
// via TieredModelGenerator and TieredTextureGenerator classes.
// The DynamicResourcePack serves these assets at runtime.
//
// Do NOT generate blockstates/models via KubeJS as they will conflict with
// the Java-generated dynamic resources.
