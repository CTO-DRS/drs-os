# tools/automation-reference — Rule-Engine Reference Model

**Status: specification-by-example (Phase 2). This is NOT product code.**

`rule_engine.py` pins the deterministic automation semantics defined in
`docs/automation/rule-engine-spec.md` in executable form so they can be
tested on any host with Python 3.10+, **without AOSP**. The shipping
platform engine will be Kotlin, integrated per
`docs/ui/native-system-surfaces.md` (surface 9/6 touchpoints), and must
replay the same test vectors (docs/security/test-plan.md T-19).

## Guarantees

- Pure stdlib; deterministic pure functions; no I/O; no network.
- No AI/ML — closed action registry, explicit rules only (ADR-0004).
- Executes nothing: evaluation returns an ActionRequest + audit data;
  a future host layer executes under normal platform permissions.

## Tests

```bash
bash tests/automation/run_automation_tests.sh   # 25 tests; skips if no python3
```

Negative-control property demonstrated during Phase 2: the tests caught
three real test-side defects (condition tests evaluated at times when the
time trigger was inactive) — fixed by isolating condition semantics with
a level-triggered rule, and an ordering test was added. Tests constrain
the engine; they are not decoration.

## Boundary

Product integration (foreground service, AlarmManager scheduling, Settings
UI, audit persistence) is **future work** and tracked in the spec §4 host
contract and the security test plan. Nothing here runs on a device.
