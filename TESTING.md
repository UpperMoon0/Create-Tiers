# Create Tiers verification

Create Tiers uses layered verification. Cheap contract checks fail first; Minecraft
boots are reserved for behavior that cannot be certified by source or JVM tests.
A pull request is mergeable only when the exact final PR head has both required
gates green.

## Layers

| Layer | What it proves | What it does not prove |
| --- | --- | --- |
| Harness contracts | Supported targets, CI matrix generation, required scenario declarations, exact-head receipt validation | Minecraft behavior |
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

tools/runtime_verification.py owns the two-target CI matrix and the scenario list
written into exact-head .pass receipts. Missing, extra, malformed, or stale
receipts fail the required runtime gate. Each runtime run also writes
build/runtime-evidence/<target>/result.json and process.log.

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
with Required runtime verification. release.yml is push/dispatch-only and reuses
the same core and runtime workflows before packaging and publishing.

The merge rule is simple: Required core verification and Required runtime
verification must both be green for the exact final PR head. A green result from
an older commit is not acceptable evidence.
