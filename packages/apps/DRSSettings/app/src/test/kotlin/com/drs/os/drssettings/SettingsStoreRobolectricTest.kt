/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS Settings — Robolectric no-device CI layer (Phase 5).
 *
 * Persistence tests through the REAL SharedPreferences implementation
 * (AndroidSettingsByteStore) and the real settings-core repository:
 * durable commit, round-trip, Arabic defaults on a fresh install, and
 * structural-corruption discard semantics (no partial salvage).
 *
 * Honest scope note: JVM-simulated Android (Robolectric), not a device.
 */
package com.drs.os.drssettings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.drs.os.settings.AppLanguage
import com.drs.os.settings.DrsSettings
import com.drs.os.settings.SaveResult
import com.drs.os.settings.SettingsRepository
import com.drs.os.settings.SettingsSource
import com.drs.os.settings.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SettingsStoreRobolectricTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    fun resetStore() {
        // Test isolation: start every test from a clean prefs file.
        assertTrue(
            context.getSharedPreferences("drs_settings", Context.MODE_PRIVATE)
                .edit().clear().commit()
        )
    }

    @Test
    fun freshEmptyStore_loadsArabicDefaults() {
        val loaded = SettingsRepository(AndroidSettingsByteStore(context)).load()
        assertEquals(SettingsSource.FRESH, loaded.source)
        // The Arabic-default requirement, verified on the real storage path.
        assertEquals(AppLanguage.ARABIC, loaded.settings.language)
        assertEquals(ThemeMode.SYSTEM, loaded.settings.theme)
        assertEquals(false, loaded.settings.automationMasterEnabled)
        assertEquals(true, loaded.settings.automationDryRunDefault)
    }

    @Test
    fun commitWriteIsDurable_andRoundTripsThroughRealPreferences() {
        val repository = SettingsRepository(AndroidSettingsByteStore(context))
        val toSave = DrsSettings(
            language = AppLanguage.ENGLISH,
            theme = ThemeMode.DARK,
            automationMasterEnabled = true,
            automationDryRunDefault = false,
            securityShortcutsEnabled = false
        )
        // SaveResult.Saved means the commit() write reported durability.
        assertEquals(SaveResult.Saved, repository.save(toSave))

        val reloaded = SettingsRepository(AndroidSettingsByteStore(context)).load()
        assertEquals(SettingsSource.FRESH, reloaded.source)
        assertEquals(toSave, reloaded.settings)
    }

    @Test
    fun structurallyCorruptedDocument_isDiscardedAndFlagged_neverPartiallySalvaged() {
        // Seed REAL preferences with a structurally invalid document
        // (no '=' separator — the foundation KvStore codec rejects it).
        assertTrue(
            context.getSharedPreferences("drs_settings", Context.MODE_PRIVATE)
                .edit().putString("settings_v1", "garbage-without-separator").commit()
        )

        val loaded = SettingsRepository(AndroidSettingsByteStore(context)).load()
        assertEquals(SettingsSource.RESTORED_FROM_CORRUPTED, loaded.source)
        // Whole-document discard: defaults come back, nothing partial.
        assertEquals(DrsSettings(), loaded.settings)
        assertTrue(loaded.recoveries.contains("corrupted-document"))
    }
}
