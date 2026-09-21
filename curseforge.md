## About

**Create Tiers** is a customizable extension for the [Create mod](https://www.curseforge.com/minecraft/mc-mods/create) that adds configurable progression tiers to Create's kinetic system.

Define your own **Max RPM**, **Max Stress Units (SU)**, colors, and progression, then let Create Tiers generate the matching transmission and control components automatically. It is designed for modpacks that want deeper Create progression without tying the pack to a specific technology tree.

Create Tiers was inspired by the tiered kinetic progression found in [Greate](https://github.com/GreateBeyondTheHorizon/Greate), while keeping the system standalone and pack-author controlled.

## Key Features

* **Dynamic Tier Registration** — Register as many tiers as your pack needs during startup.
* **Custom RPM and Stress Limits** — Give every tier its own Max RPM and Max SU. The lowest participating tier's Max SU acts as the connected network limit.
* **Custom Colors** — Configure shaft and cogwheel colors independently for each tier.
* **Generated Create Components** — Each tier automatically receives its own shafts, cogwheels, gearboxes, clutch, gearshift, chain drives, Rotation Speed Controller, supported encased variants, and other transmission components.
* **Tier Existing Create Machines** — Pack authors can authorize tiered variants of eligible existing Create kinetic blocks with `registerTierUpgrade`, without replacing their normal Create implementation.
* **Create Compatibility** — Tiered shafts work through belt pulley and Steam Engine shaft transformations while preserving their tier and source identity.
* **Automatic Resource Generation** — Models, blockstates, translations, mining tags, loot, and other required resources are generated at runtime.
* **Optional Jade Integration** — When Jade is installed, tiered components can show their tier, Max RPM, and Max SU directly in the tooltip.
* **Minimal Core Dependencies** — Create is required. KubeJS is the supported scripting route for pack configuration; Jade is optional.

## Supported Versions

Current tested baselines:

* **Minecraft 1.20.1** — Forge 47.x — Create 6.0.8
* **Minecraft 1.21.1** — NeoForge 21.1.x — Create 6.0.11

## KubeJS Integration

Tier definitions belong in `kubejs/startup_scripts`. Register tiers before registering upgrade variants.

```javascript
// kubejs/startup_scripts/create_tiers.js

CreateTiers.registerTiers([
    {
        name: 'crude',
        maxRPM: 128,
        maxSU: 512,
        shaftColor: 0x707572,
        cogwheelColor: 0x6B4E2C,
        displayName: 'Crude'
    },
    {
        name: 'basic',
        maxRPM: 256,
        maxSU: 2048,
        shaftColor: 0x936C3D,
        cogwheelColor: 0xBCBCBC,
        displayName: 'Basic'
    },
    {
        name: 'refined',
        maxRPM: 512,
        maxSU: 8192,
        shaftColor: 0xA995BB,
        cogwheelColor: 0x49EAD6,
        displayName: 'Refined'
    }
])

// Optional: allow an existing Create kinetic block to carry a tier.
CreateTiers.registerTierUpgrade('create:large_water_wheel', 'refined')
```

Changing tier or upgrade registrations requires a **full client/server restart**. The same startup definitions should be shipped to both sides of a modpack.

By default, a registered tier upgrade can receive a simple fallback recipe using the base item plus the matching tiered shaft. Pack authors can disable that recipe and define their own progression with KubeJS recipes instead.

## For Modpack Authors

Create Tiers is intended to be progression-friendly without forcing a specific pack design:

* Use only the generated tiered transmission family.
* Add tier limits to selected existing Create machines.
* Build strict Basic → Advanced → Elite style progression with custom recipes.
* Use completely custom tier names, limits, colors, and balancing.
* Keep ordinary untiered Create components at Create's normal configured limits.

## Credits

Thanks to [@MoonScenty](https://github.com/MoonScenty) for creating [CreateTiersEngineCompat](https://github.com/MoonScenty/CreateTiersEngineCompat), sharing the tiered-shaft belt/Steam-Engine compatibility work, and permitting that work to be integrated or adapted into Create Tiers. The final implementation in Create Tiers is native and independently maintained.
