/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Automation presentation mapping tests (pure JVM — no Android APIs).
 */
package com.drs.os.automation.ui

import com.drs.os.engine.Condition
import com.drs.os.engine.DecisionReason
import com.drs.os.engine.EventContext
import com.drs.os.engine.Evaluator
import com.drs.os.engine.Rule
import com.drs.os.engine.Trigger
import com.drs.os.engine.host.ExecutionRecord
import com.drs.os.engine.host.ExecutionStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AutomationPresentationTest {

    private val rule = Rule(
        id = "quiet_hours", name = "Quiet", enabled = true,
        trigger = Trigger.BatteryBelow(20),
        conditions = listOf(Condition.DayOfWeek(setOf(0))),
        actionId = "enable_battery_saver", explanation = "e")

    @Test
    fun `rule rows expose stable trigger keys and validation state`() {
        val rows = AutomationPresentation.ruleRows(listOf(rule))
        val row = rows.single()
        assertEquals("trigger_battery_below", row.triggerKey)
        assertEquals("20", row.triggerDetail)
        assertTrue(row.invalidReasons.isEmpty())
    }

    @Test
    fun `invalid rules surface validation reasons in their row`() {
        val invalid = rule.copy(id = "", maxRunsPerDay = 0)
        val row = AutomationPresentation.ruleRows(listOf(invalid)).single()
        assertTrue(row.invalidReasons.isNotEmpty())
        assertTrue(row.invalidReasons.any { it.startsWith("id:") })
        assertTrue(row.invalidReasons.any { it.startsWith("max_runs_per_day:") })
    }

    @Test
    fun `decision rows map wire reasons to stable keys`() {
        val fired = Evaluator.evaluate(rule, EventContext(600, 0, 10, false, true, 0))
        assertEquals("reason_fired", AutomationPresentation.decisionRow(fired).reasonKey)
        assertEquals("enable_battery_saver", AutomationPresentation.decisionRow(fired).reasonDetail)

        val locked = Evaluator.evaluate(rule, EventContext(600, 0, 10, false, false, 0))
        assertEquals("reason_requires_unlock", AutomationPresentation.decisionRow(locked).reasonKey)
        assertEquals("", AutomationPresentation.decisionRow(locked).reasonDetail)
    }

    @Test
    fun `execution rows map statuses to stable keys and keep error types`() {
        val rows = AutomationPresentation.executionRows(listOf(
            ExecutionRecord("r1", "set_dnd_on", ExecutionStatus.EXECUTED),
            ExecutionRecord("r2", "set_dnd_on", ExecutionStatus.FAILED, "dnd-policy-access-denied")))
        assertEquals("exec_executed", rows[0].statusKey)
        assertEquals(null, rows[0].errorType)
        assertEquals("exec_failed", rows[1].statusKey)
        assertEquals("dnd-policy-access-denied", rows[1].errorType)
    }

    @Test
    fun `dry run report lists one row per decision in order`() {
        val rules = listOf(rule, rule.copy(id = "aaa"))
        val decisions = rules.map {
            Evaluator.evaluate(it, EventContext(600, 0, 10, false, true, 0))
        }
        val report = AutomationPresentation.dryRunReport(decisions)
        assertEquals(listOf("quiet_hours", "aaa"), report.map { it.ruleId })
        assertEquals(listOf(DecisionReason.FIRED, DecisionReason.FIRED),
            decisions.map { it.reason })
    }
}
