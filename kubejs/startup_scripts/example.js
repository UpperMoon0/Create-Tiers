// Create Tiers - KubeJS startup example
//
// Place tier definitions and upgrade authorization in kubejs/startup_scripts.
// Ship the same startup script to the client and dedicated server.
// Any change here requires a full restart; /reload is not enough.
//
// ORDER MATTERS: tiers must be registered before upgrades reference them.

// 1) Define the tier registry first.
CreateTiers.registerTiers([
    {
        name: 'basic',
        maxRPM: 256,
        maxSU: 1024,
        shaftColor: 0xAAAAAA,
        cogwheelColor: 0x777777,
        displayName: 'Basic'
    },
    {
        name: 'advanced',
        maxRPM: 512,
        maxSU: 2048,
        shaftColor: 0xDDFF00,
        cogwheelColor: 0xFFAA00,
        displayName: 'Advanced'
    },
    {
        name: 'elite',
        maxRPM: 1024,
        maxSU: 4096,
        shaftColor: 0x00FFBB,
        cogwheelColor: 0x55FF55,
        displayName: 'Elite'
    },
    {
        name: 'ultimate',
        maxRPM: 2048,
        maxSU: 8192,
        shaftColor: 0xFF00FF,
        cogwheelColor: 0xAA00AA,
        displayName: 'Ultimate'
    }
])

// 2) After all referenced tiers exist, authorize ordinary Create item+tier variants.
// defaultRecipe defaults to true. Use false when server_scripts owns progression.
CreateTiers.registerTierUpgrades([
    { item: 'create:water_wheel', tier: 'advanced' },
    { item: 'create:large_water_wheel', tier: 'advanced', defaultRecipe: false },
    { item: 'create:large_water_wheel', tier: 'elite', defaultRecipe: false }
])

console.log(
    'Create Tiers: registered ' + CreateTiers.getAllTiers().size()
    + ' tiers; upgrade variants registered after tier definitions'
)
