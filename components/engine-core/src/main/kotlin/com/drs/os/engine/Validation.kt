/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS OS deterministic automation — closed action registry and rule
 * validation. Semantics mirror tools/automation-reference/rule_engine.py
 * (spec §2). Extending the registry is a reviewed change that must also
 * extend the reference model and tests.
 */
package com.drs.os.engine

/** Closed registry: action_id → security_sensitive. */
object ActionRegistry {
    val ACTIONS: Map<String, Boolean> = linkedMapOf(
        "set_dnd_on" to false,
        "set_dnd_off" to false,
        "set_theme_dark" to false,
        "set_theme_light" to false,
        "enable_battery_saver" to false,
        "disable_battery_saver" to false
    )

    fun isKnown(actionId: String): Boolean = ACTIONS.containsKey(actionId)

    fun isSensitive(actionId: String): Boolean = ACTIONS[actionId] == true
}

data class RuleValidationError(val field: String, val message: String) {
    override fun toString(): String = "$field: $message"
}

private val TIME_RE = Regex("""^([01]\d|2[0-3]):([0-5]\d)$""")

private const val WIRE_TIME_DAILY = "TIME_DAILY"
private const val WIRE_BATTERY_BELOW = "BATTERY_BELOW"
private const val WIRE_POWER_CONNECTED = "POWER_CONNECTED"

/** Wire names for trigger kinds (shared with the reference model). */
object TriggerKinds {
    const val TIME_DAILY = WIRE_TIME_DAILY
    const val BATTERY_BELOW = WIRE_BATTERY_BELOW
    const val POWER_CONNECTED = WIRE_POWER_CONNECTED
}

/**
 * Returns every validation error; an empty list means the rule is valid.
 * Acceptance set is identical to the Python reference: a rule accepted
 * here is accepted there and vice versa (enforced differentially by the
 * committed test vectors).
 *
 * The registry is injectable ONLY for the differential/test layer; the
 * production entry point [validateRule] always uses the closed
 * [ActionRegistry].
 */
fun validateRule(
    rule: Rule,
    registry: Map<String, Boolean>
): List<RuleValidationError> {
    val errors = ArrayList<RuleValidationError>()

    if (rule.id.isBlank()) {
        errors.add(RuleValidationError("id", "required non-empty string"))
    }
    if (rule.name.isBlank()) {
        errors.add(RuleValidationError("name", "required non-empty string"))
    }
    if (rule.explanation.isBlank()) {
        errors.add(
            RuleValidationError(
                "explanation", "required non-empty user-visible string"))
    }

    when (val t = rule.trigger) {
        is Trigger.TimeDaily -> {
            if (!TIME_RE.matches(t.time)) {
                errors.add(
                    RuleValidationError(
                        "trigger.time", "required 'HH:MM' 24-hour string"))
            }
        }
        is Trigger.BatteryBelow -> {
            if (t.threshold !in 1..99) {
                errors.add(
                    RuleValidationError(
                        "trigger.threshold", "required integer 1..99"))
            }
        }
        is Trigger.PowerConnected -> {
            // state: Boolean — type system enforces the reference check.
        }
    }

    for (c in rule.conditions) {
        when (c) {
            is Condition.TimeWindow -> {
                if (!TIME_RE.matches(c.start)) {
                    errors.add(
                        RuleValidationError(
                            "condition TIME_WINDOW.start", "required 'HH:MM'"))
                }
                if (!TIME_RE.matches(c.end)) {
                    errors.add(
                        RuleValidationError(
                            "condition TIME_WINDOW.end", "required 'HH:MM'"))
                }
            }
            is Condition.DayOfWeek -> {
                if (c.days.isEmpty() || c.days.any { it !in 0..6 }) {
                    errors.add(
                        RuleValidationError(
                            "condition DAY_OF_WEEK.days",
                            "required non-empty list of ints 0..6"))
                }
            }
        }
    }

    if (!registry.containsKey(rule.actionId)) {
        errors.add(
            RuleValidationError(
                "action_id",
                "must be a known action from the closed registry " +
                    "(unknown or missing: \"${rule.actionId}\")"
            )
        )
    } else if (registry[rule.actionId] == true &&
        rule.actionId !in rule.userConsentedActions
    ) {
        errors.add(
            RuleValidationError(
                "action_id",
                "action \"${rule.actionId}\" is security-sensitive: explicit " +
                    "recorded consent required in user_consented_actions"
            )
        )
    }

    if (rule.maxRunsPerDay < 1) {
        errors.add(
            RuleValidationError(
                "max_runs_per_day", "required integer >= 1"))
    }
    // requires_unlock: Boolean — type system enforces the reference check.

    return errors
}

fun isRuleValid(rule: Rule): Boolean = validateRule(rule).isEmpty()

/** Production entry point: always the closed registry. */
fun validateRule(rule: Rule): List<RuleValidationError> =
    validateRule(rule, ActionRegistry.ACTIONS)
