#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""DRS OS automation — differential test-vector generator.

Runs the Phase 2 Python reference model (rule_engine.py) over a fixed,
hand-picked matrix of rule/context cases and writes the expected
decisions to test_vectors.json. The Kotlin engine (engine-core) replays
the same file in its unit tests, so the two implementations are kept
semantically identical by construction.

Deterministic: no randomness, no clocks, no network. Re-running this
script MUST produce a byte-identical file; CI can enforce that by
regenerating and diffing.
"""

from __future__ import annotations

import argparse
import json
import os
import sys

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))

from rule_engine import EventContext, evaluate, validate_rule  # noqa: E402

DEFAULT_OUT = os.path.join(os.path.dirname(os.path.abspath(__file__)),
                           "test_vectors.json")


def R(**kw):
    """Build a rule dict with spec defaults."""
    rule = {
        "id": kw.get("id", "rule"),
        "name": kw.get("name", "rule"),
        "enabled": kw.get("enabled", True),
        "trigger": kw["trigger"],
        "conditions": kw.get("conditions", []),
        "action_id": kw["action_id"],
        "requires_unlock": kw.get("requires_unlock", True),
        "max_runs_per_day": kw.get("max_runs_per_day", 10),
        "explanation": kw.get("explanation", "explanation"),
        "user_consented_actions": kw.get("user_consented_actions", []),
    }
    errors = validate_rule(rule)
    if errors:
        raise ValueError("invalid test rule %r: %s" % (rule["id"], errors))
    return rule


def C(now=1320, weekday=2, battery=80, charging=False, unlocked=True,
      runs=0):
    return {"now_minutes": now, "weekday": weekday, "battery": battery,
            "charging": charging, "device_unlocked": unlocked,
            "runs_today": runs}


def case(case_id, rule, ctx):
    decision = evaluate(rule, EventContext(**ctx))
    return {
        "case_id": case_id,
        "rule": rule,
        "context": ctx,
        "expected": {
            "fired": decision.fired,
            "reason": decision.reason,
            "action_id": decision.action_id,
            "explanation": decision.explanation,
        },
    }


TIME = {"kind": "TIME_DAILY", "time": "22:00"}
BATT = {"kind": "BATTERY_BELOW", "threshold": 20}
POWER = {"kind": "POWER_CONNECTED", "state": True}
WIN_21_2330 = {"kind": "TIME_WINDOW", "start": "21:00", "end": "23:30"}
WIN_MIDNIGHT = {"kind": "TIME_WINDOW", "start": "22:00", "end": "02:00"}
WIN_EMPTY_GAP = {"kind": "TIME_WINDOW", "start": "23:30", "end": "23:00"}
DAYS_ALL = {"kind": "DAY_OF_WEEK", "days": [0, 1, 2, 3, 4, 5, 6]}
DAYS_WEEKDAY = {"kind": "DAY_OF_WEEK", "days": [0, 1, 2, 3, 4]}

vectors = []

# --- quiet hours (spec §6 example 1) --------------------------------
quiet = R(id="quiet_hours", name="Quiet hours", trigger=TIME,
          conditions=[WIN_21_2330], action_id="set_dnd_on",
          max_runs_per_day=1, explanation="يُفعّل عدم الإزعاج")
vectors += [
    case("quiet_fires_in_window", quiet, C(now=1320)),
    case("quiet_edge_start_inclusive", quiet, C(now=1260)),   # 21:00
    case("quiet_edge_end_exclusive", quiet, C(now=1410)),     # 23:30
    case("quiet_outside_window", quiet, C(now=1200)),         # 20:00
    case("quiet_wrong_minute", quiet, C(now=1321)),
    case("quiet_disabled", R(**{**quiet, "enabled": False}), C(now=1320)),
    case("quiet_max_runs_reached", quiet, C(now=1320, runs=1)),
    case("quiet_locked_device", quiet, C(now=1320, unlocked=False)),
    case("quiet_runs_zero_ok", quiet, C(now=1320, runs=0)),
]

# --- battery saver (spec §6 example 2) -------------------------------
batt = R(id="battery_saver", name="Battery saver", trigger=BATT,
         conditions=[DAYS_WEEKDAY], action_id="enable_battery_saver",
         max_runs_per_day=4, explanation="موفّر الطاقة")
vectors += [
    case("batt_fires_below_threshold", batt, C(now=600, battery=19, weekday=0)),
    case("batt_not_active_at_threshold", batt, C(now=600, battery=20, weekday=0)),
    case("batt_threshold_one_edge", R(id="b1", name="b", trigger={"kind": "BATTERY_BELOW", "threshold": 1},
                                      action_id="enable_battery_saver"),
         C(now=600, battery=0)),
    case("batt_threshold_ninetynine_edge", R(id="b99", name="b", trigger={"kind": "BATTERY_BELOW", "threshold": 99},
                                            action_id="enable_battery_saver"),
         C(now=600, battery=98)),
    case("batt_condition_weekend_blocks", batt, C(now=600, battery=10, weekday=5)),
    case("batt_condition_sunday_passes", batt, C(now=600, battery=10, weekday=6)),
]

# --- morning theme (spec §6 example 3) -------------------------------
morning = R(id="morning_theme", name="Morning theme",
            trigger={"kind": "TIME_DAILY", "time": "07:30"},
            conditions=[], action_id="set_theme_light",
            requires_unlock=True, max_runs_per_day=2,
            explanation="ثيم الصباح")
vectors += [
    case("morning_fires_unlocked", morning, C(now=450)),
    case("morning_waits_for_unlock", morning, C(now=450, unlocked=False)),
    case("morning_second_run_of_day", morning, C(now=451, runs=2)),
    case("morning_first_of_two_runs", morning, C(now=451, runs=1)),
]

# --- power connected --------------------------------------------------
power = R(id="charger_dark", name="Charger dark", trigger=POWER,
          action_id="set_theme_dark", explanation="e")
vectors += [
    case("power_fires_on_state_true", power, C(now=600, charging=True)),
    case("power_not_active_when_false", power, C(now=600, charging=False)),
    case("power_disabled_wins_first", R(**{**power, "enabled": False}),
         C(now=600, charging=True)),
]

# --- midnight-spanning window ----------------------------------------
mid = R(id="night_span", name="Night span", trigger={"kind": "TIME_DAILY", "time": "23:30"},
        conditions=[WIN_MIDNIGHT], action_id="set_dnd_on", explanation="e")
vectors += [
    case("midnight_at_start", mid, C(now=1410)),          # 23:30
    case("midnight_after_wrap", mid, C(now=90)),          # 01:30
    case("midnight_end_exclusive", mid, C(now=120)),      # 02:00
    case("midnoon_outside", mid, C(now=720)),             # 12:00
]

# --- empty window (start > end, non-midnight gap) ---------------------
gap = R(id="empty_gap", name="Empty gap", trigger={"kind": "TIME_DAILY", "time": "23:15"},
        conditions=[WIN_EMPTY_GAP], action_id="set_dnd_off", explanation="e")
vectors += [
    case("gap_before_inverted_start_fails", gap, C(now=1395)),  # 23:15 < start
    case("gap_inside_inverted_passes", gap, C(now=1415)),       # 23:35 >= start
]

# --- day-of-week edges ------------------------------------------------
daily = R(id="all_days", name="all", trigger={"kind": "TIME_DAILY", "time": "09:00"},
          conditions=[DAYS_ALL], action_id="set_theme_light", explanation="e")
vectors += [
    case("days_monday", daily, C(now=540, weekday=0)),
    case("days_sunday", daily, C(now=540, weekday=6)),
    case("days_wrong_minute", daily, C(now=541, weekday=6)),
]

# --- defaults & boundary fields ---------------------------------------
no_unlock = R(id="no_unlock", name="n", trigger={"kind": "TIME_DAILY", "time": "10:00"},
              action_id="set_theme_light", requires_unlock=False, explanation="e")
vectors += [
    case("no_unlock_fires_locked", no_unlock, C(now=600, unlocked=False)),
]

ten_runs = R(id="ten", name="t", trigger={"kind": "TIME_DAILY", "time": "11:00"},
             action_id="set_dnd_off", max_runs_per_day=10, explanation="e")
vectors += [
    case("default_max_at_nine", ten_runs, C(now=660, runs=9)),
    case("default_max_at_ten", ten_runs, C(now=660, runs=10)),
]

# --- write ------------------------------------------------------------


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--out", default=DEFAULT_OUT,
                        help="output path (default: committed test_vectors.json)")
    args = parser.parse_args()
    with open(args.out, "w", encoding="utf-8") as fh:
        json.dump(vectors, fh, sort_keys=True, indent=1,
                  ensure_ascii=False)
        fh.write("\n")
    print("wrote %d vectors to %s" % (len(vectors), args.out))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
