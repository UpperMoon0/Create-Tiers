# Changelog

## 0.2.4

### Added

- Support attached tier upgrades on every Create `KineticBlockEntity`-backed component across Forge 1.20.1 and NeoForge 1.21.1 without cloning upstream machine classes.
- Add startup `registerTierUpgrade` / `registerTierUpgrades` APIs that register legal item+tier variants independently from recipe choice.
- Add `CreateTiers.tieredItem(item, tier)` for KubeJS recipes, allowing the same registered output to be used by crafting tables, Create Mechanical Crafting, or third-party machine recipe types.
- Generate an optional default `createtiers:tier_upgrade` shapeless recipe for each registration; it consumes the base item plus the matching tiered shaft and can be disabled per registration.
- Preserve tier-upgrade item data through vanilla item placement and matching block drops on both supported loaders.
- Persist attached tiers in block-entity NBT and rebuild the kinetic connection when the applied tier changes.
- Apply attached-tier custom colors to ordinary Create kinetics: dedicated cogwheels use `cogwheelColor`, other rotating/mechanical parts use `shaftColor`, and specialized machines receive a subtle tier-colored accent when their renderer has no suitable tintable part.
- Keep attached-tier visuals consistent across Flywheel and fallback block-entity rendering while preserving Create's overstress and kinetic-debugger feedback.
- Add optional Jade integration for intrinsic and upgraded tiers, showing the tier name, tier source, Max RPM, and Max SU from authoritative server data.
- Keep speedometers and stressometers intentionally exempt from tier upgrades.
- Expand each registered tier's native default family with clutch, gearshift, encased chain drive, adjustable chain gearshift, rotation speed controller, and metal-girder encased shaft.
- Discover standard Create shaft/cogwheel encasing variants from the installed Create version instead of maintaining an andesite/brass-only registration list.
- Reuse Create's upstream block-entity implementations and client models for native relay/control variants while applying intrinsic tier RPM/SU and colors.

### Fixed

- Preserve Create's configured RPM limit for untiered kinetic components in mixed tiered networks.
- Keep per-tier RPM limits scoped to tiered receiving components instead of globally raising vanilla Create limits.
- Remove the redundant numeric tier level from the Java/KubeJS tier model; tier progression and deterministic ordering are now derived from Max RPM and Max SU. Styled direct registration uses `registerTierStyled` / `registerCustomTierStyled` so old level-based overloads cannot silently map their arguments onto the new API.
- Reject duplicate tier IDs, generated names, invalid limits, invalid generated resource paths, and invalid RGB colors during startup.
- Reject incomparable crossed capability definitions where higher Max RPM comes with lower Max SU; equal-capability tiers remain valid and batch registration stays atomic.
- Validate tier-upgrade targets in the registry-stable common-setup phase so unknown items, non-block items, non-kinetic blocks, gauges, and native Create Tiers components fail startup without forcing Minecraft registry bootstrap from KubeJS startup scripts.
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
- Preserve a tiered shaft through metal-girder encasing, wrench recovery, schematic requirements, and block loot instead of downgrading it to a vanilla shaft.
- Mirror Create's `safe_nbt` block tag for generated tiered Rotation Speed Controllers so schematic/configurable block-entity data keeps upstream behavior.

### Changed

- Replace universal reusable-shaft upgrading with registered item+tier variants. Packs can disable the default consumed-shaft recipe and define progression with any KubeJS recipe type; tiered shafts remain a fallback only for in-world kinetics with no normal item form.
- Clarify that tier definitions must be registered during startup (for example with KubeJS `startup_scripts`). Runtime datapacks cannot register new tier blocks after Minecraft freezes registries.
- Clarify Max SU semantics: the lowest tier Max SU is the hard cap for the connected Create kinetic network.
- Run shared/core verification plus required Forge 1.20.1 and NeoForge 1.21.1 GameTest matrices on pull requests, with exact-head runtime receipts gating the final result; releases remain push-to-main only.

### Tests

- Added regression coverage for tier registry invariants, freeze behavior, valid/invalid atomic batches, KubeJS defaults and exact numeric parsing, and NeoForge 1.21 dynamic pack `getResource`/`listResources` behavior.
- Added Forge and NeoForge GameTests for receiver-scoped tiered/untiered RPM enforcement, lowest-tier connected-network Max SU/overspeed behavior, generic tier attachment on ordinary Create kinetic block entities, tier-upgraded item placement/drop round-tripping, adjustable kinetic components, native relay/control default registration, and native tiered-shaft belt/steam-engine interoperability.
- Compatibility investigation for #2 was informed by MoonScenty's CreateTiersEngineCompat report/reference project; the native implementation is maintained directly in Create Tiers.

- Preserve attached tiers and source block identity across Create-native block-entity replacement paths, including vanilla shaft-to-belt round trips, steam powered-shaft round trips, and standard shaft/cog encasing/decasing. Belt pulleys persist their exact source block ID so calibrated vanilla shafts do not restore as intrinsic tiered shafts.
- Mirror Create's `axeOrPickaxe()` mining tags for generated cogwheel, gearbox, encased, clutch, gearshift, chain-drive, and speed-controller variants.
