# Create Tiers

A dynamic, customizable tier system for Create kinetic components.

## What it does

Create Tiers adds tiered shafts, cogwheels, encased variants, and gearboxes with configurable mechanical limits and colors.

Each tier defines:

- **Max RPM** — the highest speed that tiered component may receive.
- **Max SU** — a hard stress-cap for the connected Create kinetic network. If multiple tiered components are present, the lowest Max SU wins.
- **Shaft and cogwheel colors** — used by the generated models and kinetic rendering.

Untiered Create components always keep Create's normal configured maximum RPM. A high-speed tiered network therefore does **not** make vanilla Create shafts, cogs, gearboxes, or other ordinary kinetic components high-RPM-safe.

## Registering tiers

Tiers must exist before Minecraft freezes the block/item registries. Register them from **KubeJS `startup_scripts`** or from another mod during initialization.

Runtime/server datapacks cannot create new tier block registry entries and are therefore not a supported tier-registration mechanism.

### KubeJS

```javascript
// kubejs/startup_scripts/create_tiers.js
CreateTiers.registerTier('basic', 1, 256, 1024)
CreateTiers.registerTier('advanced', 2, 512, 4096, 0xC88A45)
CreateTiers.registerTier('elite', 3, 1024, 16384, 0x00FFBB, 0x55FF55, 'Elite')
```

Batch form:

```javascript
CreateTiers.registerTiers([
  {
    name: 'basic',
    level: 1,
    maxRPM: 256,
    maxSU: 1024,
    shaftColor: 0xAAAAAA,
    cogwheelColor: 0x777777,
    displayName: 'Basic'
  },
  {
    name: 'advanced',
    level: 2,
    maxRPM: 512,
    maxSU: 4096,
    shaftColor: 0xB87333,
    displayName: 'Advanced'
  }
])
```

Batch registration is atomic: if any definition in the batch is invalid or conflicts with another tier, none of that batch is registered. Numeric fields must be whole 32-bit integers; fractional or overflowing values are rejected instead of truncated.

Tier IDs, numeric levels, and generated tier names must be unique. Generated names must also be valid Minecraft resource paths. Invalid definitions fail during startup with a descriptive error instead of silently overwriting another tier.

`registerCustomTier(namespace, name, ...)` may be used when another integration needs a namespaced lookup ID. Generated Create Tiers component IDs still use `name`, so generated names remain globally unique across namespaces.

## Generated resources

Create Tiers generates models, blockstates, translations, mining tags, and block loot for registered components at runtime. Minecraft 1.20.1 Forge and 1.21.1 NeoForge use their version-correct resource/data-pack layouts.

## License

MIT
