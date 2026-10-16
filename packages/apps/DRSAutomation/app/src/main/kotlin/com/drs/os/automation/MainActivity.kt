/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS Automation — main activity and engine wiring.
 *
 * v1 trigger support (honest scope):
 *  * MANUAL — "Run now" per rule (the device is unlocked by definition:
 *    the user is interacting with the app);
 *  * BATTERY_BELOW / POWER_CONNECTED — level triggers evaluated live
 *    from the permissionless battery sticky broadcast while the app is
 *    in the foreground;
 *  * TIME_DAILY — visible as "planned" in the editor but NOT creatable:
 *    an edge trigger needs a platform scheduler (WorkManager integration
 *    is a documented next step), and shipping a control that silently
 *    does nothing is prohibited by the project rules.
 */
package com.drs.os.automation

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.drs.os.automation.host.AutomationKeyValueStore
import com.drs.os.automation.host.DrsExecutors
import com.drs.os.automation.host.SharedPreferencesStore
import com.drs.os.automation.ui.AutomationViewModel
import com.drs.os.automation.ui.screens.AutomationApp
import com.drs.os.automation.ui.theme.DrsTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: AutomationViewModel

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            viewModel.onBatteryStateChanged()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val kv: AutomationKeyValueStore =
            SharedPreferencesStore(applicationContext, "drs_automation")
        val holder = EngineHolder(kv)
        viewModel = AutomationViewModel(
            holder = holder,
            executors = DrsExecutors.all(applicationContext),
            batteryProbe = { readBatteryState() }
        )
        setContent {
            DrsTheme {
                val state by viewModel.uiState.collectAsState()
                AutomationApp(state = state, callbacks = viewModel.callbacks)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        registerReceiver(batteryReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        viewModel.onBatteryStateChanged()
    }

    override fun onPause() {
        super.onPause()
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: IllegalArgumentException) {
            Log.w("DRSAutomation", "battery receiver not registered", e)
        }
    }

    private fun readBatteryState(): BatteryState {
        val intent = registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return BatteryState(level = 100, scale = 100, charging = false)
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
            status == BatteryManager.BATTERY_STATUS_FULL
        return BatteryState(
            level = if (level >= 0) level else 0,
            scale = if (scale > 0) scale else 100,
            charging = charging
        )
    }
}

data class BatteryState(val level: Int, val scale: Int, val charging: Boolean) {
    val percent: Int get() = (level * 100) / scale
}
