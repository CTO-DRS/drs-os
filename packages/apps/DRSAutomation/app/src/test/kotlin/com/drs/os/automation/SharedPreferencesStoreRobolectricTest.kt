/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS Automation — Robolectric no-device CI layer (Phase 5).
 *
 * Exercises the Android host store (SharedPreferencesStore) against
 * REAL SharedPreferences, and the rule persistence codec end-to-end:
 * commit() durability, encode/decode round-trip through the real store,
 * and the corrupted-document discard policy (empty set + flag, never a
 * partially-salvaged rule set).
 *
 * Honest scope note: JVM-simulated Android (Robolectric), not a device.
 */
package com.drs.os.automation

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.drs.os.automation.host.RuleStoreCodec
import com.drs.os.automation.host.RuleStoreCodec.RULES_KEY
import com.drs.os.automation.host.SharedPreferencesStore
import com.drs.os.engine.Rule
import com.drs.os.engine.Trigger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [35])
class SharedPreferencesStoreRobolectricTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun newStore(): SharedPreferencesStore =
        SharedPreferencesStore(context, "drs_automation_robolectric_test")

    private fun sampleRule(id: String): Rule = Rule(
        id = id,
        name = "test-rule-$id",
        enabled = true,
        trigger = Trigger.TimeDaily("07:30"),
        actionId = "set_dnd_on",
        explanation = "Robolectric round-trip fixture"
    )

    @Test
    fun write_reportsDurability_andSurvivesAcrossStoreInstances() {
        val first = newStore()
        // commit() semantics: the boolean honestly reports durability.
        assertTrue(first.write("probe_key", "probe-value"))
        // A fresh instance must observe the same durable value.
        assertEquals("probe-value", newStore().read("probe_key"))
    }

    @Test
    fun rules_roundTripThroughRealSharedPreferences_sortedAndUncorrupted() {
        val store = newStore()
        // Deliberately unsorted ids; the codec is required to sort by id.
        val rules = listOf(sampleRule("r2"), sampleRule("r1"))

        val encoded = RuleStoreCodec.encodeRules(rules)
        assertTrue(store.write(RULES_KEY, encoded))

        val decoded = RuleStoreCodec.decodeRules(store.read(RULES_KEY))
        assertFalse(decoded.corrupted)
        assertEquals(listOf("r1", "r2"), decoded.rules.map { it.id })
        assertEquals(rules.sortedBy { it.id }, decoded.rules)
    }

    @Test
    fun corruptedRulesDocument_yieldsEmptyRuleSet_andCorruptionFlag() {
        val store = newStore()
        // Seed REAL storage with a structurally invalid document (no '='
        // separator — the foundation KvStore codec rejects it).
        assertTrue(store.write(RULES_KEY, "garbage-without-separator"))

        val decoded = RuleStoreCodec.decodeRules(store.read(RULES_KEY))
        assertTrue(decoded.rules.isEmpty())
        assertTrue(decoded.corrupted)
    }
}
