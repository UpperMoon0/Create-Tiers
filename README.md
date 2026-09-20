# Create Tiers

A dynamic, customizable tier system for Create transmission components and kinetic machines.

## Supported versions and dependencies

| Minecraft | Loader | Create baseline | Java | KubeJS scripting baseline |
| --- | --- | --- | --- | --- |
| 1.20.1 | Forge 47.x | Create 6.0.8 | Java 17 | KubeJS `2001.6.5-build.16` |
| 1.21.1 | NeoForge 21.1.x | Create 6.0.11 | Java 21 | KubeJS `2101.7.2-build.363` |

Create is required. **KubeJS is the supported script configuration route** for pack authors; another mod may instead register tiers during initialization through the Java API. **Jade is optional** and only adds tier information to its existing Create tooltip.

The versions above are the source/test baselines used by this repository. Do not assume a different Create minor version has identical internal behavior just because the loader accepts it.

## Pack-author quick start

Create Tiers has two startup registries, and their order matters:

```javascript
// kubejs/startup_scripts/create_tiers.js

// 1. Register every tier first.
CreateTiers.registerTiers([
  { name: 'basic', maxRPM: 256, maxSU: 1024, shaftColor: 0xAAAAAA, cogwheelColor: 0x777777, displayName: 'Basic' },
  { name: 'advanced', maxRPM: 512, maxSU: 4096, shaftColor: 0xB87333, cogwheelColor: 0xC88A45, displayName: 'Advanced' },
  { name: 'elite', maxRPM: 1024, maxSU: 16384, shaftColor: 0x00FFBB, cogwheelColor: 0x55FF55, displayName: 'Elite' }
])

// 2. Only after the tiers exist, authorize item+tier upgrade variants.
CreateTiers.registerTierUpgrades([
  { item: 'create:large_water_wheel', tier: 'advanced' },
  { item: 'create:large_water_wheel', tier: 'elite', defaultRecipe: false }
])
```

`registerTier(...)` / `registerTiers(...)` **must run before** `registerTierUpgrade(...)` / `registerTierUpgrades(...)`. Upgrade registration immediately rejects an unknown tier.

Tier and upgrade declarations belong in **`kubejs/startup_scripts`** because they affect registries and generated resources. Changing them requires a **full game/server restart**; `/reload` is not sufficient. Ship the same startup declarations to the modpack client and dedicated server so both sides build the same tier registry and generated block/item set.

If `defaultRecipe: false` is used, put the actual crafting or machine recipes in **`kubejs/server_scripts`**. Recipe scripts are separate from startup registration; they do not create tiers or authorize new item+tier variants.

The checked-in examples mirror this layout:

- `kubejs/startup_scripts/example.js` â€” tier definitions first, then upgrade authorization.
- `kubejs/server_scripts/example_recipes.js` â€” custom recipes using already-registered `tieredItem(...)` outputs.

## What it does

Create Tiers provides a generated native tier family for Create's transmission/control infrastructure and can attach the same limits to existing item-backed Create kinetic blocks without replacing their upstream block classes.

Each tier defines **Max RPM**, **Max SU**, and shaft/cogwheel colors. Max RPM is the highest speed that tiered component may receive. Max SU is a hard stress cap for the connected Create kinetic network; if several tiered components are present, the lowest Max SU wins.

Untiered Create components keep Create's normal configured maximum RPM. A high-speed tiered network therefore does **not** make ordinary Create components high-RPM-safe unless they are explicitly tier-upgraded.

## Registering tiers

Tiers must exist before Minecraft freezes the block/item registries. Register them in KubeJS `startup_scripts` or from another mod during initialization. Runtime datapacks cannot create new tier block registry entries.

Each `registerTier` automatically creates the canonical native transmission/control family for that tier: shafts and powered-shaft runtime state, cogwheels, large cogwheels, standard Create encasings discovered from the running Create version, gearbox/vertical gearbox, clutch, gearshift, encased chain drive, adjustable chain gearshift, Rotation Speed Controller, and the metal-girder encased shaft.

These are intrinsic tier components. Functional machines and generators are **not** generated automatically; eligible ordinary Create blocks must be explicitly authorized with `registerTierUpgrade`.

Example direct registration:

```javascript
// kubejs/startup_scripts/create_tiers.js
CreateTiers.registerTier('basic', 256, 1024)
CreateTiers.registerTierStyled('advanced', 512, 4096, 0xC88A45, 'Advanced')
CreateTiers.registerTierStyled('elite', 1024, 16384, 0x00FFBB, 0x55FF55, 'Elite')
```

Batch registration is atomic:

```javascript
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
    maxSU: 4096,
    shaftColor: 0xB87333,
    displayName: 'Advanced'
  }
])
```

Tier progression is capability-derived; there is no numeric level. Tiers are ordered by `maxRPM`, then `maxSU`. Higher RPM may not come with a lower Max SU than another registered tier, because that would make the progression incomparable. Equal-capability tiers are allowed.

Tier IDs and generated names must be unique. Generated names must be valid lowercase Minecraft resource paths. `maxRPM` and `maxSU` must be positive whole 32-bit integers. Colors must be 24-bit RGB values from `0x000000` through `0xFFFFFF`. Batch numeric parsing rejects fractional and overflowing values instead of truncating them. In `registerTiers`, a supplied `displayName` must be a non-empty string; custom namespaces must also be valid Minecraft namespaces.

`registerCustomTier(namespace, name, ...)` creates a custom lookup ID for integrations. Generated Create Tiers block/item IDs still use `name`, so generated names remain globally unique across namespaces.

## KubeJS API reference

| API | Purpose / constraints |
| --- | --- |
| `registerTier(name, maxRPM, maxSU)` | Register a `createtiers:<name>` tier with white default colors. |
| `registerTierStyled(name, maxRPM, maxSU, color, displayName)` | Register one tier using the same RGB color for shaft and cogwheel. |
| `registerTierStyled(name, maxRPM, maxSU, shaftColor, cogwheelColor, displayName)` | Register one tier with separate 24-bit RGB colors. |
| `registerCustomTier(namespace, name, maxRPM, maxSU)` | Register a custom namespaced tier ID. Generated component names still use `name`. |
| `registerCustomTierStyled(namespace, name, maxRPM, maxSU, color, displayName)` | Custom-ID styled form with one shared color. |
| `registerCustomTierStyled(namespace, name, maxRPM, maxSU, shaftColor, cogwheelColor, displayName)` | Custom-ID styled form with separate colors. |
| `registerTiers([{ name, maxRPM, maxSU, shaftColor?, cogwheelColor?, displayName? }, ...])` | Atomic batch tier registration. `shaftColor` defaults to white; `cogwheelColor` defaults to the shaft color; `displayName` defaults to `name`. |
| `registerTierUpgrade(item, tier)` | Authorize one item+tier pair and enable the default fallback recipe. The tier must already exist. |
| `registerTierUpgrade(item, tier, defaultRecipe)` | Same, with explicit fallback-recipe control. |
| `registerTierUpgrades([{ item, tier, defaultRecipe? }, ...])` | Atomic batch authorization; `defaultRecipe` defaults to `true`. All referenced tiers must already exist. |
| `tieredItem(item, tier)` | Return a **fresh** tiered output stack for an already-registered item+tier pair. Intended for `server_scripts` recipe outputs. |
| `getTier(name)` | Return a tier from the default `createtiers` namespace. |
| `getAllTiers()` | Return all registered tiers in capability order. |
| `tierExists(name)` | Check the default `createtiers` namespace. |

Bare tier names in upgrade APIs, such as `'advanced'`, resolve to `createtiers:advanced`; a full namespaced tier ID may also be used. Use full namespaced item IDs such as `create:large_water_wheel`.

`getTier(name)` and `tierExists(name)` are convenience helpers for the default `createtiers` namespace. `getAllTiers()` includes custom-namespaced tiers as well.

## Tiering ordinary Create components

`registerTierUpgrade` authorizes an existing Create block item to carry a selected tier while keeping the original Create block and block-entity implementation.

A direct upgrade target must satisfy all of these runtime checks:

1. the item exists and is a `BlockItem`;
2. the block creates a block entity;
3. that block entity is a Create `KineticBlockEntity`;
4. the block is not a speedometer/stressometer gauge;
5. the block is not already an intrinsic Create Tiers tier block.

Rather than relying on a static "supported blocks" list, treat those five checks as authoritative for the installed Create version. `create:large_water_wheel` is a verified example of a direct target; other item-backed Create blocks are accepted only when their live implementation passes the same validation.

### Direct upgrade targets vs compatibility states

Not every Create state that participates in tier behavior is a valid `registerTierUpgrade` target.

| Category | Examples | How it is handled |
| --- | --- | --- |
| Direct upgrade target | `create:large_water_wheel`, other item-backed `KineticBlockEntity` blocks | Register the item+tier pair with `registerTierUpgrade`. |
| Intrinsic generated block | Create Tiers shaft/cogwheel/gearbox/clutch/etc. | Created by `registerTier`; do not register it again as an attached upgrade. |
| Temporary transformation state | belt pulley segments, powered shafts created for Steam Engines | Tier/source data is inherited only from a legitimate source block during the Create transformation. These states are not independently upgradeable. |
| Compatibility mechanism | Steam Engine block interacting with its shaft | The Steam Engine itself is **not** a valid upgrade target because its block entity is not a `KineticBlockEntity`; Create Tiers supports the shaft/powered-shaft transformation around it. |
| Observation device | speedometer, stressometer | Explicitly excluded so Create Tiers' unlimited-RPM observation behavior remains intact. |
Belts should therefore not appear in a pack's `registerTierUpgrade` list: belt segments do not have a normal block item to authorize. Their tier behavior exists only while they replace/contain a supported shaft source.

## Upgrade recipes and progression

Upgrade authorization and recipe choice are separate. Registering a legal item+tier pair answers **what may exist**; recipe scripts decide **how players obtain it**.

With the default behavior:

```javascript
CreateTiers.registerTierUpgrade('create:large_water_wheel', 'advanced')
```

Create Tiers generates a shapeless fallback recipe using the **base item plus the target tier's shaft**. The tiered shaft is consumed.

The default fallback recipe does **not** enforce a Basic -> Advanced -> Elite chain. If both Advanced and Elite are registered with default recipes, the player can craft either target directly from the base item using that target tier's shaft.

For strict pack progression, disable fallback recipes and define the intended path yourself:

```javascript
// kubejs/startup_scripts/create_tiers.js
CreateTiers.registerTierUpgrade('create:large_water_wheel', 'advanced', false)
CreateTiers.registerTierUpgrade('create:large_water_wheel', 'elite', false)
```

Then put the custom recipes in `kubejs/server_scripts`.

### Custom recipe output

`tieredItem(item, tier)` can be used as the output of shaped crafting, Create Mechanical Crafting, or another KubeJS-compatible recipe type:

```javascript
// kubejs/server_scripts/create_tiers_recipes.js
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

`tieredItem` rejects item+tier pairs that were never registered.

### Item-data preservation

The built-in Create Tiers fallback recipe finds the actual base input stack, copies it, sets the output count to one, and then adds/replaces the Create Tiers tier field. Existing item NBT/components on that input are therefore preserved unless the tier field itself is being replaced.

`CreateTiers.tieredItem(item, tier)` is different: it starts from a **new `ItemStack` of the requested item** and applies the tier. A normal custom recipe whose output is `tieredItem(...)` therefore does **not** automatically copy arbitrary NBT/components, custom names, or other data from the recipe input.

If a custom progression recipe must preserve arbitrary input data, its recipe logic must explicitly copy/transfer the desired data from the input stack. `tieredItem(...)` alone only creates the registered tiered output.

## Runtime tier preservation

The resulting upgraded stack keeps the original Create item identity and stores the selected tier in block-entity item data/components. Normal placement transfers it into the Create `KineticBlockEntity`; breaking that upgraded block preserves the registered tier on the matching dropped item.

Create's `PlacementOffset` helpers are tier-aware. Recipe-produced upgraded shafts retain their tier through normal shaft-extension placement and direct shaft-on-Steam-Engine placement. Only the authorized Create Tiers tier payload is replayed after helper placement; arbitrary held-item block-entity data is not copied by that helper path.

Itemless states cannot mint tiers. Belt pulleys and powered shafts may carry a tier only while temporarily replacing a legitimate source block. Persisted source provenance is validated against the live replacement state, preventing unrelated kinetic block entities from claiming a shaft source to borrow its tier.

Native tiered shafts also participate in Create's shaft-only interactions, including shaft-on-middle-belt pulley creation and Steam Engine shaft conversion. Registered upgraded vanilla shafts receive equivalent tier-preserving handling. Wrenching a tiered pulley back into a middle belt clears tier/source state and returns the corresponding intrinsic or upgraded shaft item.

## Rendering and UI

Upgraded components inherit the tier's custom colors. In-world kinetic rendering keeps Create's casing/base materials intact while coloring the mechanical parts exposed by Create's renderer. Recipe-produced upgraded item models use general `shaftColor` tinting where appropriate, while mixed-material models retain selective shaft/cogwheel tint channels.

Tiered Rotation Speed Controllers keep Create's dedicated large-cog coupling and use a compact signed numeric input from `-Max RPM` through `+Max RPM` (excluding zero), avoiding Create's wide fixed value board at high tier limits.

## Jade

Jade integration is **optional**. When Jade is installed, Create Tiers appends authoritative server-side tier information to Jade's existing Create tooltip:

- effective tier display name;
- Max RPM;
- Max SU.

Untiered Create blocks receive no extra Create Tiers Jade lines.

## Generated resources

Create Tiers generates models, blockstates, translations, mining tags, and block loot for registered native tier components at runtime. Forge 1.20.1 and NeoForge 1.21.1 use their version-correct resource/data-pack layouts.

Standard Create encasings are discovered from Create's own encasing registry rather than maintained as an andesite/brass-only list. Special non-registry encasings such as the metal-girder shaft are handled explicitly when their interaction/loot semantics differ.

## Compatibility credit

Thanks to **MoonScenty** and [CreateTiersEngineCompat](https://github.com/MoonScenty/CreateTiersEngineCompat) for independently identifying and documenting the tiered-shaft belt/Steam-Engine compatibility gap that led to the native fix in Create Tiers.

## License

MIT
