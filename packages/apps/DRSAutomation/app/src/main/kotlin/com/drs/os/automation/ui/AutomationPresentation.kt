/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS Automation — pure presentation mapping (JVM-testable, no Android
 * imports). Produces UI models with stable string keys; translations
 * bind in resources (ar default, values-en).
 */
package com.drs.os.automation.ui

import com.drs.os.engine.ActionRequest
import com.drs.os.engine.AuditRecord
import com.drs.os.engine.Decision
import com.drs.os.engine.DecisionReason
import com.drs.os.engine.Rule
import com.drs.os.engine.host.ExecutionRecord
import com.drs.os.engine.host.ExecutionStatus

object AutomationPresentation {

    data class RuleRow(
        val id: String,
        val name: String,
        val enabled: Boolean,
        val triggerKey: String,
        val triggerDetail: String,
        val actionId: String,
        val explanation: String,
        val invalidReasons: List<String>
    )

    fun ruleRows(rules: List<Rule>): List<RuleRow> = rules
        .sortedBy { it.id }
        .map { r ->
            RuleRow(
                id = r.id,
                name = r.name,
                enabled = r.enabled,
                triggerKey = when (r.trigger) {
                    is com.drs.os.engine.Trigger.TimeDaily -> "trigger_time_daily"
                    is com.drs.os.engine.Trigger.BatteryBelow -> "trigger_battery_below"
                    is com.drs.os.engine.Trigger.PowerConnected -> "trigger_power_connected"
                },
                triggerDetail = when (val t = r.trigger) {
                    is com.drs.os.engine.Trigger.TimeDaily -> t.time
                    is com.drs.os.engine.Trigger.BatteryBelow -> t.threshold.toString()
                    is com.drs.os.engine.Trigger.PowerConnected ->
                        if (t.state) "true" else "false"
                },
                actionId = r.actionId,
                explanation = r.explanation,
                invalidReasons = com.drs.os.engine.validateRule(r).map { it.toString() }
            )
        }

    data class DecisionRow(
        val ruleId: String,
        val reasonKey: String,
        val reasonDetail: String
    )

    fun decisionRow(decision: Decision): DecisionRow = DecisionRow(
        ruleId = decision.ruleId,
        reasonKey = "reason_${decision.reason.wire.replace('-', '_')}",
        reasonDetail = when (decision.reason) {
            DecisionReason.FIRED -> decision.actionId ?: ""
            else -> ""
        }
    )

    fun dryRunReport(decisions: List<Decision>): List<DecisionRow> =
        decisions.map { decisionRow(it) }

    data class ExecutionRow(
        val ruleId: String,
        val actionId: String,
        val statusKey: String,
        val errorType: String?
    )

    fun executionRows(records: List<ExecutionRecord>): List<ExecutionRow> =
        records.map {
            ExecutionRow(
                ruleId = it.ruleId,
                actionId = it.actionId,
                statusKey = "exec_${it.status.name.lowercase()}",
                errorType = it.errorType
            )
        }

    data class AuditRow(
        val ruleId: String,
        val reasonKey: String,
        val timestamp: String
    )

    fun auditRows(records: List<AuditRecord>): List<AuditRow> = records
        .map { AuditRow(it.ruleId, "reason_${it.reason.replace('-', '_')}", it.timestamp) }
}
