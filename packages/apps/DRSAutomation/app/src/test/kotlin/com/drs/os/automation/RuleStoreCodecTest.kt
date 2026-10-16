/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Rule persistence codec tests (pure JVM — no Android APIs).
 */
package com.drs.os.automation.host

import com.drs.os.engine.Condition
import com.drs.os.engine.Rule
import com.drs.os.engine.Trigger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RuleStoreCodecTest {

    private fun sample(id: String = "quiet_hours") = Rule(
        id = id, name = "ساعات الهدوء", enabled = true,
        trigger = Trigger.TimeDaily("22:00"),
        conditions = listOf(Condition.TimeWindow("21:00", "23:30")),
        actionId = "set_dnd_on", maxRunsPerDay = 1,
        explanation = "يُفعّل عدم الإزعاج")

    @Test
    fun `rules round trip through the kv wire format`() {
        val rules = listOf(sample(), sample("battery"), sample("aaa"))
        val raw = RuleStoreCodec.encodeRules(rules)
        val decoded = RuleStoreCodec.decodeRules(raw)
        assertFalse(decoded.corrupted)
        // sorted by id on both sides — deterministic
        assertEquals(listOf("aaa", "battery", "quiet_hours"), decoded.rules.map { it.id })
        assertEquals(sample("aaa"), decoded.rules.first())
        assertEquals(sample(), decoded.rules.last())
    }

    @Test
    fun `empty and null stores decode to empty uncorrupted`() {
        assertFalse(RuleStoreCodec.decodeRules(null).corrupted)
        assertTrue(RuleStoreCodec.decodeRules(null).rules.isEmpty())
        assertFalse(RuleStoreCodec.decodeRules("").corrupted)
        assertTrue(RuleStoreCodec.decodeRules("  \n").rules.isEmpty())
    }

    @Test
    fun `corrupted store yields empty rules with corruption flag`() {
        for (bad in listOf(
            "no-separator\n",
            "quiet_hours={\"id\":\"quiet_hours\"\n",          // truncated json
            "quiet_hours=not-json\n",
            "wrongid={\"id\":\"other\",\"name\":\"n\",\"enabled\":true," +
                "\"trigger\":{\"kind\":\"POWER_CONNECTED\",\"state\":true}," +
                "\"action_id\":\"set_dnd_on\",\"explanation\":\"e\"}\n"
        )) {
            val decoded = RuleStoreCodec.decodeRules(bad)
            assertTrue(decoded.rules.isEmpty(), "input: $bad")
            assertTrue(decoded.corrupted, "input: $bad")
        }
    }

    @Test
    fun `id mismatch between key and payload is corruption not salvage`() {
        val raw = "other={\"id\":\"quiet_hours\",\"name\":\"n\",\"enabled\":true," +
            "\"trigger\":{\"kind\":\"POWER_CONNECTED\",\"state\":true}," +
            "\"action_id\":\"set_dnd_on\",\"explanation\":\"e\"}\n"
        val decoded = RuleStoreCodec.decodeRules(raw)
        assertTrue(decoded.corrupted)
        assertTrue(decoded.rules.isEmpty())
    }
}
