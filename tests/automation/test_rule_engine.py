#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""Unit tests for the DRS OS automation rule-engine reference model.

Run directly (python3 tests/automation/test_rule_engine.py) or via
tests/automation/run_automation_tests.sh. Stdlib only; deterministic;
no network. These tests pin the semantics in
docs/automation/rule-engine-spec.md.
"""

from __future__ import annotations

import os
import sys
import unittest

sys.path.insert(0, os.path.join(os.path.dirname(__file__), "..", "..",
                                "tools", "automation-reference"))

import rule_engine as re_mod  # noqa: E402

evaluate = re_mod.evaluate
validate_rule = re_mod.validate_rule
EventContext = re_mod.EventContext


def ctx(**kw):
    base = dict(now_minutes=12 * 60, weekday=2, battery=80, charging=False,
                device_unlocked=True, runs_today=0)
    base.update(kw)
    return EventContext(**base)


def time_rule(**over):
    rule = {
        "id": "r1", "name": "Quiet hours", "enabled": True,
        "trigger": {"kind": "TIME_DAILY", "time": "22:00"},
        "conditions": [],
        "action_id": "set_dnd_on",
        "requires_unlock": True,
        "max_runs_per_day": 1,
        "explanation": "Turns on Do Not Disturb at 22:00 every day.",
        "user_consented_actions": [],
    }
    rule.update(over)
    return rule


def level_rule(**over):
    """Rule with an always-active level trigger (battery < 99), used to
    isolate CONDITION semantics from trigger timing."""
    rule = time_rule()
    rule["id"] = "r-level"
    rule["trigger"] = {"kind": "BATTERY_BELOW", "threshold": 99}
    rule["max_runs_per_day"] = 10
    rule.update(over)
    return rule


class ValidationTests(unittest.TestCase):
    def test_valid_rule_has_no_errors(self):
        self.assertEqual(validate_rule(time_rule()), [])

    def test_missing_required_fields(self):
        errs = " ".join(validate_rule({}))
        for frag in ("id:", "name:", "enabled:", "explanation:",
                     "trigger.", "action_id:"):
            self.assertIn(frag, errs)

    def test_unknown_trigger_kind_rejected(self):
        r = time_rule(trigger={"kind": "AI_SUGGEST", "time": "10:00"})
        self.assertIn("trigger.kind", " ".join(validate_rule(r)))

    def test_unknown_action_rejected(self):
        r = time_rule(action_id="launch_ai_assistant")
        self.assertIn("action_id", " ".join(validate_rule(r)))

    def test_bad_time_format_rejected(self):
        r = time_rule(trigger={"kind": "TIME_DAILY", "time": "24:61"})
        self.assertTrue(any("trigger.time" in e for e in validate_rule(r)))

    def test_battery_threshold_bounds(self):
        r = time_rule(trigger={"kind": "BATTERY_BELOW", "threshold": 0})
        self.assertTrue(any("threshold" in e for e in validate_rule(r)))
        r2 = time_rule(trigger={"kind": "BATTERY_BELOW", "threshold": 99})
        self.assertEqual(validate_rule(r2), [])


class ConsentTests(unittest.TestCase):
    def test_sensitive_action_requires_consent(self):
        re_mod.ACTION_REGISTRY["wipe_device_demo"] = True  # test-only action
        try:
            r = time_rule(action_id="wipe_device_demo")
            self.assertTrue(any("security-sensitive" in e
                                for e in validate_rule(r)))
            r["user_consented_actions"] = ["wipe_device_demo"]
            self.assertEqual(validate_rule(r), [])
        finally:
            del re_mod.ACTION_REGISTRY["wipe_device_demo"]


class TimeTriggerTests(unittest.TestCase):
    def test_fires_at_exact_minute(self):
        d = evaluate(time_rule(), ctx(now_minutes=22 * 60))
        self.assertTrue(d.fired)
        self.assertEqual(d.action_id, "set_dnd_on")
        self.assertEqual(d.reason, "fired")

    def test_not_fired_off_minute(self):
        d = evaluate(time_rule(), ctx(now_minutes=22 * 60 + 1))
        self.assertEqual(d.reason, "trigger-not-active")

    def test_daily_cap_blocks_second_fire(self):
        r = time_rule(max_runs_per_day=1)
        d = evaluate(r, ctx(now_minutes=22 * 60, runs_today=1))
        self.assertEqual(d.reason, "max-runs-reached")


class ConditionTests(unittest.TestCase):
    def test_window_holds(self):
        r = level_rule(conditions=[{"kind": "TIME_WINDOW",
                                    "start": "21:00", "end": "23:30"}])
        self.assertTrue(evaluate(r, ctx(now_minutes=22 * 60)).fired)

    def test_window_fails_outside(self):
        r = level_rule(conditions=[{"kind": "TIME_WINDOW",
                                    "start": "21:00", "end": "23:30"}])
        self.assertEqual(evaluate(r, ctx(now_minutes=20 * 60)).reason,
                         "condition-failed")

    def test_window_spans_midnight(self):
        r = level_rule(conditions=[{"kind": "TIME_WINDOW",
                                    "start": "22:00", "end": "06:00"}])
        self.assertTrue(evaluate(r, ctx(now_minutes=23 * 60)).fired)
        self.assertTrue(evaluate(r, ctx(now_minutes=5 * 60 + 59)).fired)
        self.assertEqual(evaluate(r, ctx(now_minutes=6 * 60)).reason,
                         "condition-failed")

    def test_day_of_week(self):
        r = level_rule(conditions=[{"kind": "DAY_OF_WEEK", "days": [0, 4]}])
        self.assertTrue(evaluate(r, ctx(weekday=0)).fired)
        self.assertEqual(evaluate(r, ctx(weekday=2)).reason,
                         "condition-failed")

    def test_all_conditions_must_hold(self):
        r = level_rule(conditions=[
            {"kind": "TIME_WINDOW", "start": "21:00", "end": "23:30"},
            {"kind": "DAY_OF_WEEK", "days": [0]},
        ])
        self.assertEqual(evaluate(r, ctx(now_minutes=22 * 60, weekday=2)).reason,
                         "condition-failed")

    def test_trigger_precedes_conditions(self):
        # With an inactive trigger, conditions are never consulted
        # (spec evaluation order) — first-failure-wins reason.
        r = time_rule(conditions=[{"kind": "TIME_WINDOW",
                                   "start": "21:00", "end": "23:30"}])
        self.assertEqual(evaluate(r, ctx(now_minutes=12 * 60)).reason,
                         "trigger-not-active")


class LevelTriggerTests(unittest.TestCase):
    def test_battery_below_fires(self):
        r = time_rule(trigger={"kind": "BATTERY_BELOW", "threshold": 20})
        self.assertTrue(evaluate(r, ctx(battery=19)).fired)

    def test_battery_equal_threshold_not_fired(self):
        r = time_rule(trigger={"kind": "BATTERY_BELOW", "threshold": 20})
        self.assertEqual(evaluate(r, ctx(battery=20)).reason,
                         "trigger-not-active")

    def test_power_connected(self):
        r = time_rule(trigger={"kind": "POWER_CONNECTED", "state": True})
        self.assertTrue(evaluate(r, ctx(charging=True)).fired)
        self.assertEqual(evaluate(r, ctx(charging=False)).reason,
                         "trigger-not-active")


class ConstraintTests(unittest.TestCase):
    def test_requires_unlock_blocks_when_locked(self):
        d = evaluate(time_rule(), ctx(now_minutes=22 * 60,
                                      device_unlocked=False))
        self.assertEqual(d.reason, "requires-unlock")

    def test_disabled_rule_never_fires(self):
        d = evaluate(time_rule(enabled=False), ctx(now_minutes=22 * 60))
        self.assertEqual(d.reason, "rule-disabled")

    def test_default_cap_is_ten(self):
        r = time_rule(max_runs_per_day=10)
        d = evaluate(r, ctx(now_minutes=22 * 60, runs_today=10))
        self.assertEqual(d.reason, "max-runs-reached")


class AuditAndDeterminismTests(unittest.TestCase):
    def test_audit_record_fields(self):
        r = time_rule()
        d = evaluate(r, ctx(now_minutes=22 * 60))
        rec = re_mod.make_audit_record(r, d, ctx(now_minutes=22 * 60),
                                       "2026-10-16T12:00:00Z")
        as_dict = rec.to_dict()
        for key in ("rule_id", "reason", "action_id", "explanation",
                    "context", "timestamp"):
            self.assertIn(key, as_dict)
        self.assertEqual(as_dict["timestamp"], "2026-10-16T12:00:00Z")

    def test_action_request_only_when_fired(self):
        r = time_rule()
        self.assertIsNotNone(re_mod.action_request(
            evaluate(r, ctx(now_minutes=22 * 60))))
        self.assertIsNone(re_mod.action_request(
            evaluate(r, ctx(now_minutes=23 * 60))))

    def test_determinism_same_inputs_same_outputs(self):
        r = time_rule(conditions=[{"kind": "TIME_WINDOW",
                                   "start": "21:00", "end": "23:30"}])
        c = ctx(now_minutes=22 * 60)
        outs = {(evaluate(r, c).fired, evaluate(r, c).reason,
                 evaluate(r, c).action_id) for _ in range(5)}
        self.assertEqual(len(outs), 1)


if __name__ == "__main__":
    unittest.main(verbosity=2)
