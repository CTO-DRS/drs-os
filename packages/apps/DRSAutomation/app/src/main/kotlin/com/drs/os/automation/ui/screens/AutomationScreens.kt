/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS Automation UI: rules list, rule detail (dry run / run now /
 * enable / delete), rule editor (only triggers the app actually
 * supports), audit view. Every control works; unsupported things are
 * labeled, not displayed as clickable fakes.
 */
package com.drs.os.automation.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.drs.os.automation.R
import com.drs.os.automation.ui.AutomationPresentation
import com.drs.os.automation.ui.AutomationViewModel

@Composable
fun AutomationApp(state: AutomationViewModel.UiState, callbacks: AutomationViewModel.Callbacks) {
    var screen by remember { mutableStateOf("rules") }
    var selectedRule by remember { mutableStateOf<String?>(null) }
    var editorOpen by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        when {
            editorOpen -> RuleEditorScreen(
                onSave = { errors ->
                    val result = callbacks.onSaveDraft(errors)
                    if (result.isEmpty()) editorOpen = false
                    result
                },
                onCancel = { editorOpen = false }
            )
            selectedRule != null -> RuleDetailScreen(
                row = state.ruleRows.firstOrNull { it.id == selectedRule },
                lastDryRun = state.lastDryRun,
                lastExecutions = state.lastExecutions,
                callbacks = callbacks,
                onBack = { selectedRule = null }
            )
            screen == "rules" -> RulesListScreen(
                state = state,
                onOpenRule = { selectedRule = it },
                onNewRule = { editorOpen = true },
                onOpenAudit = { screen = "audit" }
            )
            else -> AuditScreen(
                rows = state.lastAudit,
                onBack = { screen = "rules" }
            )
        }
    }
}

@Composable
private fun RulesListScreen(
    state: AutomationViewModel.UiState,
    onOpenRule: (String) -> Unit,
    onNewRule: () -> Unit,
    onOpenAudit: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium)
        if (state.storeCorrupted) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.warn_store_corrupted),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        Button(onClick = onNewRule, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_new_rule))
        }
        OutlinedButton(onClick = onOpenAudit, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.action_open_audit))
        }
        if (state.ruleRows.isEmpty()) {
            Text(stringResource(R.string.empty_rules), style = MaterialTheme.typography.bodyLarge)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(state.ruleRows, key = { it.id }) { row ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(row.name, style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.weight(1f))
                                Text(
                                    stringResource(
                                        if (row.enabled) R.string.state_enabled
                                        else R.string.state_disabled),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                            Text(
                                text = stringResource(
                                    when (row.triggerKey) {
                                        "trigger_battery_below" -> R.string.trigger_battery_below
                                        "trigger_power_connected" -> R.string.trigger_power_connected
                                        else -> R.string.trigger_time_daily
                                    }) + " · " + row.triggerDetail,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = stringResource(R.string.action_label) + ": " + row.actionId,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { onOpenRule(row.id) }) {
                                    Text(stringResource(R.string.action_details))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleDetailScreen(
    row: AutomationPresentation.RuleRow?,
    lastDryRun: List<AutomationPresentation.DecisionRow>,
    lastExecutions: List<AutomationPresentation.ExecutionRow>,
    callbacks: AutomationViewModel.Callbacks,
    onBack: () -> Unit
) {
    if (row == null) {
        Text(stringResource(R.string.error_rule_missing))
        return
    }
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            OutlinedButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
        }
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(row.name, style = MaterialTheme.typography.headlineMedium)
                    Text(row.explanation, style = MaterialTheme.typography.bodyLarge)
                    if (row.invalidReasons.isNotEmpty()) {
                        Text(
                            stringResource(R.string.error_validation),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { callbacks.onDryRun(row.id) }) {
                    Text(stringResource(R.string.action_dry_run))
                }
                OutlinedButton(onClick = { callbacks.onRunNow(row.id) }) {
                    Text(stringResource(R.string.action_run_now))
                }
                OutlinedButton(onClick = {
                    callbacks.onSetEnabled(row.id, !row.enabled)
                }) {
                    Text(stringResource(
                        if (row.enabled) R.string.action_disable else R.string.action_enable))
                }
                OutlinedButton(onClick = { callbacks.onDelete(row.id); onBack() }) {
                    Text(stringResource(R.string.action_delete))
                }
            }
        }
        if (lastDryRun.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.dry_run_title), style = MaterialTheme.typography.titleLarge)
                        lastDryRun.forEach {
                            Text(stringResource(reasonRes(it.reasonKey)) + " " + it.reasonDetail,
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        if (lastExecutions.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(stringResource(R.string.executions_title), style = MaterialTheme.typography.titleLarge)
                        lastExecutions.forEach {
                            Text(
                                it.actionId + " — " + stringResource(statusRes(it.statusKey)) +
                                    (it.errorType?.let { e -> " ($e)" } ?: ""),
                                style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RuleEditorScreen(
    onSave: (AutomationViewModel.EditorDraft) -> List<Int>,
    onCancel: () -> Unit
) {
    var id by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var explanation by remember { mutableStateOf("") }
    var triggerKind by remember { mutableStateOf("battery_below") }
    var threshold by remember { mutableStateOf("20") }
    var powerState by remember { mutableStateOf(true) }
    var maxRuns by remember { mutableStateOf("10") }
    var requiresUnlock by remember { mutableStateOf(true) }
    var actionId by remember { mutableStateOf("set_dnd_on") }
    var errorKeys by remember { mutableStateOf<List<Int>>(emptyList()) }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text(stringResource(R.string.editor_title), style = MaterialTheme.typography.headlineMedium)
        }
        item {
            OutlinedTextField(
                value = id, onValueChange = { id = it },
                label = { Text(stringResource(R.string.field_rule_id)) },
                modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text(stringResource(R.string.field_rule_name)) },
                modifier = Modifier.fillMaxWidth())
        }
        item {
            OutlinedTextField(
                value = explanation, onValueChange = { explanation = it },
                label = { Text(stringResource(R.string.field_explanation)) },
                modifier = Modifier.fillMaxWidth())
        }
        item {
            Text(stringResource(R.string.field_trigger), style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.FilterChip(
                    selected = triggerKind == "battery_below",
                    onClick = { triggerKind = "battery_below" },
                    label = { Text(stringResource(R.string.trigger_battery_below)) })
                androidx.compose.material3.FilterChip(
                    selected = triggerKind == "power_connected",
                    onClick = { triggerKind = "power_connected" },
                    label = { Text(stringResource(R.string.trigger_power_connected)) })
            }
            // TIME_DAILY is deliberately NOT offered: without a platform
            // scheduler it would be a control that does nothing.
            Text(
                stringResource(R.string.trigger_time_daily_planned),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (triggerKind == "battery_below") {
            item {
                OutlinedTextField(
                    value = threshold, onValueChange = { threshold = it },
                    label = { Text(stringResource(R.string.field_threshold_1_99)) },
                    modifier = Modifier.fillMaxWidth())
            }
        }
        item {
            Text(stringResource(R.string.field_action), style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                androidx.compose.material3.FilterChip(
                    selected = actionId == "set_dnd_on",
                    onClick = { actionId = "set_dnd_on" },
                    label = { Text("set_dnd_on") })
                androidx.compose.material3.FilterChip(
                    selected = actionId == "set_theme_dark",
                    onClick = { actionId = "set_theme_dark" },
                    label = { Text("set_theme_dark") })
                androidx.compose.material3.FilterChip(
                    selected = actionId == "set_theme_light",
                    onClick = { actionId = "set_theme_light" },
                    label = { Text("set_theme_light") })
            }
        }
        item {
            OutlinedTextField(
                value = maxRuns, onValueChange = { maxRuns = it },
                label = { Text(stringResource(R.string.field_max_runs_per_day)) },
                modifier = Modifier.fillMaxWidth())
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = requiresUnlock, onCheckedChange = { requiresUnlock = it })
                Text(stringResource(R.string.field_requires_unlock))
            }
        }
        if (errorKeys.isNotEmpty()) {
            item {
                Text(
                    errorKeys.joinToString("\n") { stringResource(it) },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = {
                    errorKeys = onSave(
                        AutomationViewModel.EditorDraft(
                            id = id, name = name, explanation = explanation,
                            actionId = actionId,
                            triggerKind = triggerKind,
                            batteryThreshold = threshold.toIntOrNull() ?: 0,
                            powerConnected = powerState,
                            requiresUnlock = requiresUnlock,
                            maxRunsPerDay = maxRuns.toIntOrNull() ?: 0
                        )
                    )
                }) { Text(stringResource(R.string.action_save)) }
                OutlinedButton(onClick = onCancel) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        }
    }
}

@Composable
private fun AuditScreen(rows: List<AutomationPresentation.AuditRow>, onBack: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onBack) { Text(stringResource(R.string.action_back)) }
        Text(stringResource(R.string.audit_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.audit_scope_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (rows.isEmpty()) {
            Text(stringResource(R.string.empty_audit))
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(rows, key = { it.timestamp + it.ruleId + it.reasonKey }) { row ->
                    Text(
                        row.timestamp + " · " + row.ruleId + " · " +
                            stringResource(reasonRes(row.reasonKey)),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun reasonRes(key: String): Int = when (key) {
    "reason_fired" -> R.string.reason_fired
    "reason_rule_disabled" -> R.string.reason_rule_disabled
    "reason_trigger_not_active" -> R.string.reason_trigger_not_active
    "reason_condition_failed" -> R.string.reason_condition_failed
    "reason_requires_unlock" -> R.string.reason_requires_unlock
    else -> R.string.reason_max_runs_reached
}

@Composable
private fun statusRes(key: String): Int = when (key) {
    "exec_executed" -> R.string.exec_executed
    "exec_failed" -> R.string.exec_failed
    "exec_cancelled" -> R.string.exec_cancelled
    "exec_skipped_limit" -> R.string.exec_skipped_limit
    else -> R.string.exec_no_executor
}
