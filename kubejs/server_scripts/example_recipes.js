// Create Tiers - KubeJS server recipe example
//
// Put custom progression recipes in kubejs/server_scripts.
// The item+tier pair must already be authorized from startup_scripts with
// registerTierUpgrade(..., false) or registerTierUpgrades(... defaultRecipe: false).

ServerEvents.recipes(event => {
    const advancedWheel = CreateTiers.tieredItem('create:large_water_wheel', 'advanced')

    event.shaped(advancedWheel, [
        'ABA',
        'BCB',
        'ABA'
    ], {
        A: 'minecraft:diamond',
        B: 'create:precision_mechanism',
        C: 'create:large_water_wheel'
    })

    // tieredItem(...) creates a fresh output stack. It does not automatically
    // copy arbitrary NBT/components from the C input. If your recipe must
    // preserve input data, implement that copy explicitly in the custom recipe logic.
})
