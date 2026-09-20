// Create Tiers - Example KubeJS Script
// Tier progression is derived from Max RPM and Max SU; there is no separate numeric level.
CreateTiers.registerTiers([
    // Basic - Entry capability
    { name: 'basic', maxRPM: 256, maxSU: 1024, shaftColor: 0xAAAAAA, cogwheelColor: 0x777777, displayName: 'Basic' },
    
    // Advanced - Mid capability
    { name: 'advanced', maxRPM: 512, maxSU: 2048, shaftColor: 0xDDFF00, cogwheelColor: 0xFFAA00, displayName: 'Advanced' },
    
    // Elite - High capability
    { name: 'elite', maxRPM: 1024, maxSU: 4096, shaftColor: 0x00FFBB, cogwheelColor: 0x55FF55, displayName: 'Elite' },
    
    // Ultimate - End-game capability
    { name: 'ultimate', maxRPM: 2048, maxSU: 8192, shaftColor: 0xFF00FF, cogwheelColor: 0xAA00AA, displayName: 'Ultimate' }
]);

// Console output to confirm registration
console.log('Create Tiers: Registered ' + CreateTiers.getAllTiers().size() + ' tiers via KubeJS');
