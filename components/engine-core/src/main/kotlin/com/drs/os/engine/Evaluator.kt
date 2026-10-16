/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS OS deterministic automation — pure evaluation (spec §3).
 * Function-for-function equivalent of the Python reference model.
 */
package com.drs.os.engine

object Evaluator {

    /** "HH:MM" → minutes of day. Validation guarantees the format. */
    fun timeToMinutes(time: String): Int {
        val m = TIME_RE.matchEntire(time)
            ?: throw IllegalArgumentException("malformed time '$time'")
        val h = m.groupValues[1].toInt()
        val min = m.groupValues[2].toInt()
        return h * 60 + min
    }

    private val TIME_RE = Regex("""^([01]\d|2[0-3]):([0-5]\d)$""")

    fun triggerActive(trigger: Trigger, ctx: EventContext): Boolean = when (trigger) {
        is Trigger.TimeDaily -> ctx.nowMinutes == timeToMinutes(trigger.time)
        is Trigger.BatteryBelow -> ctx.battery < trigger.threshold
        is Trigger.PowerConnected -> ctx.charging == trigger.state
    }

    fun conditionHolds(condition: Condition, ctx: EventContext): Boolean = when (condition) {
        is Condition.TimeWindow -> {
            val start = timeToMinutes(condition.start)
            val end = timeToMinutes(condition.end)
            if (start <= end) {
                start <= ctx.nowMinutes && ctx.nowMinutes < end
            } else {
                // start > end legally spans midnight (spec §2).
                ctx.nowMinutes >= start || ctx.nowMinutes < end
            }
        }
        is Condition.DayOfWeek -> ctx.weekday in condition.days
    }

    /**
     * Evaluate one rule against one context. Never executes anything.
     * Decision order (first failure wins, reason recorded) — spec §3:
     * rule-disabled → trigger-not-active → condition-failed →
     * requires-unlock → max-runs-reached → fired.
     */
    fun evaluate(rule: Rule, ctx: EventContext): Decision {
        if (!rule.enabled) {
            return Decision(rule.id, false, null, DecisionReason.RULE_DISABLED, rule.explanation)
        }
        if (!triggerActive(rule.trigger, ctx)) {
            return Decision(rule.id, false, null, DecisionReason.TRIGGER_NOT_ACTIVE, rule.explanation)
        }
        for (cond in rule.conditions) {
            if (!conditionHolds(cond, ctx)) {
                return Decision(rule.id, false, null, DecisionReason.CONDITION_FAILED, rule.explanation)
            }
        }
        if (rule.requiresUnlock && !ctx.deviceUnlocked) {
            return Decision(rule.id, false, null, DecisionReason.REQUIRES_UNLOCK, rule.explanation)
        }
        if (ctx.runsToday >= rule.maxRunsPerDay) {
            return Decision(rule.id, false, null, DecisionReason.MAX_RUNS_REACHED, rule.explanation)
        }
        return Decision(rule.id, true, rule.actionId, DecisionReason.FIRED, rule.explanation)
    }
}

/** Extracts the ActionRequest from a fired Decision (spec §1.5). */
fun actionRequest(decision: Decision): ActionRequest? =
    if (decision.fired && decision.actionId != null) {
        ActionRequest(decision.ruleId, decision.actionId, decision.explanation)
    } else {
        null
    }

/**
 * Deterministic multi-rule selection: enabled rules whose trigger
 * satisfies [isActive], ordered by rule id ascending. The reference
 * model has no priority field; id order is the documented tie-break so
 * two hosts always evaluate rules in the same sequence.
 */
object RuleSelection {
    fun forTrigger(
        rules: List<Rule>,
        isActive: (Trigger) -> Boolean
    ): List<Rule> = rules
        .asSequence()
        .filter { it.enabled && isActive(it.trigger) }
        .sortedBy { it.id }
        .toList()
}
