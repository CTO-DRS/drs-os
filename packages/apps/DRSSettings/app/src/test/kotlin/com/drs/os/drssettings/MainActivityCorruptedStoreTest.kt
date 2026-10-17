/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS Settings — Robolectric no-device CI layer (Phase 5).
 *
 * End-to-end corrupted-store recovery through the REAL launch path:
 * a test-only Application seeds REAL SharedPreferences with a
 * structurally invalid document; MainActivity must discard the whole
 * document (settings-core policy: no partial salvage), start from
 * Arabic defaults, and render the recovery banner.
 *
 * Honest scope note: JVM-simulated Android (Robolectric), not a device.
 */
package com.drs.os.drssettings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(
    sdk = [29],
    qualifiers = "w1280dp-h2000dp",
    application = CorruptedSeedApplication::class
)
class MainActivityCorruptedStoreTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Test
    fun corruptedRealStore_recoversToArabicDefaults_andShowsBanner() {
        rule.waitForIdle()

        // The banner text, rendered from the recovered state:
        rule.onNodeWithText(
            "تمت استعادة الإعدادات الافتراضية لأن البيانات المحفوظة كانت تالفة."
        ).assertIsDisplayed()

        // Recovery to ARABIC DEFAULTS: the Arabic title renders and the
        // automation master shows its default OFF state ("غير مسموح").
        rule.onNodeWithText("إعدادات DRS OS").assertIsDisplayed()
        rule.onNodeWithText("السماح بقواعد الأتمتة — غير مسموح").assertIsDisplayed()
    }
}
