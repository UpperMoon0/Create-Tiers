// Create Tiers - Example KubeJS Script
// This script demonstrates how to register custom tiers for Create Tiers
CreateTiers.registerTiers([
    // Tier 1: Basic - Entry level tier
    { name: 'basic', level: 1, maxRPM: 256, maxSU: 1024, shaftColor: 0xAAAAAA, cogwheelColor: 0x777777, displayName: 'Basic' },
    
    // Tier 2: Advanced - Mid tier
    { name: 'advanced', level: 2, maxRPM: 512, maxSU: 2048, shaftColor: 0xDDFF00, cogwheelColor: 0xFFAA00, displayName: 'Advanced' },
    
    // Tier 3: Elite - High tier
    { name: 'elite', level: 3, maxRPM: 1024, maxSU: 4096, shaftColor: 0x00FFBB, cogwheelColor: 0x55FF55, displayName: 'Elite' },
    
    // Tier 4: Ultimate - End game tier
    { name: 'ultimate', level: 4, maxRPM: 2048, maxSU: 8192, shaftColor: 0xFF00FF, cogwheelColor: 0xAA00AA, displayName: 'Ultimate' }
]);

// Console output to confirm registration
console.log('Create Tiers: Registered ' + CreateTiers.getAllTiers().size() + ' tiers via KubeJS');