#!/usr/bin/env python3
# SPDX-License-Identifier: Apache-2.0
"""DRS OS deterministic automation — rule engine REFERENCE MODEL.

Specification-by-example for docs/automation/rule-engine-spec.md.

This is NOT the shipping product code: the platform engine will be
Kotlin, integrated per docs/ui/native-system-surfaces.md. This module
pins the semantics in executable form so they can be tested on any host
with Python 3.10+, without AOSP, without network, without AI.

Design invariants:
  * Pure functions — same inputs always produce the same Decision.
  * Executes nothing — returns ActionRequest/audit data only.
  * Closed action registry; unknown actions are validation errors.
  * Security-sensitive actions require explicit recorded consent.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field

# --------------------------------------------------------------------------
# Action registry (closed)
# --------------------------------------------------------------------------

ACTION_REGISTRY: dict[str, bool] = {
    # action_id            security_sensitive
    "set_dnd_on": False,
    "set_dnd_off": False,
    "set_theme_dark": False,
    "set_theme_light": False,
    "enable_battery_saver": False,
    "disable_battery_saver": False,
}

_TRIGGER_KINDS = ("TIME_DAILY", "BATTERY_BELOW", "POWER_CONNECTED")
_CONDITION_KINDS = ("TIME_WINDOW", "DAY_OF_WEEK")
_TIME_RE = re.compile(r"^([01]\d|2[0-3]):([0-5]\d)$")


# --------------------------------------------------------------------------
# Value objects
# --------------------------------------------------------------------------

@dataclass(frozen=True)
class EventContext:
    now_minutes: int        # 0..1439, minute of day
    weekday: int            # 0=Monday .. 6=Sunday (ISO)
    battery: int            # 0..100
    charging: bool
    device_unlocked: bool
    runs_today: int         # host-maintained counter for this rule today


@dataclass(frozen=True)
class Decision:
    rule_id: str
    fired: bool
    action_id: str | None
    reason: str             # spec §3 reasons
    explanation: str


@dataclass(frozen=True)
class ActionRequest:
    rule_id: str
    action_id: str
    explanation: str


@dataclass(frozen=True)
class AuditRecord:
    rule_id: str
    reason: str
    action_id: str | None
    explanation: str
    context: EventContext
    timestamp: str          # host-supplied (engine stays clock-free)

    def to_dict(self) -> dict:
        return {
            "rule_id": self.rule_id,
            "reason": self.reason,
            "action_id": self.action_id,
            "explanation": self.explanation,
            "context": {
                "now_minutes": self.context.now_minutes,
                "weekday": self.context.weekday,
                "battery": self.context.battery,
                "charging": self.context.charging,
                "device_unlocked": self.context.device_unlocked,
                "runs_today": self.context.runs_today,
            },
            "timestamp": self.timestamp,
        }


# --------------------------------------------------------------------------
# Validation
# --------------------------------------------------------------------------

def validate_rule(rule: dict) -> list[str]:
    """Return a list of validation errors; empty list means valid."""
    errors: list[str] = []

    if not isinstance(rule, dict):
        return ["rule must be an object"]

    if not rule.get("id") or not isinstance(rule.get("id"), str):
        errors.append("id: required non-empty string")
    if not rule.get("name") or not isinstance(rule.get("name"), str):
        errors.append("name: required non-empty string")
    if not isinstance(rule.get("enabled"), bool):
        errors.append("enabled: required boolean")
    if not (isinstance(rule.get("explanation"), str) and rule["explanation"].strip()):
        errors.append("explanation: required non-empty user-visible string")

    # --- trigger ---
    trig = rule.get("trigger")
    if not isinstance(trig, dict) or trig.get("kind") not in _TRIGGER_KINDS:
        errors.append("trigger.kind: must be one of %s" % (_TRIGGER_KINDS,))
    else:
        kind = trig["kind"]
        if kind == "TIME_DAILY":
            t = trig.get("time")
            if not (isinstance(t, str) and _TIME_RE.match(t)):
                errors.append("trigger.time: required 'HH:MM' 24-hour string")
        elif kind == "BATTERY_BELOW":
            th = trig.get("threshold")
            if not (isinstance(th, int) and not isinstance(th, bool)
                    and 1 <= th <= 99):
                errors.append("trigger.threshold: required integer 1..99")
        elif kind == "POWER_CONNECTED":
            if not isinstance(trig.get("state"), bool):
                errors.append("trigger.state: required boolean")

    # --- conditions ---
    conds = rule.get("conditions", [])
    if not isinstance(conds, list):
        errors.append("conditions: must be a list")
    else:
        for c in conds:
            if not isinstance(c, dict) or c.get("kind") not in _CONDITION_KINDS:
                errors.append("condition.kind: must be one of %s"
                              % (_CONDITION_KINDS,))
                continue
            for k in (("start", "end") if c["kind"] == "TIME_WINDOW"
                      else ("days",)):
                if k not in c:
                    errors.append("condition %s: missing %r" % (c["kind"], k))
            if c["kind"] == "TIME_WINDOW":
                for k in ("start", "end"):
                    if k in c and not (isinstance(c[k], str)
                                       and _TIME_RE.match(c[k])):
                        errors.append("condition TIME_WINDOW.%s: "
                                      "required 'HH:MM'" % k)
            if c["kind"] == "DAY_OF_WEEK":
                days = c.get("days")
                if not (isinstance(days, list) and days
                        and all(isinstance(d, int) and not isinstance(d, bool)
                                and 0 <= d <= 6 for d in days)):
                    errors.append("condition DAY_OF_WEEK.days: "
                                  "required non-empty list of ints 0..6")

    # --- action + consent ---
    action_id = rule.get("action_id")
    if not isinstance(action_id, str) or action_id not in ACTION_REGISTRY:
        errors.append("action_id: must be a known action from the closed "
                      "registry (unknown or missing: %r)" % (action_id,))
    else:
        consented = rule.get("user_consented_actions", [])
        if ACTION_REGISTRY[action_id] and action_id not in consented:
            errors.append(
                "action %r is security-sensitive: explicit recorded consent "
                "required in user_consented_actions" % action_id)

    # --- constraints ---
    ru = rule.get("requires_unlock", True)
    if not isinstance(ru, bool):
        errors.append("requires_unlock: must be boolean")
    mr = rule.get("max_runs_per_day", 10)
    if not (isinstance(mr, int) and not isinstance(mr, bool) and mr >= 1):
        errors.append("max_runs_per_day: required integer >= 1")

    return errors


# --------------------------------------------------------------------------
# Evaluation (pure, deterministic)
# --------------------------------------------------------------------------

def _time_to_minutes(t: str) -> int:
    h, m = t.split(":")
    return int(h) * 60 + int(m)


def _trigger_active(trigger: dict, ctx: EventContext) -> bool:
    kind = trigger["kind"]
    if kind == "TIME_DAILY":
        return ctx.now_minutes == _time_to_minutes(trigger["time"])
    if kind == "BATTERY_BELOW":
        return ctx.battery < trigger["threshold"]
    if kind == "POWER_CONNECTED":
        return ctx.charging == trigger["state"]
    return False  # unreachable: registry-validated


def _condition_holds(cond: dict, ctx: EventContext) -> bool:
    kind = cond["kind"]
    if kind == "TIME_WINDOW":
        start = _time_to_minutes(cond["start"])
        end = _time_to_minutes(cond["end"])
        if start <= end:
            return start <= ctx.now_minutes < end
        return ctx.now_minutes >= start or ctx.now_minutes < end  # midnight span
    if kind == "DAY_OF_WEEK":
        return ctx.weekday in cond["days"]
    return False  # unreachable


def evaluate(rule: dict, ctx: EventContext) -> Decision:
    """Evaluate one rule against one context. Never executes anything."""
    def dec(fired: bool, action: str | None, reason: str) -> Decision:
        return Decision(rule["id"], fired, action, reason,
                        rule.get("explanation", ""))

    if not rule.get("enabled", False):
        return dec(False, None, "rule-disabled")
    if not _trigger_active(rule["trigger"], ctx):
        return dec(False, None, "trigger-not-active")
    for cond in rule.get("conditions", []):
        if not _condition_holds(cond, ctx):
            return dec(False, None, "condition-failed")
    if rule.get("requires_unlock", True) and not ctx.device_unlocked:
        return dec(False, None, "requires-unlock")
    if ctx.runs_today >= rule.get("max_runs_per_day", 10):
        return dec(False, None, "max-runs-reached")
    return dec(True, rule["action_id"], "fired")


def action_request(decision: Decision) -> ActionRequest | None:
    """Extract the ActionRequest from a fired Decision (spec §1.5)."""
    if decision.fired and decision.action_id:
        return ActionRequest(decision.rule_id, decision.action_id,
                             decision.explanation)
    return None


def make_audit_record(rule: dict, decision: Decision, ctx: EventContext,
                      timestamp: str) -> AuditRecord:
    """Build the mandatory audit record; timestamp supplied by the host."""
    return AuditRecord(rule["id"], decision.reason, decision.action_id,
                       decision.explanation, ctx, timestamp)
