# ADR-0005: JVM-testable Kotlin core modules separate from Android UI

Status: Accepted (Phase 3, 2026-10-16)
Supersedes: none
Related: ADR-0002 (repository strategy), ADR-0004 (no-AI policy),
docs/automation/rule-engine-spec.md

## Context

Phase 2 produced a Python *reference model* of the automation rule
engine — specification-by-example, explicitly NOT product code — and
the security/settings designs were documentation-only. Phase 3 had to
turn those into real, testable implementation, but the available
environment has **no Android SDK** (measured, docs/phase3/PHASE3_BASELINE.md):
application modules cannot be compiled or run here.

## Decision

1. The domain logic lives in pure-Kotlin modules
   (`components/foundation|engine-core|security-core|settings-core`)
   that import neither Android nor any third-party library.
2. Platform dependencies are expressed as narrow interfaces owned by
   the cores (`CheckDataProvider`, `ActionExecutor`, `SettingsByteStore`,
   `AutomationKeyValueStore`); the Android apps implement them.
3. The cores are compiled with the standalone Kotlin compiler (2.0.21)
   and tested with JUnit Platform Console (1.10.2) on the existing JRE —
   both free downloads with recorded provenance (tools/README.md).
4. The Kotlin engine is kept semantically identical to the Phase 2
   Python reference by a committed, deterministic differential-vector
   file; regeneration must be byte-identical (checked by
   `run_core_tests.sh`).
5. Application modules (Compose, ar-first resources, Gradle) are
   source-complete and build-configured, and are labeled **SOURCE
   IMPLEMENTED — BUILD NOT VERIFIED** until a host with the Android SDK
   executes `gradlew :app:assembleDebug`.

## Consequences

Positive: real, executed verification of the security-critical logic in
the sandbox; one semantic source of truth (the vectors) binds Python
spec and Kotlin product code together; app modules stay thin.

Negative (accepted): the pure-source subsets of the apps are duplicated
test targets until Gradle runs; the Android UI layer remains unverified
here — a real build host with ~4 GB free RAM and the SDK is the
prerequisite (tracked in docs/phase3/KNOWN_LIMITATIONS.md).

Prohibited by this ADR: claiming the app UI is "tested" because the
cores are; moving Android imports into the cores; skipping the
differential check when the engine changes.
