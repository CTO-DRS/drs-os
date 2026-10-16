/*
 * SPDX-License-Identifier: Apache-2.0
 */
package com.drs.os.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class ValidationTest {

    private fun rule(
        id: String = "quiet_hours",
        name: String = "Quiet hours",
        enabled: Boolean = true,
        trigger: Trigger = Trigger.TimeDaily("22:00"),
        conditions: List<Condition> = listOf(Condition.TimeWindow("21:00", "23:30")),
        actionId: String = "set_dnd_on",
        requiresUnlock: Boolean = true,
        maxRunsPerDay: Int = 1,
        explanation: String = "يُفعّل عدم الإزعاج ليلاً",
        consented: List<String> = emptyList()
    ) = Rule(id, name, enabled, trigger, conditions, actionId,
        requiresUnlock, maxRunsPerDay, explanation, consented)

    @Test
    fun `valid rule has no errors`() {
        assertTrue(validateRule(rule()).isEmpty())
    }

    @Test
    fun `defaults accepted - no conditions, default limits`() {
        val r = Rule(
            id = "r1", name = "n", enabled = false,
            trigger = Trigger.PowerConnected(true), actionId = "set_theme_dark",
            explanation = "x")
        assertTrue(validateRule(r).isEmpty())
        assertEquals(true, r.requiresUnlock)
        assertEquals(10, r.maxRunsPerDay)
    }

    @Test
    fun `blank required strings rejected`() {
        assertEquals(1, validateRule(rule(id = "")).size)
        assertEquals(1, validateRule(rule(name = "  ")).size)
        assertEquals(1, validateRule(rule(explanation = "")).size)
        val fields = validateRule(rule(id = "", name = "", explanation = ""))
            .map { it.field }.toSet()
        assertEquals(setOf("id", "name", "explanation"), fields)
    }

    @Test
    fun `time format enforced`() {
        assertTrue(validateRule(rule(trigger = Trigger.TimeDaily("7:00")))
            .any { it.field == "trigger.time" })
        assertTrue(validateRule(rule(trigger = Trigger.TimeDaily("24:00")))
            .any { it.field == "trigger.time" })
        assertTrue(validateRule(rule(trigger = Trigger.TimeDaily("23:59"))).isEmpty())
        assertTrue(validateRule(rule(trigger = Trigger.TimeDaily("00:00"))).isEmpty())
    }

    @Test
    fun `battery threshold bounds enforced`() {
        assertTrue(validateRule(rule(
            trigger = Trigger.BatteryBelow(0), actionId = "enable_battery_saver"))
            .any { it.field == "trigger.threshold" })
        assertTrue(validateRule(rule(
            trigger = Trigger.BatteryBelow(100), actionId = "enable_battery_saver"))
            .any { it.field == "trigger.threshold" })
        assertTrue(validateRule(rule(
            trigger = Trigger.BatteryBelow(1), actionId = "enable_battery_saver")).isEmpty())
        assertTrue(validateRule(rule(
            trigger = Trigger.BatteryBelow(99), actionId = "enable_battery_saver")).isEmpty())
    }

    @Test
    fun `unknown action rejected by closed registry`() {
        val errs = validateRule(rule(actionId = "format_device"))
        assertEquals(listOf("action_id"), errs.map { it.field })
        assertFalse(errs.isEmpty())
    }

    @Test
    fun `security-sensitive action requires recorded consent`() {
        val testRegistry = mapOf("wipe_cache" to true, "set_dnd_on" to false)
        val noConsent = Rule(
            id = "r", name = "n", enabled = true,
            trigger = Trigger.PowerConnected(true),
            actionId = "wipe_cache", explanation = "x")
        val errs = validateRule(noConsent, testRegistry)
        assertTrue(errs.any {
            it.field == "action_id" && it.message.contains("security-sensitive")
        })
        val withConsent = noConsent.copy(userConsentedActions = listOf("wipe_cache"))
        assertTrue(validateRule(withConsent, testRegistry).isEmpty())
    }

    @Test
    fun `condition value constraints enforced`() {
        assertTrue(validateRule(rule(
            conditions = listOf(Condition.TimeWindow("21:0", "23:00"))))
            .any { it.field.startsWith("condition TIME_WINDOW") })
        assertTrue(validateRule(rule(conditions = listOf(Condition.DayOfWeek(emptySet()))))
            .any { it.field == "condition DAY_OF_WEEK.days" })
        assertTrue(validateRule(rule(conditions = listOf(Condition.DayOfWeek(setOf(0, 6)))))
            .isEmpty())
        assertTrue(validateRule(rule(conditions = listOf(Condition.DayOfWeek(setOf(7)))))
            .any { it.field == "condition DAY_OF_WEEK.days" })
    }

    @Test
    fun `max_runs_per_day must be at least one`() {
        assertTrue(validateRule(rule(maxRunsPerDay = 0))
            .any { it.field == "max_runs_per_day" })
        assertTrue(validateRule(rule(maxRunsPerDay = 1)).isEmpty())
    }

    @Test
    fun `registry is closed and non-sensitive in v1`() {
        assertEquals(
            setOf("set_dnd_on", "set_dnd_off", "set_theme_dark", "set_theme_light",
                "enable_battery_saver", "disable_battery_saver"),
            ActionRegistry.ACTIONS.keys)
        assertTrue(ActionRegistry.ACTIONS.values.none { it })
    }
}
