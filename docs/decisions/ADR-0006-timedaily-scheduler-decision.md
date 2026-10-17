# ADR-0006: TIME_DAILY stays engine-owned; the host scheduler is only a wake-up trigger

Status: Accepted (Phase 5, 2026-10-17)
Supersedes: none
Related: ADR-0004 (no-AI policy), ADR-0005 (core module strategy),
docs/automation/rule-engine-spec.md, docs/phase4/PHASE4_FINAL_REPORT.md
(item 5 of the recommended Phase 5 scope)

## Context

The engine's `Trigger.TimeDaily("HH:MM")` needs a host mechanism to be
woken up at the configured time. The Phase 4 task book required the
scheduler decision to be made against the real platform behaviors and
their battery/permission costs, documented per rule — before any
platform integration is attempted. Three platform mechanisms were
considered:

1. **`AlarmManager`** (exact) — fires at the wall-clock time, but exact
   alarms on API 31+ require `SCHEDULE_EXACT_ALARM` (user-granted,
   revocable, Play-policy-sensitive) and alarm firing can still be
   deferred in deep doze. A permission-gated path contradicts the
   Phase 4 proof that the apps run with **zero platform permissions**.
2. **`JobScheduler`** / **`WorkManager`** — inexact by design; the OS
   batches jobs to save battery. A TIME_DAILY rule evaluated at 09:07
   instead of 09:00 is acceptable ONLY if the user-visible semantics
   say "around the requested time", and the engine's own level checks
   (window conditions, `max_runs_per_day`) still hold.
3. **Engine-owned polling loop** — a persistent foreground service
   polling every minute: exact, permission-free, but battery-hostile
   and out of proportion for daily rules.

Additional invariant from the engine spec (§1): the engine is
**clock-free and executes nothing** — it evaluates a context handed to
it and returns decisions plus an audit record. Whatever wakes the app,
the *decision* is always re-made by the engine, never replayed.

## Decision

1. The engine remains the ONLY source of truth for whether a rule
   fires. The host scheduler is a **wake-up trigger**, nothing more.
2. The v1 host binding (when platform integration begins) uses
   **`WorkManager` periodic/one-off work aligned to the next local
   occurrence of each enabled TIME_DAILY trigger**:
   * no new platform permission is required (`SCHEDULE_EXACT_ALARM` is
     deliberately NOT used);
   * firing is inexact by OS batching — the user-visible rule
     explanation strings must therefore say "يُقيَّم حوالي الوقت
     المحدد" / "evaluated around the requested time", not "exactly
     at";
   * every wake-up hands the engine a fresh `EventContext` (clock
     re-read, `runs_today` re-loaded from persisted state); the engine
     decides, including honoring time-window conditions that may
     legitimately be past by the time the job runs;
   * missed wake-ups (doze, killed process) are NOT replayed: a
     TIME_DAILY rule skipped by the OS is re-armed for the next day,
     and the audit records the evaluation that actually happened. This
     matches the engine's anti-duplicate-run counter semantics.
3. `AlarmManager` exact alarms remain a documented FUTURE option gated
   on an explicit product decision to accept the
   `SCHEDULE_EXACT_ALARM` permission — which would also change the
   "zero permissions" property the security posture relies on.
4. The decision applies to platform integration only; the standalone
   apps of Phases 3–5 contain no scheduler binding, and the engine
   itself is unchanged (no code change from this ADR).

## Consequences

Positive: keeps the zero-platform-permission property intact; keeps
the engine clock-free and deterministic; battery cost is the OS's
minimum (batched jobs); the user-facing wording can be honest about
the inexactness instead of promising wall-clock precision the OS will
not deliver.

Negative: TIME_DAILY rules can fire minutes late or, in doze, be
skipped for a day; this is an accepted product trade-off, documented
here rather than hidden. If exactness ever becomes a requirement, the
engine needs no change — only the host binding and this ADR's
decision item 3.

Verification status: the deterministic parts (window conditions,
`max_runs_per_day`, dedup, audit) are engine-core unit-tested
(Phase 3/4/5 suites); the WorkManager binding is NOT implemented in
this sandbox (no platform integration) and its real-world behavior is
**BLOCKED on a compliant AOSP host** — recorded in
docs/phase5/PHASE5_REPORT.md.
