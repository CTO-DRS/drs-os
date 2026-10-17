# Phase 4 — Known Limitations

Every limitation is stated with its consequence and its removal path.
Nothing here is hidden behind a PASS label elsewhere.

> **PHASE 5 UPDATE (2026-10-17):** the Robolectric no-device CI layer
> (11 tests, docs/phase5/PHASE5_REPORT.md) narrows but does NOT close
> the runtime gap below — Robolectric is a JVM simulation, not device
> runtime. Device/emulator items remain BLOCKED as stated. New
> Robolectric-specific limitations are listed in the Phase 5 report
> §5 (LocaleManager shadow on SDK 33+, JVM-only graphics).

## 1. Runtime verification gap (the defining limitation)

The three APKs are **BUILD VERIFIED — RUNTIME NOT VERIFIED**. No launch,
no navigation, no RTL rendering, no screenshot exists, because the
sandbox has no KVM (x86_64 emulator images require it), 2 vCPU, and
3.9 GiB RAM. An APK that builds can still crash on launch; nothing in
this phase claims otherwise. Removal: KVM-capable host + the exact
commands in TEST_RESULTS.md §4.

## 2. Instrumented test suites do not exist yet

`connectedDebugAndroidTest` source sets were not authored in Phase 4
(authoring them without ever running them would produce untested test
code). The AndroidTest plan exists (T-16..T-20). Removal: Phase 5, on
a host that can actually execute them.

## 3. Robolectric not attempted

Deliberately NOT RUN (TEST_RESULTS.md §5): adding a Robolectric
dependency mid-phase without review would expand the dependency set
(android-all runtime downloads) beyond the phase's reviewed scope.
First Phase 5 candidate.

## 4. Environment-only tuning is not committed

`org.gradle.jvmargs=-Xmx1536m` lives in the sandbox's
`GRADLE_USER_HOME`, not in the repo (repo keeps `-Xmx2048m`). A machine
with <4 GiB RAM must apply the same override or OOMs are possible on
first build. Documented in TOOLCHAIN_REPORT.md §4.

## 5. No Gradle wrapper (deliberate)

The repository is text-only by design (static checks enforce it); the
Gradle wrapper ships a binary jar (~60 KB) and would violate that
policy. Builders install Gradle 8.9 directly (TOOLCHAIN_REPORT.md §7).
If the wrapper is ever wanted, it needs an explicit policy decision.

## 6. Scheduler gap in DRSAutomation

`TIME_DAILY` triggers exist in the engine model but cannot be created
from the UI, and nothing runs in the background — foreground-only by
design until a scheduler backend (WorkManager/JobScheduler/AlarmManager)
is chosen against real platform behavior. This is an honest-scope
decision, not an omission.

## 7. Debug-signed artifacts only

All three APKs are debug-signed (apksigner evidence). They must never
be distributed, installed outside a test device, or presented as
releases. Production signing requires the key ceremony that
intentionally does not exist yet.

## 8. Sandbox commit identity

Git commits in this sandbox use the preset identity
`Z User <z@container>`. Before any publication, history must be
rewritten or the project re-imported under the project's real identity.

## 9. Phase 3 test-count record corrected

The Phase 3 report's "188 core tests" double-counted 25 foundation
tests (classpath-scan bug, defect D-9). True unique count: 138. The
discrepancy is documented in TEST_RESULTS.md §3, not silently corrected.

## 10. Arabic-first UI verified at resource level only

Resource compilation, ar/en parity, RTL attributes, and localeConfig
are enforced by checks and now proven to build into real APKs; actual
rendered RTL behavior, large fonts, screen readers, and narrow screens
remain unverified until a device/emulator exists (TEST_RESULTS.md §6).

## 11. External prerequisites still outstanding

- Compliant AOSP host (≥64 GiB RAM, ~400 GB disk, KVM) — for Phase 1's
  original build goal, still untouched.
- DRS-owned Git remote(s) — requires user authorization; until then no
  active manifest may reference any DRS remote.
- User authorization for any Step beyond the current phase.
