# Changelog

## 0.2.4

### Added

- Support tier calibration on every Create `KineticBlockEntity`-backed component across Forge 1.20.1 and NeoForge 1.21.1 without cloning upstream machine classes.
- Add data-driven `createtiers:calibration` crafting recipes that output the original Create block item carrying the selected tier, so packs can price each machine/tier upgrade appropriately.
- Preserve calibrated tier data through vanilla item placement and matching block drops on both supported loaders.
- Persist attached tiers in block-entity NBT and rebuild the kinetic connection when calibration changes.
- Apply attached-tier custom colors to ordinary Create kinetics: dedicated cogwheels use `cogwheelColor`, other rotating/mechanical parts use `shaftColor`, and specialized machines receive a subtle tier-colored accent when their renderer has no suitable tintable part.
- Keep attached-tier visuals consistent across Flywheel and fallback block-entity rendering while preserving Create's overstress and kinetic-debugger feedback.
- Add optional Jade integration for intrinsic and calibrated tiers, showing the tier name, tier source, Max RPM, and Max SU from authoritative server data.
- Keep speedometers and stressometers intentionally exempt from calibration.

### Fixed

- Preserve Create's configured RPM limit for untiered kinetic components in mixed tiered networks.
- Keep per-tier RPM limits scoped to tiered receiving components instead of globally raising vanilla Create limits.
- Reject duplicate tier IDs, numeric levels, generated names, invalid limits, invalid generated resource paths, and invalid RGB colors during startup.
- Make KubeJS batch tier registration atomic so one invalid/conflicting entry cannot leave earlier entries partially registered.
- Reject fractional and overflowing KubeJS numeric fields instead of silently truncating or wrapping them to `int`.
- Generate pickaxe mining tags and loot for tiered gearboxes.
- Use Minecraft 1.21.1 data-pack format 48 and the 1.21 singular `tags/block` / `loot_table` resource paths on NeoForge.
- Show tier RPM/SU tooltips on normal and vertical tiered gearboxes.
- Show registered encased kinetic variants in the Create Tiers creative tab.
- Add the documented KubeJS registration overloads and descriptive validation for malformed batch tier definitions.
- Correct GitHub issue tracker metadata.
- Make native tiered shafts work as Create belt pulleys and steam-engine shafts while preserving the tier through belt/powered-shaft replacement and restoring the same tiered shaft on teardown (fixes #2).
- Fix Jade provider configuration localization so the Forge 1.20.1 development client no longer crashes at the title screen, and ensure the shared language resource is packaged on NeoForge.
- Preserve Create's vanilla belt pulley body on tiered belts, remove the generic square accent from belt endpoints, and render the surviving source shaft separately in its tier color.

### Changed

- Replace universal reusable-shaft calibration with recipe-defined upgrade costs for normal item-backed machines; high-value generators and machines can no longer receive an effectively free tier upgrade from owning one shaft. Tiered shafts remain a fallback only for in-world kinetics with no normal item form.
- Clarify that tier definitions must be registered during startup (for example with KubeJS `startup_scripts`). Runtime datapacks cannot register new tier blocks after Minecraft freezes registries.
- Clarify Max SU semantics: the lowest tier Max SU is the hard cap for the connected Create kinetic network.
- Run shared/core verification plus required Forge 1.20.1 and NeoForge 1.21.1 GameTest matrices on pull requests, with exact-head runtime receipts gating the final result; releases remain push-to-main only.

### Tests

- Added regression coverage for tier registry invariants, freeze behavior, valid/invalid atomic batches, KubeJS defaults and exact numeric parsing, and NeoForge 1.21 dynamic pack `getResource`/`listResources` behavior.
- Added Forge and NeoForge GameTests for receiver-scoped tiered/untiered RPM enforcement, lowest-tier connected-network Max SU/overspeed behavior, generic tier attachment on ordinary Create kinetic block entities, calibrated recipe item placement/drop round-tripping, adjustable kinetic components, and native tiered-shaft belt/steam-engine interoperability.
- Compatibility investigation for #2 was informed by MoonScenty's CreateTiersEngineCompat report/reference project; the native implementation is maintained directly in Create Tiers.
