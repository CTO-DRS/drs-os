/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS Settings — Robolectric no-device CI layer (Phase 5).
 *
 * Fresh-launch flows end-to-end through the REAL launch path and REAL
 * SharedPreferences: the automation master toggle must persist through
 * the settings-core codec (commit() durability), and the privacy
 * section must honestly show the "Security Center not installed"
 * notice instead of a dead button (fixture condition: the package
 * com.drs.os.securitycenter does not exist in this environment).
 *
 * Honest scope note: JVM-simulated Android (Robolectric), not a device.
 */
package com.drs.os.drssettings

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.drs.os.settings.SettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [29], qualifiers = "w1280dp-h2000dp")
class MainActivityFreshLaunchTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun resetRealStore() {
        // Test isolation on the REAL prefs file the app uses.
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        assertTrue(
            ctx.getSharedPreferences("drs_settings", Context.MODE_PRIVATE)
                .edit().clear().commit()
        )
    }

    @Test
    fun automationMasterToggle_persistsThroughRealStore_endToEnd() {
        rule.waitForIdle()
        // Wait for a displayed-sized instance (fresh-launch AppCompat
        // recreation dynamics can detach the first composition).
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithText("السماح بقواعد الأتمتة — غير مسموح")
                .fetchSemanticsNodes()
                .any { it.boundsInRoot.width > 0f && it.boundsInRoot.height > 0f }
        }

        // Default is OFF. The whole row is the click target since the
        // Phase 5 a11y fix (F-1).
        rule.onNodeWithText("السماح بقواعد الأتمتة — غير مسموح").performClick()
        rule.waitForIdle()

        // UI reflects the new state…
        rule.onNodeWithText("السماح بقواعد الأتمتة — مسموح").assertIsDisplayed()

        // …and the value REALLY persisted: a brand-new repository over
        // REAL SharedPreferences decodes automation_master_enabled=true.
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val reloaded = SettingsRepository(AndroidSettingsByteStore(ctx)).load()
        assertEquals(true, reloaded.settings.automationMasterEnabled)
    }

    @Test
    fun privacySection_whenSecurityCenterNotInstalled_showsMissingNotice() {
        rule.waitForIdle()
        // Fixture: in the Robolectric environment the package
        // com.drs.os.securitycenter is NOT installed, so the section must
        // show the explicit "not installed" notice instead of a dead
        // button — the honest-UI requirement from the app spec.
        rule.onNodeWithText(
            "تطبيق مركز أمان DRS غير مثبّت على هذا الجهاز."
        ).assertIsDisplayed()
    }
}
