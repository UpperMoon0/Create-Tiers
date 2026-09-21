# Create Tiers verification

Create Tiers uses layered verification. Cheap contract checks fail first; Minecraft
boots are reserved for behavior that cannot be certified by source or JVM tests.
A pull request is mergeable only when the exact final PR head has both required
gates green.

## Layers

| Layer | What it proves | What it does not prove |
| --- | --- | --- |
| Harness contracts | Supported targets, CI matrix generation, runtime scenario-evidence parsing, and exact-head receipt validation | Minecraft behavior itself |
| JVM tests | Loader-independent tier policy and render math on both targets, plus loader-owned API/resource/KubeJS regressions | Real kinetic graph behavior |
| Forge + NeoForge GameTests | Real Create kinetic propagation, overspeed semantics, stress caps, tier-upgrade/persistence behavior, native relay/control registration, adjustable Create components, and native tiered-shaft belt/steam-engine interoperability | Final rendered pixels, every third-party Create addon |
| Narrow integration smoke | Reserved for lightweight Jade/color checks and future interactions that require a real client | General modpack compatibility |
| Release gate | The same core and runtime contract completed for the exact release commit before packaging | Behavior outside the declared contract |

No framebuffer/pixel harness is used today. If a genuine rendering regression
appears later, add a narrow visual test for that claim rather than broad graphical
infrastructure.

## Required runtime scenarios

Both forge-1.20.1 and neoforge-1.21.1 must cover tiered-to-tiered propagation,
high-tier to ordinary Create receivers, overspeed rejection, lowest-tier Max SU,
attached-tier apply/clear and NBT persistence, rebuilt-network behavior after tier changes,
registered tier-upgrade item data surviving placement and drops,
Rotation Speed Controller range, Creative Motor range, default native clutch/gearshift/chain-drive/controller registration, tiered-shaft belt creation/teardown, and tiered-shaft steam-engine powered-shaft conversion/recovery.

tools/runtime_verification.py owns the two-target CI matrix and required scenario set.
Each successful GameTest emits a `CREATE_TIERS_SCENARIO_PASS:<scenario>` marker only
after its assertions complete. The runtime harness parses those markers and fails
even after a zero Gradle exit if a required scenario is missing or undeclared
scenario evidence appears. Receipt creation then re-reads that exact run's
`result.json` and refuses to issue a pass receipt unless the observed scenario set
and commit match. Missing, extra, malformed, or stale receipts still fail the final
required runtime gate. Each runtime run also writes
`build/runtime-evidence/<target>/result.json` and `process.log`.

## Root commands

On Linux/macOS:

~~~sh
./gradlew harnessTest
./gradlew testAllTargets
./gradlew buildAllTargets
./gradlew gameTestAllTargets
./gradlew verify
~~~

On on-prem-1 Windows, run Gradle with JDK 21 (Forge compilation still uses its
Java 17 toolchain):

~~~powershell
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-21'
$env:Path = "$env:JAVA_HOME\bin;$env:Path"
.\gradlew.bat harnessTest
.\gradlew.bat testAllTargets
.\gradlew.bat buildAllTargets
py -3 tools\runtime_verification.py --target forge-1.20.1
py -3 tools\runtime_verification.py --target neoforge-1.21.1
~~~

The direct loader commands remain available:

~~~powershell
.\gradlew.bat :forge-1.20.1:runGameTestServer
.\gradlew.bat :neoforge-1.21.1:runGameTestServer
~~~

## CI

pr-tests.yml runs the harness self-tests first, then both loader JVM tests and
builds. runtime-tests.yml runs a two-cell fail-fast:false matrix, preserves
logs/evidence on failures, writes exact-head receipts on success, and finishes
with Required runtime verification. release.yml is version-driven: pushes that touch
gradle.properties first compare mod_version with the previous main revision, and only an
actual version bump may reuse the same core/runtime gates before packaging and publishing
to CurseForge and GitHub Releases.

The merge rule is simple: Required core verification and Required runtime
verification must both be green for the exact final PR head. A green result from
an older commit is not acceptable evidence.


### Create-native transformation tier preservation

Runtime GameTests cover ordinary attached tiers across Create transformations that replace block-entity types:

- tier-upgraded vanilla shaft -> belt pulley -> the same vanilla shaft identity;
- tier-upgraded vanilla shaft -> steam powered shaft -> vanilla shaft;
- tier-upgraded vanilla shaft/cogwheel -> standard Create encasing -> the same vanilla decased identity.

Belt pulleys persist the exact source block ID in block-entity NBT. The intrinsic-shaft
belt regression explicitly serializes a pulley block entity, recreates it from that
NBT, emulates legacy saved data that duplicated the intrinsic tier as an attached
tier, clears that legacy attached copy, and then tears the belt down. The pulley must
remain effectively tiered from source provenance and restore the original intrinsic
tiered shaft identity.

The native relay family also verifies that generated clutch, gearshift, chain-drive, adjustable chain gearshift, and rotation-speed-controller variants are both axe- and pickaxe-mineable, matching Create's `axeOrPickaxe()` registrations.

Progression-bypass GameTests start from **untiered** vanilla shafts and verify that neither a belt pulley nor a powered shaft can acquire a tier merely because the state has no item form. Teardown/recovery must return an untiered vanilla shaft. Forged-NBT regressions also verify that unrelated kinetic block entities cannot borrow a registered or intrinsic shaft through `CreateTiersReplacementSourceBlock`, with or without an attached-tier key.

Interaction-path GameTests execute Create's real helpers on both loaders. They verify that recipe-produced upgraded shafts retain their tier through normal shaft-extension `PlacementOffset` placement and direct shaft-on-Steam-Engine placement. They also exercise both registered upgraded and intrinsic tiered shaft items on an existing middle belt, then wrench the resulting pulley back to `MIDDLE` and assert that runtime state **and persisted NBT** contain no stale tier/source provenance while the correct shaft item is returned.

Client-side JVM tests execute the numeric Rotation Speed Controller input validation directly at positive/negative tier limits and invalid values. Loader-specific model tests transform real `BakedQuad` instances through `TierUpgradeTintedItemModel`, asserting that generic full-item tinting inserts channel 0 while pre-existing selective tint channels are preserved.


### Creative-tab upgrade entries

The Create Tiers creative tab exposes every registered `registerTierUpgrade(item, tier)` pair as a real tier-upgraded item stack using the same item data path as recipe outputs. Runtime GameTests verify the startup fixture's registered `create:shaft` upgrade appears with the expected tier.

Generated encased shaft, encased cogwheel, and encased large-cogwheel items are intentionally omitted from the mod creative tab. Resource-contract coverage prevents those generated encased item lists from being reintroduced there.


### Belt pulley lifecycle refunds

Runtime GameTests cover both registered-upgraded vanilla shafts and intrinsic tiered shafts through the remaining Create belt lifecycle paths:

- direct mining of a pulley must drop the exact source shaft item, not a plain `create:shaft`;
- BeltSlicer shortening must preserve endpoint tier/source provenance and refund the exact source shaft.

These scenarios run on Forge 1.20.1 and NeoForge 1.21.1 and are part of the required exact-head runtime evidence.
