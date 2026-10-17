/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS Settings — Robolectric no-device CI layer (Phase 5).
 *
 * Activity-level smoke tests through the REAL AppCompatDelegate
 * per-app-locale pipeline: launch, Arabic default, RTL layout
 * geometry, English switch to LTR.
 *
 * Layout direction is asserted BEHAVIORALLY: the screen title Text node
 * is anchored near the right edge of the root under RTL and near the
 * left edge under LTR (the title is an intrinsic-width child of a
 * full-width, non-RTL-aware Column, so its x-position is direction-
 * observable). The activity's real locale configuration is asserted as
 * well.
 *
 * Honest scope note: these run on a JVM-simulated Android (Robolectric),
 * NOT on a device/emulator. Device runtime verification remains
 * BLOCKED (no KVM in this sandbox) — see docs/phase4/TEST_RESULTS.md
 * §4 and docs/phase5/PHASE5_REPORT.md.
 */
package com.drs.os.drssettings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.core.os.LocaleListCompat
import androidx.appcompat.app.AppCompatDelegate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
// SDK 29 (the app minSdk) is deliberate: on SDK >= 33 Robolectric's
// LocaleManager shadow stores per-app locales but never updates the
// activity configuration, so the appcompat pre-T self-managed pipeline
// (real recreation + config application) is the only fully observable
// path under Robolectric. On a real API 33+ device the same
// setApplicationLocales call delegates to the platform LocaleManager.
@Config(sdk = [29], qualifiers = "w1280dp-h2000dp")
class MainActivityLocaleTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    @Before
    fun preSetArabicLocale() {
        // Deterministic start: set the per-app locale BEFORE the activity
        // launches, so MainActivity's own applyLanguage(ARABIC) is a no-op
        // and no mid-onCreate recreation race can detach the first
        // composition. (On a truly fresh store the recreation happens and
        // is covered end-to-end in MainActivityCorruptedStoreTest /
        // MainActivityFreshLaunchTest, which wait for displayed sizes.)
        AppCompatDelegate.setApplicationLocales(
            LocaleListCompat.forLanguageTags("ar"))
    }

    private fun titleStartX(text: String): Float {
        val title = rule.onNodeWithText(text).fetchSemanticsNode()
        return title.positionInRoot.x
    }

    private fun rootWidth(): Float =
        rule.onRoot().fetchSemanticsNode().boundsInRoot.width

    @Test
    fun freshLaunch_rendersArabicTitle_anchoredRight_inRtl() {
        // Arabic is the DEFAULT language; MainActivity applies the
        // persisted locale ("ar" on a fresh store) before the first frame.
        //
        // On a truly fresh store AppCompatDelegate applies the locale
        // DURING the first onCreate, which triggers one activity
        // recreation; the pre-recreate composition exists but is not
        // attached to a shown window ("not displayed"). So instead of a
        // plain waitForIdle, wait until a displayed-sized instance of the
        // title exists, then assert on it.
        rule.waitForIdle()
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithText("إعدادات DRS OS")
                .fetchSemanticsNodes()
                .any { it.boundsInRoot.width > 0f && it.boundsInRoot.height > 0f }
        }
        rule.onNodeWithText("إعدادات DRS OS").assertIsDisplayed()

        // Real per-app locale applied by AppCompatDelegate:
        val locale = rule.activity.resources.configuration.locales[0].toLanguageTag()
        assertTrue("expected ar locale, got: $locale", locale.startsWith("ar"))

        // RTL geometry: the title is anchored in the right half.
        assertTrue(
            "expected RTL-anchored title, x=${titleStartX("إعدادات DRS OS")}",
            titleStartX("إعدادات DRS OS") > rootWidth() / 2f
        )
    }

    @Test
    fun pickingEnglish_switchesStringsAndGeometryToLtr() {
        rule.waitForIdle()
        // Wait for a displayed-sized instance of the target row (the
        // fresh-launch recreation dynamics documented in the test above
        // can make the first composition detached and untouchable).
        rule.waitUntil(timeoutMillis = 10_000) {
            rule.onAllNodesWithText("English")
                .fetchSemanticsNodes()
                .any { it.boundsInRoot.width > 0f && it.boundsInRoot.height > 0f }
        }
        // The English row label is the literal "English" in BOTH locales,
        // so this target is locale-stable. The whole row is the click
        // target since the Phase 5 a11y fix (F-1).
        rule.onNodeWithText("English").performClick()
        rule.waitForIdle()

        rule.onNodeWithText("DRS OS settings").assertIsDisplayed()

        val locale = rule.activity.resources.configuration.locales[0].toLanguageTag()
        assertTrue("expected en locale, got: $locale", locale.startsWith("en"))

        // LTR geometry: the title is anchored in the left half.
        val x = titleStartX("DRS OS settings")
        assertTrue(
            "expected LTR-anchored title, x=$x",
            x < rootWidth() / 2f
        )
    }
}
