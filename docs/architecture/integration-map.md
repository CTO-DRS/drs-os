# DRS OS Architecture Integration Map

Status: Phase 2. Classifies every component the project touches into four
mandated categories and states what can and cannot be verified in the
current environment. Companion to system-overview.md; integration
readiness is tracked in docs/build/integration-readiness.md.

## Categories

| Code | Category | Meaning |
|---|---|---|
| **U** | verified upstream AOSP component | Ships with the baseline branch; used unchanged; existence verified from official sources/Phase 0 manifest fetch |
| **D** | DRS-owned component existing in this repository | Present here today (docs, tooling, templates) |
| **P** | proposed DRS component, not yet implemented | Designed; no code exists anywhere |
| **H** | hardware/build-dependent component | Requires the real checkout, device, or build host to even exist |

## Component classification

| Component | Class | Location at integration | Notes |
|---|---|---|---|
| Linux kernel (Cuttlefish) | U / H | upstream prebuilt source | no DRS kernel work exists or is planned pre-device |
| Cuttlefish HALs & VIRTIO device stack | U / H | upstream | inherited wholesale |
| ART & core runtime | U | upstream | unchanged by policy |
| Java API framework & system services | U | upstream | DRS modifies via overlays only (ADR-0003) |
| SELinux policy (upstream) | U / H | upstream | enforcing at build; DRS additions gated (sepolicy README) |
| PermissionController (Mainline module) | U | upstream | reuse for permission UI/privacy hub |
| SafetyCenter (upstream API surface) | U | upstream | integration point for Security & Privacy Center — confirm exact availability on baseline at checkout |
| SystemUI (keyguard, shade, QS) | U baseline / P customization | upstream + DRS overlay repo | S4 spec: docs/ui/native-system-surfaces.md |
| Launcher3 (home, app drawer, Quickstep recents) | U baseline / P theming | upstream + DRS overlay repo | overlay-only stage 1 |
| Settings (packages/apps/Settings) | U baseline / P overlay | upstream + DRS overlay repo | no fork in stage 1 |
| DRS framework/SystemUI/Settings overlays | D (spec) / P (code) | `overlays` repo (manifest template mounts it) | resources only; no code |
| DRS Security & Privacy Center | P | `packages/apps/DRSSecurityCenter` | privileged app; whitelist-gated |
| DRS deterministic automation engine | P + D (reference model) | `packages/apps/DRSAutomation`; reference model in this repo (`tools/automation-reference/`) | semantics testable today; platform integration P |
| DRS Files | P | `packages/apps/DRSFiles` | native app |
| DRS device layer (product makefiles deriving Cuttlefish) | P / H | `device/drs` | requires real tree for inherited product names |
| DRS vendor config | P | `vendor/drs` | policy-only content initially |
| OTA updater surface (DRS) | P / H | new package + update_engine integration | design in update-integrity.md |
| Repo manifests (upstream) | U / H | android.googlesource.com/platform/manifest | branch verified Phase 0; SHA pinned at init |
| DRS local manifest | D (template) / P (production) | `manifests/` | production manifest cannot exist until remotes authorized — see integration-readiness.md |
| Validation tooling | D | `build/scripts`, `tests/` | active in this repo |
| Branding/design tokens | D | `branding/` | specs; asset binaries deferred with licensing plan |

## What cannot be verified without a full AOSP checkout (honest list)

1. Exact lunch-target string for the Cuttlefish product on the baseline
   revision (must be read from the tree).
2. Exact availability/signatures of SystemUI, SafetyCenter, and
   PermissionController integration points on `android-latest-release`
   (documented symbols are design-level and re-verified at checkout).
3. Semantic manifest validation against the live `default.xml`.
4. Any build, boot, SELinux-enforcement, or OTA behavior.
5. Whether upstream-referenced documentation matches tree-level reality
   (docs drift is possible; the tree is authoritative).

## Non-goals restated

No kernel, device-tree, or HAL code is created to make the tree look
complete. Hardware-dependent components stay documented-proposal until a
verified basis exists (checkout +, later, real hardware phase).
