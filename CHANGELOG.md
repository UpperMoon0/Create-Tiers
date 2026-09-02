# Changelog

## 0.2.4

### Fixed

- Preserve Create's configured RPM limit for untiered kinetic components in mixed tiered networks.
- Keep per-tier RPM limits scoped to tiered receiving components instead of globally raising vanilla Create limits.
- Reject duplicate tier IDs, numeric levels, generated names, invalid limits, and invalid RGB colors during startup.
- Generate pickaxe mining tags and loot for tiered gearboxes.
- Use Minecraft 1.21.1 data-pack format 48 and the 1.21 singular `tags/block` / `loot_table` resource paths on NeoForge.
- Show tier RPM/SU tooltips on normal and vertical tiered gearboxes.
- Show registered encased kinetic variants in the Create Tiers creative tab.
- Add the documented KubeJS registration overloads and descriptive validation for malformed batch tier definitions.
- Correct GitHub issue tracker metadata.

### Changed

- Clarify that tier definitions must be registered during startup (for example with KubeJS `startup_scripts`). Runtime datapacks cannot register new tier blocks after Minecraft freezes registries.
- Clarify Max SU semantics: the lowest tier Max SU is the hard cap for the connected Create kinetic network.
- Run both Forge 1.20.1 and NeoForge 1.21.1 builds/tests on pull requests; releases remain push-to-main only.

### Tests

- Added regression coverage for untiered-vs-tiered RPM policy, tier registry invariants, and NeoForge 1.21 gearbox data paths.
