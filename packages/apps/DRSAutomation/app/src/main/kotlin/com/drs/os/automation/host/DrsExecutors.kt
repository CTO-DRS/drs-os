/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS Automation — host executors. Every action id from the CLOSED
 * engine registry has an executor here OR is explicitly documented as
 * platform-blocked. There is no silent mapping between registry ids
 * and runtime behavior.
 *
 * Honesty contract (v1, standalone app):
 *  * theme actions work for THIS app via UiModeManager (public API);
 *  * DND actions require notification-policy access; without it the
 *    executor reports a stable failure and the UI explains how to grant;
 *  * battery-saver has NO public third-party API on Android — the
 *    executor reports platform-api-unavailable instead of pretending;
 *    the real implementation is a platform-integration task.
 */
package com.drs.os.automation.host

import android.app.UiModeManager
import android.content.Context
import com.drs.os.engine.ActionRequest
import com.drs.os.engine.host.ActionExecutor
import com.drs.os.engine.host.CancellationToken
import com.drs.os.engine.host.ExecutionResult

class ThemeActionExecutor(private val context: Context) : ActionExecutor {

    private fun uiMode(): UiModeManager =
        context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager

    override val actionId: String = "set_theme_dark"

    override fun execute(request: ActionRequest, token: CancellationToken): ExecutionResult {
        if (token.cancelled) return ExecutionResult.Failed("cancelled-before-start")
        uiMode().setApplicationNightMode(UiModeManager.MODE_NIGHT_YES)
        return ExecutionResult.Executed
    }
}

class ThemeLightActionExecutor(private val context: Context) : ActionExecutor {

    override val actionId: String = "set_theme_light"

    override fun execute(request: ActionRequest, token: CancellationToken): ExecutionResult {
        if (token.cancelled) return ExecutionResult.Failed("cancelled-before-start")
        (context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager)
            .setApplicationNightMode(UiModeManager.MODE_NIGHT_NO)
        return ExecutionResult.Executed
    }
}

class SetDndOnExecutor(private val context: Context) : ActionExecutor {

    override val actionId: String = "set_dnd_on"

    override fun execute(request: ActionRequest, token: CancellationToken): ExecutionResult {
        if (token.cancelled) return ExecutionResult.Failed("cancelled-before-start")
        return try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as android.app.NotificationManager
            nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            ExecutionResult.Executed
        } catch (se: SecurityException) {
            ExecutionResult.Failed("dnd-policy-access-denied")
        } catch (e: Exception) {
            ExecutionResult.Failed("platform-api-unavailable")
        }
    }
}

class SetDndOffExecutor(private val context: Context) : ActionExecutor {

    override val actionId: String = "set_dnd_off"

    override fun execute(request: ActionRequest, token: CancellationToken): ExecutionResult {
        if (token.cancelled) return ExecutionResult.Failed("cancelled-before-start")
        return try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE)
                as android.app.NotificationManager
            nm.setInterruptionFilter(android.app.NotificationManager.INTERRUPTION_FILTER_ALL)
            ExecutionResult.Executed
        } catch (se: SecurityException) {
            ExecutionResult.Failed("dnd-policy-access-denied")
        } catch (e: Exception) {
            ExecutionResult.Failed("platform-api-unavailable")
        }
    }
}

/**
 * Android exposes no public API for a third-party app to toggle battery
 * saver. The honest executor records a stable failure; the real
 * implementation requires platform integration (privileged caller).
 */
class BatterySaverExecutor(private val enable: Boolean) : ActionExecutor {

    override val actionId: String = if (enable) "enable_battery_saver" else "disable_battery_saver"

    override fun execute(request: ActionRequest, token: CancellationToken): ExecutionResult {
        if (token.cancelled) return ExecutionResult.Failed("cancelled-before-start")
        return ExecutionResult.Failed("platform-api-unavailable")
    }
}

object DrsExecutors {
    fun all(context: Context): Map<String, ActionExecutor> {
        val list: List<ActionExecutor> = listOf(
            ThemeActionExecutor(context),
            ThemeLightActionExecutor(context),
            SetDndOnExecutor(context),
            SetDndOffExecutor(context),
            BatterySaverExecutor(enable = true),
            BatterySaverExecutor(enable = false)
        )
        return list.associateBy { it.actionId }
    }
}
