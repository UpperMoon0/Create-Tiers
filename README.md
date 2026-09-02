# Create Tiers

A dynamic, customizable tier system for Create kinetic components.

## What it does

Create Tiers provides native generated tiered shafts, cogwheels, encased variants, and gearboxes, and can attach those same tier limits to Create's existing kinetic machines without replacing their upstream block classes.

Each tier defines:

- **Max RPM** — the highest speed that tiered component may receive.
- **Max SU** — a hard stress-cap for the connected Create kinetic network. If multiple tiered components are present, the lowest Max SU wins.
- **Shaft and cogwheel colors** — used by the generated models and kinetic rendering.

Untiered Create components always keep Create's normal configured maximum RPM. A high-speed tiered network therefore does **not** make ordinary Create components high-RPM-safe unless they are explicitly tier-calibrated.

## Tiering Create components

Create's kinetic system is much broader than shafts and cogwheels. Create Tiers therefore supports every current `KineticBlockEntity`-backed Create component generically instead of copying dozens of upstream machine classes.

**Sneak-use a tiered shaft on a Create kinetic component** to calibrate that component to the shaft's tier. Sneak-use the same tier again to clear the attachment and restore ordinary Create limits. The shaft is a reusable calibration key; it is not consumed.

This automatically covers Create kinetic families such as:

- transmission and control: clutches, gearshifts, encased chain drives, adjustable chain gearshifts, belts, chain conveyors, gantry shafts, sequenced gearshifts, flywheels, and rotation speed controllers;
- processing and logistics: encased fans, turntables, millstones, crushing wheels, mechanical presses/mixers, weighted ejectors, pumps, hose pulleys, drills, saws, deployers, mechanical crafters, and mechanical arms;
- contraption motion: mechanical pistons, mechanical/clockwork bearings, rope pulleys, and elevator pulleys;
- generators: creative motors, water wheels, large water wheels, hand cranks, valve handles, steam engines, and windmill bearings;
- any future Create component that participates through `KineticBlockEntity`, unless Create Tiers deliberately exempts it.

Speedometers and stressometers are deliberately not calibratable: they are observation devices and retain Create Tiers' unlimited RPM observation exemption.

Attached tiers are stored in the target block entity's NBT and move with normal Create block-entity serialization. Changing or clearing a tier detaches and reattaches the component's kinetic connection so the new RPM/SU policy is enforced immediately. Native Create Tiers blocks keep their intrinsic tier and cannot be double-tiered through calibration.

Calibrated components also inherit the tier's custom colors without replacing Create's casing textures. Create cogwheel blocks, including encased cogwheels, use `cogwheelColor`; other tintable rotating/mechanical parts use `shaftColor`. Create Tiers applies the tint through both Flywheel and fallback block-entity rendering, preserves Create's red/green overstress feedback, and adds a small tier-colored top-edge accent to calibrated machines whose specialized renderer does not expose a suitable rotating part. Create's kinetic debugger takes visual priority while it is active.

### Jade

Jade support is optional. When Jade is installed, Create Tiers adds tier information to Jade's existing Create tooltip instead of replacing Create's own kinetic information. Tiered and calibrated kinetic components show:

- the effective tier display name;
- whether the tier is **Intrinsic** (a native Create Tiers block) or **Calibrated** (attached to a normal Create block);
- the tier's **Max RPM**;
- the tier's **Max SU**.

The Jade payload is generated from the server-side block entity, so multiplayer clients see the authoritative tier rather than relying on locally inferred state. Untiered Create blocks do not receive extra Create Tiers Jade lines.

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
