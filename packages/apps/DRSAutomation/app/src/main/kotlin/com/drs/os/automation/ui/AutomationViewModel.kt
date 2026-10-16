/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Automation ViewModel: owns evaluation cycles, dry-run, manual
 * execution and persistence. All decision logic lives in the tested
 * engine-core; this class only orchestrates and maps to UI state.
 */
package com.drs.os.automation.ui

import androidx.lifecycle.ViewModel
import com.drs.os.automation.BatteryState
import com.drs.os.automation.EngineHolder
import com.drs.os.automation.R
import com.drs.os.engine.ActionRequest
import com.drs.os.engine.Decision
import com.drs.os.engine.Rule
import com.drs.os.engine.Trigger
import com.drs.os.engine.host.ActionExecutor
import com.drs.os.engine.host.CancellationToken
import com.drs.os.engine.host.ExecutionCoordinator
import com.drs.os.engine.host.ExecutionRecord
import com.drs.os.engine.host.HostRuntime
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Calendar

class AutomationViewModel(
    private val holder: EngineHolder,
    private val executors: Map<String, ActionExecutor>,
    private val batteryProbe: () -> BatteryState
) : ViewModel() {

    data class UiState(
        val ruleRows: List<AutomationPresentation.RuleRow> = emptyList(),
        val storeCorrupted: Boolean = false,
        val lastDryRun: List<AutomationPresentation.DecisionRow> = emptyList(),
        val lastExecutions: List<AutomationPresentation.ExecutionRow> = emptyList(),
        val lastAudit: List<AutomationPresentation.AuditRow> = emptyList(),
        val battery: BatteryState? = null
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState

    private fun publish(transform: (UiState) -> UiState) {
        _uiState.value = transform(_uiState.value.copy())
            .copy(
                ruleRows = AutomationPresentation.ruleRows(holder.rules()),
                lastAudit = AutomationPresentation.auditRows(holder.stateStore.auditLog())
            )
    }

    init {
        publish { it.copy(storeCorrupted = holder.loadedCorrupted) }
    }

    fun onBatteryStateChanged() {
        publish { it.copy(battery = batteryProbe()) }
        evaluateLevelTriggers()
    }

    /**
     * Live evaluation of level triggers (battery/charging). Manual and
     * daily triggers are intentionally NOT evaluated here.
     */
    private fun evaluateLevelTriggers() {
        val battery = batteryProbe()
        val (epochDay, minuteOfDay, weekday) = currentWallClock()
        val allRequests = ArrayList<ActionRequest>()
        for (rule in holder.rules()) {
            val active = when (rule.trigger) {
                is Trigger.BatteryBelow, is Trigger.PowerConnected -> true
                else -> false
            }
            if (!active) continue
            val evaluation = HostRuntime.evaluateWithHost(
                rule = rule,
                epochDay = epochDay,
                minuteOfDay = minuteOfDay,
                weekday = weekday,
                battery = battery.percent,
                charging = battery.charging,
                deviceUnlocked = true,   // foreground app: user is present
                timestamp = isoTimestamp(),
                state = holder.stateStore
            )
            allRequests.addAll(evaluation.actionRequests)
        }
        if (allRequests.isNotEmpty()) {
            execute(allRequests)
        }
        holder.stateStore.let { if (it is com.drs.os.automation.PrefsHostStateStore) it.persist() }
    }

    /** Manual "run now": evaluates ONE rule immediately and executes. */
    fun runNow(ruleId: String) {
        val rule = holder.rules().firstOrNull { it.id == ruleId } ?: return
        val battery = batteryProbe()
        val (epochDay, minuteOfDay, weekday) = currentWallClock()
        val evaluation = HostRuntime.evaluateWithHost(
            rule = rule,
            epochDay = epochDay,
            minuteOfDay = minuteOfDay,
            weekday = weekday,
            battery = battery.percent,
            charging = battery.charging,
            deviceUnlocked = true,
            timestamp = isoTimestamp(),
            state = holder.stateStore
        )
        execute(evaluation.actionRequests)
        holder.stateStore.let { if (it is com.drs.os.automation.PrefsHostStateStore) it.persist() }
    }

    /** Dry run NEVER executes and NEVER touches host state. */
    fun dryRun(ruleId: String) {
        val rule = holder.rules().firstOrNull { it.id == ruleId } ?: return
        val battery = batteryProbe()
        val (_, minuteOfDay, weekday) = currentWallClock()
        val decision = com.drs.os.engine.Evaluator.evaluate(
            rule,
            com.drs.os.engine.EventContext(
                nowMinutes = minuteOfDay,
                weekday = weekday,
                battery = battery.percent,
                charging = battery.charging,
                deviceUnlocked = true,
                runsToday = holder.stateStore.runsToday(rule.id, epochDay())
            )
        )
        publish { it.copy(lastDryRun = listOf(AutomationPresentation.decisionRow(decision))) }
    }

    private fun execute(requests: List<ActionRequest>) {
        val records: List<ExecutionRecord> = ExecutionCoordinator.run(
            requests, executors, CancellationToken())
        publish { it.copy(lastExecutions = AutomationPresentation.executionRows(records)) }
    }

    // ------------------------------------------------------ editor wiring

    data class EditorDraft(
        val id: String = "",
        val name: String = "",
        val explanation: String = "",
        val actionId: String = "",
        val triggerKind: String = "manual",
        val batteryThreshold: Int = 20,
        val powerConnected: Boolean = true,
        val requiresUnlock: Boolean = true,
        val maxRunsPerDay: Int = 10
    )

    /**
     * Creates a rule from the editor draft. triggerKind "manual" and
     * "time_daily" are rejected with a reason key: manual needs no
     * stored trigger (run now is per-rule), time_daily is not yet
     * supported without a scheduler. Returns error KEYS (empty = saved).
     */
    fun saveDraft(draft: EditorDraft): List<Int> {
        val errors = ArrayList<Int>()
        if (draft.id.isBlank() || draft.name.isBlank() || draft.explanation.isBlank()) {
            errors.add(R.string.error_required_fields)
        }
        val trigger: Trigger = when (draft.triggerKind) {
            "battery_below" -> Trigger.BatteryBelow(draft.batteryThreshold)
            "power_connected" -> Trigger.PowerConnected(draft.powerConnected)
            "manual" -> return listOf(R.string.error_manual_needs_no_trigger)
            "time_daily" -> return listOf(R.string.error_time_daily_planned)
            else -> return listOf(R.string.error_invalid_trigger)
        }
        if (errors.isNotEmpty()) return errors
        val rule = Rule(
            id = draft.id.trim(),
            name = draft.name.trim(),
            enabled = true,
            trigger = trigger,
            conditions = emptyList(),
            actionId = draft.actionId,
            requiresUnlock = draft.requiresUnlock,
            maxRunsPerDay = draft.maxRunsPerDay,
            explanation = draft.explanation.trim()
        )
        val saveErrors = holder.save(rule)
        if (saveErrors.isEmpty()) {
            publish { it }
        } else {
            errors.add(R.string.error_validation)
        }
        return errors
    }

    fun setEnabled(ruleId: String, enabled: Boolean) {
        holder.setEnabled(ruleId, enabled)
        publish { it }
    }

    fun delete(ruleId: String) {
        holder.delete(ruleId)
        publish { it }
    }

    // ------------------------------------------------------------ helpers

    private data class WallClock(val epochDay: Long, val minuteOfDay: Int, val weekday: Int)

    private fun currentWallClock(): WallClock {
        val cal = Calendar.getInstance()
        val minuteOfDay = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        // Calendar.DAY_OF_WEEK: SUNDAY=1..SATURDAY=7 → ISO: MONDAY=0..SUNDAY=6
        val weekday = ((cal.get(Calendar.DAY_OF_WEEK) + 5) % 7)
        return WallClock(cal.getTimeInMillis() / 86_400_000L, minuteOfDay, weekday)
    }

    private fun epochDay(): Long = currentWallClock().epochDay

    private fun isoTimestamp(): String {
        val cal = Calendar.getInstance()
        fun p(n: Int) = n.toString().padStart(2, '0')
        return "%04d-%02d-%02dT%02d:%02d:%02dZ".format(
            cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH), cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE), cal.get(Calendar.SECOND))
    }

    /**
     * Screen callbacks bound by the Compose layer. Kept as a data class
     * so the wiring is explicit and testable — no anonymous lambdas
     * hidden in composables.
     */
    data class Callbacks(
        val onRunNow: (String) -> Unit,
        val onDryRun: (String) -> Unit,
        val onSetEnabled: (String, Boolean) -> Unit,
        val onDelete: (String) -> Unit,
        val onSaveDraft: (EditorDraft) -> List<Int>,
        val onRefreshBattery: () -> Unit
    )

    val callbacks = Callbacks(
        onRunNow = ::runNow,
        onDryRun = ::dryRun,
        onSetEnabled = ::setEnabled,
        onDelete = ::delete,
        onSaveDraft = ::saveDraft,
        onRefreshBattery = ::onBatteryStateChanged
    )
}
