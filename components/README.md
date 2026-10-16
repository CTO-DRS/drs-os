# DRS OS Components (Phase 3)

Pure-Kotlin modules with zero Android and zero AI dependencies. They
carry the project's domain logic (automation engine semantics, security
checklist evaluation, settings policy) and are **compiled and
unit-tested on any JVM** — which is exactly how Phase 3 verified them
in a sandbox with no Android SDK (ADR-0005).

## Module map

| Module | Package | Purpose | Phase 3 status |
|---|---|---|---|
| `foundation/` | `com.drs.os.foundation` | deterministic KvStore line codec + strict MiniJson parser/writer | **COMPILED & TESTED** (25/25) |
| `engine-core/` | `com.drs.os.engine` (+ `engine.host`) | Kotlin implementation of docs/automation/rule-engine-spec.md: model, validation, evaluation, host runtime (dedup, counters, audit, execution coordination) | **COMPILED & TESTED** (104/104, incl. 34 differential vs the Python reference) |
| `security-core/` | `com.drs.os.security` | four-state checklist model, v1 check definitions, presentation mapping | **COMPILED & TESTED** (18/18) |
| `settings-core/` | `com.drs.os.settings` | validated, corruption-tolerant settings persistence | **COMPILED & TESTED** (41/41) |

## Rules for these modules

1. **No Android imports.** Platform binding happens in the app modules
   (`packages/apps/*`), behind small interfaces defined here
   (`SettingsByteStore`, `AutomationKeyValueStore`, `CheckDataProvider`,
   `ActionExecutor`).
2. **No network, no AI, no crypto** — none of these modules reads or
   writes anything beyond what its host hands it.
3. **Semantics are pinned.** engine-core mirrors
   `tools/automation-reference/rule_engine.py` field-for-field; the
   differential vectors (`tools/automation-reference/test_vectors.json`)
   fail the build if the two implementations diverge.
4. Build/test with `build/scripts/run_core_tests.sh` (kotlinc 2.0.21 +
   JUnit Platform 1.10.2; honest BLOCKED output if the toolchain is
   absent — never a fake PASS).
5. Every file carries the Apache-2.0 SPDX header; Kotlin naming,
   formatting, and docs follow the repository's .editorconfig.
