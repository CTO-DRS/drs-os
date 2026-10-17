/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS Settings — test-only Application that seeds REAL SharedPreferences
 * with a structurally corrupted settings document before any activity
 * starts. Used by MainActivityCorruptedStoreTest to exercise the
 * corrupted-store recovery path end-to-end (MainActivity load ->
 * settings-core decode -> recovery banner rendering).
 */
package com.drs.os.drssettings

import android.app.Application
import android.content.Context

class CorruptedSeedApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        getSharedPreferences("drs_settings", Context.MODE_PRIVATE)
            .edit()
            .putString("settings_v1", "garbage-without-separator")
            .commit()
    }
}
