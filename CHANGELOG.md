# Changelog

## 0.2.4

### Added

- Support tier calibration on every Create `KineticBlockEntity`-backed component across Forge 1.20.1 and NeoForge 1.21.1 without cloning upstream machine classes.
- Sneak-use a tiered shaft to apply its tier to an ordinary Create kinetic component; use the same tier again to clear it. The shaft is reusable.
- Persist attached tiers in block-entity NBT and rebuild the kinetic connection when calibration changes.
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

### Changed

- Clarify that tier definitions must be registered during startup (for example with KubeJS `startup_scripts`). Runtime datapacks cannot register new tier blocks after Minecraft freezes registries.
- Clarify Max SU semantics: the lowest tier Max SU is the hard cap for the connected Create kinetic network.
- Build Forge 1.20.1 and NeoForge 1.21.1 in parallel on pull requests while running the shared unit suite through NeoForge, then run a focused NeoForge GameTest suite; releases remain push-to-main only.

### Tests

- Added regression coverage for tier registry invariants, freeze behavior, valid/invalid atomic batches, KubeJS defaults and exact numeric parsing, and NeoForge 1.21 dynamic pack `getResource`/`listResources` behavior.
- Added NeoForge GameTests for receiver-scoped tiered/untiered RPM enforcement, lowest-tier connected-network Max SU/overspeed behavior, and generic tier attachment on an ordinary Create kinetic block entity.
