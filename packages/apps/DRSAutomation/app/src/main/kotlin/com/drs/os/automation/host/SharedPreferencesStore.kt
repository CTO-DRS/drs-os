/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS Automation — SharedPreferences-backed key/value store.
 *
 * Phase 4 defect fix: MainActivity referenced this class but it was
 * never present in the Phase 3 source (the tree had never been
 * compiled; caught by the first real Gradle build).
 *
 * commit() (not apply()) is used deliberately: writes are synchronous
 * so the boolean result honestly reports durability, which the
 * AutomationKeyValueStore contract requires; rule persistence is small
 * and infrequent, so the UI-thread cost is negligible.
 */
package com.drs.os.automation.host

import android.content.Context
import android.content.SharedPreferences

class SharedPreferencesStore(
    context: Context,
    name: String
) : AutomationKeyValueStore {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(name, Context.MODE_PRIVATE)

    override fun read(key: String): String? = prefs.getString(key, null)

    override fun write(key: String, value: String): Boolean =
        prefs.edit().putString(key, value).commit()
}
