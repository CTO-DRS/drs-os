# tests/automation — Deterministic Automation Tests

**Status: ACTIVE for the reference model.** Runs the rule-engine
reference-model unit tests (25 tests) on any host with python3 ≥ 3.10 —
no AOSP, no network, no root.

```bash
bash tests/automation/run_automation_tests.sh   # exit 0 pass / 1 fail / 2 skipped
```

## What is covered

- Rule validation: required fields, closed action registry, unknown
  trigger/action rejection, time formats, bounds.
- Security-sensitive action consent gate (including a registered
  test-only sensitive action).
- Trigger semantics: exact-minute time triggers; level triggers
  (battery/charging) with threshold-equality edge.
- Condition semantics: time windows (including midnight-spanning),
  weekday filters, multi-condition conjunction, evaluation order
  (trigger precedes conditions).
- Constraints: requires-unlock, max_runs_per_day, disabled rules.
- Audit record completeness; ActionRequest issued only on `fired`;
  determinism (repeated evaluation is identical).

## What is NOT covered (honestly)

- Platform integration (scheduler binding, Android APIs, persistence):
  future T-19 in docs/security/test-plan.md.
- Adversarial/fuzz inputs: planned T-20, to run **before** product
  integration.
- Any device behavior — nothing here runs on Android yet.
