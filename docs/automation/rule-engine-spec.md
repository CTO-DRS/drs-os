# DRS OS Deterministic Automation — Rule Engine Specification

Status: **specification + testable reference model (Phase 2)**. The
shipping engine will be a platform-integrated Kotlin component; the
**semantics** are fixed here and pinned by the Python reference model in
`tools/automation-reference/` with unit tests in `tests/automation/` that
run without AOSP. The reference model is a specification-by-example, not
product code.

## 1. Non-negotiable properties

1. **No AI/ML** of any kind — pure deterministic evaluation of explicit
   rules (ADR-0004). No generative behavior, no cloud inference, no
   undisclosed usage collection.
2. **No bypass** of Android background-execution restrictions: triggers
   ride platform schedulers (AlarmManager/JobScheduler semantics) and the
   engine itself executes nothing.
3. **Closed action vocabulary.** Only actions in the reviewed registry
   can be requested. Unknown actions fail validation — the engine cannot
   be talked into something new.
4. **Security-sensitive actions require explicit user consent** recorded
   on the rule; without it, validation rejects the rule outright.
5. **The engine executes nothing.** It returns an *ActionRequest* plus an
   audit record; a separate host layer decides execution under normal
   platform permissions. This separation is the auditable boundary.
6. Every rule carries a **user-visible explanation**; every evaluation
   produces an **audit record** with the decision reason; rules can be
   **disabled** and the audit shows that too.

## 2. Rule schema

| Field | Type | Constraint |
|---|---|---|
| id | string | unique, non-empty |
| name | string | non-empty, user-facing |
| enabled | bool | disabled rules never fire |
| trigger | object | one of the trigger kinds below |
| conditions | list | zero or more condition objects (all must hold) |
| action_id | string | MUST exist in the action registry |
| requires_unlock | bool (default true) | when true, fires only with device unlocked |
| max_runs_per_day | int (default 10, ≥1) | hard ceiling per day per rule |
| explanation | string | non-empty; shown in UI and audit |
| user_consented_actions | list of action ids | required to contain action_id iff that action is security-sensitive |

### Trigger kinds

| Kind | Params | Semantics |
|---|---|---|
| `TIME_DAILY` | `time` "HH:MM" (24h) | edge-triggered: host scheduler evaluates at that minute; dedup is the host's duty (contract below) |
| `BATTERY_BELOW` | `threshold` 1–99 | level-triggered: true while battery < threshold; host debounces; `max_runs_per_day` bounds repetition |
| `POWER_CONNECTED` | `state` bool | level-triggered on charging-state matches |

### Condition kinds

| Kind | Params | Semantics |
|---|---|---|
| `TIME_WINDOW` | `start`, `end` "HH:MM" | true when now in [start, end); **may span midnight** (start > end is legal) |
| `DAY_OF_WEEK` | `days` subset of 0–6 | ISO weekday, 0=Monday … 6=Sunday |

### Built-in action registry (v1, all reversible, none security-sensitive)

`set_dnd_on`, `set_dnd_off`, `set_theme_dark`, `set_theme_light`,
`enable_battery_saver`, `disable_battery_saver`.

The registry is closed; extending it is a reviewed change that also
extends the reference model and tests. Security-sensitive actions (if any
are ever proposed — each needs an ADR) are admitted only through the
consent field, and the reference model tests that path.

## 3. Evaluation contract (deterministic)

`evaluate(rule, context) → Decision` where context = (now_minutes 0–1439,
weekday 0–6, battery 0–100, charging bool, device_unlocked bool,
runs_today int). Pure function: same inputs → same Decision, always.

Decision order (first failure wins, reason recorded):

1. `rule-disabled` — rule.enabled false.
2. `trigger-not-active` — trigger condition false for this context.
3. `condition-failed` — a condition is false.
4. `requires-unlock` — device locked and requires_unlock true.
5. `max-runs-reached` — runs_today ≥ max_runs_per_day.
6. `fired` — ActionRequest issued.

## 4. Host-layer contract (future platform integration)

- Calls `evaluate` at the right moments (scheduler for TIME_DAILY with
  dedup: at most one evaluation per rule per calendar minute; debounce
  for level triggers).
- Maintains per-rule per-day run counters and feeds them as `runs_today`.
- Executes ActionRequests only through normal public APIs with the
  permissions the user already granted; never with elevated identity.
- Persists the audit record with its own timestamp.
- Provides the disable/cancel affordance in UI (sets `enabled=false`;
  audit records the disable event).

## 5. Audit record (mandatory fields)

`rule_id, decision(reason), action_id (nullable), explanation,
context_snapshot (time, weekday, battery, charging, unlocked),
timestamp (host-supplied)`. Audit is append-only at the host layer; no
network write path exists (data-flow.md F6).

## 6. Examples (also encoded as tests)

- **Quiet hours:** TIME_DAILY 22:00 → set_dnd_on; conditions
  [TIME_WINDOW 21:00–23:30]; max 1/day.
- **Battery saver:** BATTERY_BELOW 20 → enable_battery_saver; conditions
  [DAY_OF_WEEK mon–fri]; max 4/day.
- **Morning theme:** TIME_DAILY 07:30 → set_theme_light; requires_unlock
  true (theme switch waits for unlock — no surprise screen changes).

## 7. Verification

Reference model: `bash tests/automation/run_automation_tests.sh` (skips
with reason if python3 is absent). Product integration (T-19) and
adversarial-input testing (T-20, run *before* integration) are tracked in
docs/security/test-plan.md.
