/*
 * SPDX-License-Identifier: Apache-2.0
 */
package com.drs.os.engine

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class EvaluationTest {

    private fun ctx(
        nowMinutes: Int = 1320,   // 22:00
        weekday: Int = 2,
        battery: Int = 80,
        charging: Boolean = false,
        deviceUnlocked: Boolean = true,
        runsToday: Int = 0
    ) = EventContext(nowMinutes, weekday, battery, charging, deviceUnlocked, runsToday)

    private fun quietHours() = Rule(
        id = "quiet", name = "Quiet hours", enabled = true,
        trigger = Trigger.TimeDaily("22:00"),
        conditions = listOf(Condition.TimeWindow("21:00", "23:30")),
        actionId = "set_dnd_on", maxRunsPerDay = 1,
        explanation = "تجريبي")

    @Test
    fun `fires when trigger and conditions hold`() {
        val d = Evaluator.evaluate(quietHours(), ctx())
        assertTrue(d.fired)
        assertEquals(DecisionReason.FIRED, d.reason)
        assertEquals("set_dnd_on", d.actionId)
        assertEquals("تجريبي", d.explanation)
        assertEquals(ActionRequest("quiet", "set_dnd_on", "تجريبي"), actionRequest(d))
    }

    @Test
    fun `disabled rule never fires - first decision`() {
        val d = Evaluator.evaluate(quietHours().copy(enabled = false), ctx())
        assertFalse(d.fired)
        assertEquals(DecisionReason.RULE_DISABLED, d.reason)
        assertNull(actionRequest(d))
    }

    @Test
    fun `trigger-not-active beats condition check`() {
        // Condition window holds but the exact-minute trigger is off.
        val d = Evaluator.evaluate(quietHours(), ctx(nowMinutes = 1280))
        assertEquals(DecisionReason.TRIGGER_NOT_ACTIVE, d.reason)
    }

    @Test
    fun `condition failure recorded after trigger active`() {
        val weekdayRule = Rule(
            id = "battery", name = "b", enabled = true,
            trigger = Trigger.BatteryBelow(20),
            conditions = listOf(Condition.DayOfWeek(setOf(0, 1, 2, 3, 4))),
            actionId = "enable_battery_saver", explanation = "e")
        val saturday = Evaluator.evaluate(weekdayRule, ctx(nowMinutes = 600, weekday = 5, battery = 10))
        assertEquals(DecisionReason.CONDITION_FAILED, saturday.reason)
        assertNull(saturday.actionId)
        val monday = Evaluator.evaluate(weekdayRule, ctx(nowMinutes = 600, weekday = 0, battery = 10))
        assertEquals(DecisionReason.FIRED, monday.reason)
    }

    @Test
    fun `requires-unlock path`() {
        val d = Evaluator.evaluate(quietHours(), ctx(deviceUnlocked = false))
        assertFalse(d.fired)
        assertEquals(DecisionReason.REQUIRES_UNLOCK, d.reason)
        // disabled check wins over unlock
        val d2 = Evaluator.evaluate(
            quietHours().copy(enabled = false), ctx(deviceUnlocked = false))
        assertEquals(DecisionReason.RULE_DISABLED, d2.reason)
    }

    @Test
    fun `max-runs-reached path`() {
        val d = Evaluator.evaluate(quietHours(), ctx(runsToday = 1))
        assertEquals(DecisionReason.MAX_RUNS_REACHED, d.reason)
        val ok = Evaluator.evaluate(quietHours(), ctx(runsToday = 0))
        assertEquals(DecisionReason.FIRED, ok.reason)
    }

    @Test
    fun `time window midnight span is legal`() {
        // Level-style trigger so the window logic can be probed at any
        // minute (an edge TIME_DAILY trigger only fires at one minute).
        val night = Rule(
            id = "n", name = "n", enabled = true,
            trigger = Trigger.PowerConnected(true),
            conditions = listOf(Condition.TimeWindow("22:00", "02:00")),
            actionId = "set_dnd_on", explanation = "e")
        assertEquals(
            DecisionReason.FIRED,
            Evaluator.evaluate(night, ctx(nowMinutes = 23 * 60 + 30, charging = true)).reason)
        assertEquals(
            DecisionReason.FIRED,
            Evaluator.evaluate(night, ctx(nowMinutes = 1 * 60 + 30, charging = true)).reason)
        assertEquals(
            DecisionReason.CONDITION_FAILED,
            Evaluator.evaluate(night, ctx(nowMinutes = 12 * 60, charging = true)).reason)
        // boundary semantics: [start, end) — end minute excluded
        assertEquals(
            DecisionReason.CONDITION_FAILED,
            Evaluator.evaluate(night, ctx(nowMinutes = 2 * 60, charging = true)).reason)
    }

    @Test
    fun `battery level trigger semantics - true while below`() {
        val rule = Rule(
            id = "b", name = "b", enabled = true,
            trigger = Trigger.BatteryBelow(20),
            actionId = "enable_battery_saver", explanation = "e")
        assertEquals(
            DecisionReason.FIRED,
            Evaluator.evaluate(rule, ctx(nowMinutes = 600, battery = 19)).reason)
        assertEquals(
            DecisionReason.TRIGGER_NOT_ACTIVE,
            Evaluator.evaluate(rule, ctx(nowMinutes = 600, battery = 20)).reason)
    }

    @Test
    fun `charging state trigger matches exactly`() {
        val on = Rule(
            id = "c", name = "c", enabled = true,
            trigger = Trigger.PowerConnected(true),
            actionId = "set_theme_dark", explanation = "e")
        assertEquals(
            DecisionReason.FIRED,
            Evaluator.evaluate(on, ctx(nowMinutes = 600, charging = true)).reason)
        assertEquals(
            DecisionReason.TRIGGER_NOT_ACTIVE,
            Evaluator.evaluate(on, ctx(nowMinutes = 600, charging = false)).reason)
    }

    @Test
    fun `evaluation is pure - same inputs same outputs`() {
        val a = Evaluator.evaluate(quietHours(), ctx())
        val b = Evaluator.evaluate(quietHours(), ctx())
        assertEquals(a, b)
    }

    @Test
    fun `multi-rule selection is deterministic and enabled-only`() {
        val rules = listOf(
            quietHours().copy(id = "zulu"),
            quietHours().copy(id = "alpha", enabled = false),
            quietHours().copy(id = "mike"))
        val selected = RuleSelection.forTrigger(rules) { it is Trigger.TimeDaily }
        assertEquals(listOf("mike", "zulu"), selected.map { it.id })
        // reversed input order produces the same selection
        val selected2 = RuleSelection.forTrigger(rules.reversed()) { it is Trigger.TimeDaily }
        assertEquals(selected, selected2)
    }

    @Test
    fun `trigger matching predicates route rule kinds`() {
        val rules = listOf(
            Rule("t1", "t", true, Trigger.TimeDaily("22:00"),
                actionId = "set_dnd_on", explanation = "e"),
            Rule("t2", "t", true, Trigger.BatteryBelow(20),
                actionId = "enable_battery_saver", explanation = "e"),
            Rule("t3", "t", true, Trigger.PowerConnected(true),
                actionId = "disable_battery_saver", explanation = "e"))
        assertEquals(
            listOf("t1"),
            RuleSelection.forTrigger(rules) { it is Trigger.TimeDaily }.map { it.id })
        assertEquals(
            listOf("t2", "t3"),
            RuleSelection.forTrigger(rules) { it !is Trigger.TimeDaily }.map { it.id })
    }
}
