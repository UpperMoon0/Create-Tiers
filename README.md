# Create Tiers

A dynamic, customizable tier system for Create kinetic components.

## What it does

Create Tiers provides native generated tiered shafts, cogwheels, encased variants, and gearboxes, and can attach those same tier limits to Create's existing kinetic machines without replacing their upstream block classes.

Each tier defines:

- **Max RPM** — the highest speed that tiered component may receive.
- **Max SU** — a hard stress-cap for the connected Create kinetic network. If multiple tiered components are present, the lowest Max SU wins.
- **Shaft and cogwheel colors** — used by the generated models and kinetic rendering.

Untiered Create components always keep Create's normal configured maximum RPM. A high-speed tiered network therefore does **not** make ordinary Create components high-RPM-safe unless they are explicitly tier-upgraded.

## Tiering Create components

Create's kinetic system is much broader than shafts and cogwheels. Create Tiers therefore supports ordinary `KineticBlockEntity`-backed Create components as **registered tier-upgrade variants** instead of cloning upstream machine classes.

Tier-upgrade registration is separate from recipe registration. A registration says that a specific item is allowed to exist with a specific tier:

```javascript
// kubejs/startup_scripts/create_tiers.js
CreateTiers.registerTierUpgrade('create:large_water_wheel', 'advanced')
```

By default, Create Tiers generates a simple shapeless fallback recipe that consumes the base item plus the matching tiered shaft. For example, the registration above consumes a `create:large_water_wheel` and a `createtiers:shaft_advanced`. The tiered shaft is consumed; it is not a reusable upgrade key.

Disable the built-in recipe when the pack wants to own progression completely:

```javascript
CreateTiers.registerTierUpgrade('create:large_water_wheel', 'advanced', false)
```

Batch registration is also available:

```javascript
CreateTiers.registerTierUpgrades([
  { item: 'create:water_wheel', tier: 'advanced' },
  { item: 'create:large_water_wheel', tier: 'advanced', defaultRecipe: false },
  { item: 'create:large_water_wheel', tier: 'elite', defaultRecipe: false }
])
```

Like `registerTiers`, the batch is atomic: an invalid tier or duplicate item+tier pair rejects the entire batch. Bare tier names such as `advanced` resolve to `createtiers:advanced`; integrations may also use a full namespaced tier ID.

### Custom recipes

`registerTierUpgrade` does **not** lock an upgrade to the crafting table. `CreateTiers.tieredItem(item, tier)` returns the registered tiered `ItemStack`, so KubeJS can use it as the output of any recipe type that accepts an item stack.

For a normal crafting recipe:

```javascript
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
})
```

The same output can be used with Create Mechanical Crafting or a third-party machine recipe:

```javascript
ServerEvents.recipes(event => {
  const advancedWheel = CreateTiers.tieredItem('create:large_water_wheel', 'advanced')

  event.recipes.create.mechanical_crafting(advancedWheel, [
    ' AAA ',
    'ABBBA',
    'ABCBA',
    'ABBBA',
    ' AAA '
  ], {
    A: 'create:brass_sheet',
    B: 'create:precision_mechanism',
    C: 'create:large_water_wheel'
  })

  // Other mods can use the same advancedWheel output in their own recipe builders.
})
```

`tieredItem` refuses item+tier pairs that were never registered. This keeps the legal variant list deterministic while leaving the actual progression path entirely under the pack's control.

The resulting stack keeps the original Create item identity and stores the selected tier in vanilla block-entity item data. Placing it transfers the tier into the normal Create `KineticBlockEntity`; breaking that upgraded machine preserves the tier on the matching dropped block item. A later recipe may upgrade that same base item to another registered tier while preserving its other item data.

Create kinetics that genuinely have **no normal item form** cannot be recipe outputs. For those in-world-only components, such as belt segments, a tiered shaft remains a fallback interaction. This fallback is deliberately blocked for normal item-backed machines: a shaft cannot apply or change the tier of a water wheel, press, mixer, motor, or other ordinary block item. Using the matching tiered shaft may still clear an existing attached tier.

This automatically covers Create kinetic families such as:

- transmission and control: clutches, gearshifts, encased chain drives, adjustable chain gearshifts, belts, chain conveyors, gantry shafts, sequenced gearshifts, flywheels, and rotation speed controllers;
- processing and logistics: encased fans, turntables, millstones, crushing wheels, mechanical presses/mixers, weighted ejectors, pumps, hose pulleys, drills, saws, deployers, mechanical crafters, and mechanical arms;
- contraption motion: mechanical pistons, mechanical/clockwork bearings, rope pulleys, and elevator pulleys;
- generators: creative motors, water wheels, large water wheels, hand cranks, valve handles, steam engines, and windmill bearings;
- any future Create component that participates through `KineticBlockEntity`, unless Create Tiers deliberately exempts it.

Speedometers and stressometers are deliberately not tier-upgrade targets: they are observation devices and retain Create Tiers' unlimited RPM observation exemption.

Attached tiers are stored in the target block entity's NBT and move through normal Create block-entity serialization. Recipe output, placement, and matching block drops preserve the same registered tier ID. When tier state changes, Create Tiers rebuilds the component's kinetic connection so the new RPM/SU policy is enforced immediately. Native Create Tiers blocks keep their intrinsic tier and cannot be double-tiered through tier attachment.

Native tiered shafts also participate in Create's shaft-only interactions. They can be used as belt pulleys and as steam-engine shafts. When Create temporarily replaces a tiered shaft with a belt pulley or powered steam-engine shaft, Create Tiers carries the intrinsic tier through that replacement and restores the same tiered shaft when the temporary state is removed, so RPM/SU limits are never bypassed by the conversion.

Upgraded components also inherit the tier's custom colors without replacing Create's casing textures. Create cogwheel blocks, including encased cogwheels, use `cogwheelColor`; other tintable rotating/mechanical parts use `shaftColor`. Create Tiers applies the tint through both Flywheel and fallback block-entity rendering, preserves Create's red/green overstress feedback, and adds a small tier-colored top-edge accent to upgraded machines whose specialized renderer does not expose a suitable rotating part. Create's kinetic debugger takes visual priority while it is active.

### Jade

Jade support is optional. When Jade is installed, Create Tiers adds tier information to Jade's existing Create tooltip instead of replacing Create's own kinetic information. Tiered and upgraded kinetic components show:

- the effective tier display name;
- whether the tier is **Intrinsic** (a native Create Tiers block) or **Upgraded** (attached to a normal Create block);
- the tier's **Max RPM**;
- the tier's **Max SU**.

The Jade payload is generated from the server-side block entity, so multiplayer clients see the authoritative tier rather than relying on locally inferred state. Untiered Create blocks do not receive extra Create Tiers Jade lines.

## Compatibility credit

Thanks to **MoonScenty** and [CreateTiersEngineCompat](https://github.com/MoonScenty/CreateTiersEngineCompat) for independently identifying and documenting the tiered-shaft belt/steam-engine compatibility gap that led to the native fix in Create Tiers.

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

Create Tiers generates models, blockstates, translations, mining tags, and block loot for registered native tier components at runtime. Minecraft 1.20.1 Forge and 1.21.1 NeoForge use their version-correct resource/data-pack layouts.

## License

MIT
