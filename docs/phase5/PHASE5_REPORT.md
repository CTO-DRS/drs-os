# Phase 5 — Final Report

Phase: 5 — No-device CI layer, publication hygiene & scheduler decision
Date: 2026-10-17 (sandbox UTC) · Trigger: owner directive "Complete
everything" · Method: every claim has a command, an exit code, or a
file behind it.

## 1. Executive summary

Phase 4 left three hardware-blocked gaps and one recommended Phase 5
scope (PHASE4_FINAL_REPORT §13). This phase executed every item of
that scope the environment supports and fixed the real defects found
along the way:

* **Robolectric no-device CI layer** — 11 new tests, all green, proving
  *rendered behavior* (not just compilation) for the highest-risk app
  claims: Arabic-default RTL, English switch, corrupted-store
  recovery, automation persistence through real SharedPreferences,
  privacy gating.
* **Two real defects fixed** in existing code/scripts (a `printf`
  bug that silently dropped every verify_all section header, and a
  genuine a11y/touch-target defect in DRSSettings row controls).
* **One Phase 4 audit claim corrected** (the "0 permissions" wording)
  with the evidence trail preserved in place.
* **ADR-0006**: the TIME_DAILY scheduler decision, with battery and
  permission costs documented and the zero-permission property kept.
* **Publication hygiene**: the repository now lives on GitHub
  (private mirror) and the sandbox commit identity has been replaced
  with the owner identity.

Overall status labels, unchanged and explicit: apps **BUILD VERIFIED —
RUNTIME NOT VERIFIED** (Robolectric is a JVM simulation, not device
runtime); AOSP integration **NOT STARTED**; project **NOT
production-ready**. verify_all.sh: **17 PASS / 0 FAIL / 2 BLOCKED**.

## 2. What was completed from the Phase 5 recommendations

| Recommended (§13 item) | Result this phase |
|---|---|
| 1. Runtime verification on KVM host | **BLOCKED** — no KVM, 2 vCPU, 3.9 GiB (unchanged); Robolectric layer (item 2) is the strongest feasible substitute |
| 2. Robolectric Compose smoke tests | **DONE** — 11 tests, all green (§3) |
| 3. DRS remote authorization + real remotes; replace commit identity | **PARTIAL** — project remote live on GitHub (private) + identity replaced + refs re-verified. The *component* repos of the AOSP local manifest (6 repos) remain deliberately NOT created: they have no consumer until a compliant `repo sync` host exists, and the mono-vs-multi-repo split needs its own decision (deferred with rationale) |
| 4. Platform integration spike | **BLOCKED** — requires a real AOSP checkout (≥64 GiB RAM, ~400 GB disk, KVM) |
| 5. Scheduler decision for TIME_DAILY | **DONE** — ADR-0006 (documented decision; no code change required or made) |

## 3. Test results — the Robolectric layer (11 tests, 0 failures)

Exact commands (run from the repo root with the Phase 4 toolchain):

```bash
source /path/to/drs_build_env.sh   # JAVA_HOME=17, ANDROID_HOME, PATH
gradle -p packages/apps/DRSSettings   :app:testDebugUnitTest   # exit 0
gradle -p packages/apps/DRSAutomation :app:testDebugUnitTest   # exit 0
```

Result verification: JUnit XML reports inspected directly (not exit
codes alone) — `app/build/test-results/testDebugUnitTest/TEST-*.xml`.

**DRSSettings (8 tests):**

| Test | What it proves |
|---|---|
| MainActivityLocaleTest · freshLaunch_rendersArabicTitle_anchoredRight_inRtl | Arabic-default UI renders; title Text anchored in the right half (RTL geometry); activity locale config is `ar` |
| MainActivityLocaleTest · pickingEnglish_switchesStringsAndGeometryToLtr | Clicking the English row switches the app to English strings, `en` locale config, and left-anchored (LTR) geometry — through the REAL AppCompatDelegate pipeline with a real activity recreation |
| MainActivityCorruptedStoreTest · corruptedRealStore_recoversToArabicDefaults_andShowsBanner | Real SharedPreferences seeded (test-only Application) with a structurally invalid document → whole-document discard, Arabic defaults, and the recovery banner render |
| MainActivityFreshLaunchTest · automationMasterToggle_persistsThroughRealStore_endToEnd | Toggling the automation master switch persists `automation_master_enabled=true` through the settings-core codec into REAL SharedPreferences, and the UI reflects it |
| MainActivityFreshLaunchTest · privacySection_whenSecurityCenterNotInstalled_showsMissingNotice | With the Security Center package absent (Robolectric fixture), the section shows the honest "not installed" notice — no dead button |
| SettingsStoreRobolectricTest · freshEmptyStore_loadsArabicDefaults | Fresh install → Arabic defaults on the real storage path |
| SettingsStoreRobolectricTest · commitWriteIsDurable_andRoundTripsThroughRealPreferences | Save → new repository instance → identical settings; commit() durability honored |
| SettingsStoreRobolectricTest · structurallyCorruptedDocument_isDiscardedAndFlagged_neverPartiallySalvaged | Corruption → `RESTORED_FROM_CORRUPTED` + defaults; no partial salvage |

**DRSAutomation (3 tests):**

| Test | What it proves |
|---|---|
| SharedPreferencesStoreRobolectricTest · write_reportsDurability_andSurvivesAcrossStoreInstances | commit() write reports true and is durable across store instances (real SharedPreferences) |
| SharedPreferencesStoreRobolectricTest · rules_roundTripThroughRealSharedPreferences_sortedAndUncorrupted | Rule encode → real storage → decode round-trip; ids sorted; uncorrupted |
| SharedPreferencesStoreRobolectricTest · corruptedRulesDocument_yieldsEmptyRuleSet_andCorruptionFlag | Corrupted document → empty rule set + corruption flag (never partial salvage) |

**Layer accounting (cumulative, unique tests):** core JVM suites 138
(25+79+18+16), automation Python-reference 25, DRSAutomation app unit
tests 12 (9 Jupiter + 3 Robolectric), DRSSettings app unit tests 8
(Robolectric). Total **183 unique, 0 failures**. DRSSecurityCenter has
no app-local tests (honest NOT RUN at app level; its logic is covered
by security-core's 18 JVM tests).

Honest scope notes, stated everywhere it matters:

* Robolectric tests run on a **JVM-simulated Android**, not a device.
  They strengthen but do not replace device runtime verification.
* The locale/Compose classes deliberately target **SDK 29**: on
  SDK ≥ 33 Robolectric's `LocaleManager` shadow accepts
  `setApplicationLocales` but never applies it to the running
  configuration, so the pipeline cannot be observed there. On a real
  API 33+ device the same call delegates to the platform
  LocaleManager. This limitation is recorded, not hidden.

## 4. Defects found and fixed this phase

| ID | Defect | Fix | Evidence |
|---|---|---|---|
| F-1 | DRSSettings radio/switch rows: only the small control was clickable; the label was a dead touch zone (a11y/touch-target defect, Workstream-E class) | Row-level `selectable`/`toggleable` semantics with `Role.RadioButton`/`Role.Switch`; inner control onClick nulled | MainActivity.kt (commented); exercised by the toggle + language tests above |
| F-2 | `verify_all.sh` `printf '--- %s\n'` treated dash-leading formats as options → every section header died with `printf: --: invalid option` on stderr (17 occurrences per run) | `printf --` with guard comment | verify_all.sh; clean output in the Phase 5 verify_all run |
| F-3 | Phase 4 audit wording "0 uses-permission entries in each" APK | Correction note in place in docs/phase4/SECURITY_AUDIT.md §1; regression-guard guidance corrected (`android.permission.` is the meaningful key) | see §5 |

False starts recorded honestly (compile errors during test authoring,
fixed immediately): `SemanticsNode.layoutDirection` does not exist in
Compose 1.7.6 (replaced with behavioral geometry assertions — a
stronger check); `fetchSemanticsNode`/`fetchSemanticsNodes` are
members, not top-level extensions (bogus imports removed); a stale
test-results directory briefly mis-reported a deleted class (cleared;
results always re-read from fresh XML).

## 5. Security & privacy findings

* **S-5 (CORRECTION, INFO):** all three debug APKs contain the
  merger-injected custom permission
  `<applicationId>.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`
  (protectionLevel `signature`, same package, not exported). It
  existed in the Phase 4 binaries too (DRSAutomation and
  DRSSecurityCenter APK hashes are byte-identical to Phase 4; only
  DRSSettings changed due to F-1). Platform permission surface is
  still **zero `android.permission.*`** entries — re-verified by
  `aapt2 dump badging` this phase.
* `ui-test-manifest` was evaluated as the createComposeRule fix and
  **not adopted** (the manifest merged correctly but Robolectric's
  launch path still failed; the tests were redesigned around the real
  MainActivity instead). No test-only activity remains in any
  manifest; test code lives only in src/test.
* No new production dependencies: all four new artifacts are
  test-scope only (provenance table in
  docs/build/dependency-provenance.md, Phase 5 section).
* Static scan posture unchanged: secrets scanners (incl. `ghp_` +
  36-hex) live in 3 validators; zero hits in the tree.

## 6. AOSP integration progress

Unchanged and accurately **NOT STARTED** (no code was integrated, no
fake remote activated, no revision invented). What this phase added:
ADR-0006 fixes the host-scheduling approach for the eventual
integration, and the Robolectric layer gives future Soong-shim work a
no-device regression net. Component-remote creation remains deferred
(rationale in phase-plan.md, Phase 5 section).

## 7. Artifacts and hashes (after the F-1 source change)

| APK (debug) | SHA-256 | vs Phase 4 |
|---|---|---|
| DRSSettings | `154fc96cd7bf6509d90ce3cc87698fa77ea423424ac92ef418bac14ddedff0a2` | CHANGED (23,767,128 B) — a11y fix F-1 |
| DRSAutomation | `e326622050a8987a886e531fe26bb6b93387c034abe609a9ab2de4a587f477ae` | unchanged (byte-identical) |
| DRSSecurityCenter | `f8412fb80be817b01939b45438b76b06464b605e6bcb82e82e655b338aa90972` | unchanged (byte-identical) |

Debug-signed only (CN=Android Debug); not distributable. An APK being
present still does not prove it installs or launches correctly.

## 8. Blockers (unchanged, hardware/decision-gated)

1. **Device runtime verification** — KVM host required (exact commands
   in docs/phase4/TEST_RESULTS.md §4 and verify_all.sh).
2. **Instrumented/connected tests** — need a device AND authoring.
3. **Platform integration** — compliant AOSP host.
4. **Component repo split** — decision deferred with rationale; only
   blocks future `repo sync` work, nothing current.

## 9. Exact reproduction steps

```bash
# toolchain: docs/phase4/TOOLCHAIN_REPORT.md §7 (JDK 17, Gradle 8.9, SDK 35)
# env:       source the build-env script (JAVA_HOME/ANDROID_HOME/PATH)
bash build/scripts/verify_all.sh                    # 17 PASS / 0 FAIL / 2 BLOCKED
gradle -p packages/apps/DRSSettings   :app:testDebugUnitTest
gradle -p packages/apps/DRSAutomation :app:testDebugUnitTest
bash build/scripts/inventory_artifacts.sh           # APK hashes above
```

## 10. Recommended next phase

Checkout & first boot on a compliant host (phase-plan.md, "Next
phase"): host checklist → component-remote decision → manifest
finalization + check_manifests.sh in production mode → branch
re-verification → Cuttlefish target confirmation from the tree.
The standing rule applies: no automatic phase transition.
