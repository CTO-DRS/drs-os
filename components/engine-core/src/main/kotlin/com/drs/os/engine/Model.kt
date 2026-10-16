/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS OS deterministic automation — core value objects.
 *
 * This is the Kotlin product implementation of the semantics pinned by
 * docs/automation/rule-engine-spec.md and the Python reference model in
 * tools/automation-reference/rule_engine.py. Field names and decision
 * reasons intentionally match the reference wire format so both
 * implementations can be differentially tested.
 *
 * Invariants (spec §1): pure evaluation, executes nothing, closed action
 * registry, consent gate for security-sensitive actions, mandatory audit
 * records, engine stays clock-free (timestamps are host-supplied).
 */
package com.drs.os.engine

/** Machine-stable reason codes; `wire` matches the Python reference. */
enum class DecisionReason(val wire: String) {
    RULE_DISABLED("rule-disabled"),
    TRIGGER_NOT_ACTIVE("trigger-not-active"),
    CONDITION_FAILED("condition-failed"),
    REQUIRES_UNLOCK("requires-unlock"),
    MAX_RUNS_REACHED("max-runs-reached"),
    FIRED("fired");

    companion object {
        fun fromWire(wire: String): DecisionReason? =
            entries.firstOrNull { it.wire == wire }
    }
}

/** Trigger kinds (spec §2). */
sealed interface Trigger {
    /** "HH:MM" 24-hour, validated by [com.drs.os.engine.validateRule]. */
    data class TimeDaily(val time: String) : Trigger

    /** 1..99, level-triggered: true while battery < threshold. */
    data class BatteryBelow(val threshold: Int) : Trigger

    /** Level-triggered on charging-state match. */
    data class PowerConnected(val state: Boolean) : Trigger
}

/** Condition kinds (spec §2). All conditions of a rule must hold (AND). */
sealed interface Condition {
    /** now in [start, end); start > end legally spans midnight. */
    data class TimeWindow(val start: String, val end: String) : Condition

    /** Non-empty subset of 0..6, ISO weekday, 0 = Monday. */
    data class DayOfWeek(val days: Set<Int>) : Condition
}

/**
 * A user-defined automation rule. The Kotlin type system enforces
 * presence/typing that the reference model checks at runtime; value
 * constraints are enforced by [validateRule].
 */
data class Rule(
    val id: String,
    val name: String,
    val enabled: Boolean,
    val trigger: Trigger,
    val conditions: List<Condition> = emptyList(),
    val actionId: String,
    val requiresUnlock: Boolean = true,
    val maxRunsPerDay: Int = 10,
    val explanation: String,
    /** Required to contain [actionId] iff that action is sensitive. */
    val userConsentedActions: List<String> = emptyList()
)

/**
 * Pure evaluation context (spec §3). Same inputs always produce the same
 * [Decision] — the context carries everything the decision depends on.
 */
data class EventContext(
    /** 0..1439, minute of day. */
    val nowMinutes: Int,
    /** 0 = Monday .. 6 = Sunday (ISO). */
    val weekday: Int,
    /** 0..100. */
    val battery: Int,
    val charging: Boolean,
    val deviceUnlocked: Boolean,
    /** Host-maintained counter for this rule today. */
    val runsToday: Int
)

data class Decision(
    val ruleId: String,
    val fired: Boolean,
    val actionId: String?,
    val reason: DecisionReason,
    val explanation: String
)

/** The only thing the engine ever "does": hand a request to the host. */
data class ActionRequest(
    val ruleId: String,
    val actionId: String,
    val explanation: String
)

/**
 * Mandatory audit record (spec §5). Appended for EVERY evaluation,
 * including disabled-rule and non-matching outcomes. Contains no free
 * text beyond the rule's own user-visible [explanation] — no context
 * values, no personal data.
 */
data class AuditRecord(
    val ruleId: String,
    val reason: String,
    val actionId: String?,
    val explanation: String,
    val context: EventContext,
    /** Host-supplied; the engine stays clock-free (spec §5). */
    val timestamp: String
) {
    /** Flattened, persistence-ready representation (audit schema). */
    fun toFlatMap(): Map<String, String> = linkedMapOf(
        "rule_id" to ruleId,
        "reason" to reason,
        "action_id" to (actionId ?: ""),
        "explanation" to explanation,
        "ctx_now_minutes" to context.nowMinutes.toString(),
        "ctx_weekday" to context.weekday.toString(),
        "ctx_battery" to context.battery.toString(),
        "ctx_charging" to context.charging.toString(),
        "ctx_device_unlocked" to context.deviceUnlocked.toString(),
        "ctx_runs_today" to context.runsToday.toString(),
        "timestamp" to timestamp
    )

    companion object {
        fun fromFlatMap(map: Map<String, String>): AuditRecord = AuditRecord(
            ruleId = map.getValue("rule_id"),
            reason = map.getValue("reason"),
            actionId = map["action_id"]?.takeIf { it.isNotEmpty() },
            explanation = map.getValue("explanation"),
            context = EventContext(
                nowMinutes = map.getValue("ctx_now_minutes").toInt(),
                weekday = map.getValue("ctx_weekday").toInt(),
                battery = map.getValue("ctx_battery").toInt(),
                charging = map.getValue("ctx_charging").toBoolean(),
                deviceUnlocked = map.getValue("ctx_device_unlocked").toBoolean(),
                runsToday = map.getValue("ctx_runs_today").toInt()
            ),
            timestamp = map.getValue("timestamp")
        )
    }
}
