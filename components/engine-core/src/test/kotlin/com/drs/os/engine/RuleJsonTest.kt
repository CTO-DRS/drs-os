/*
 * SPDX-License-Identifier: Apache-2.0
 */
package com.drs.os.engine

import com.drs.os.foundation.MiniJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RuleJsonTest {

    private val fullRule = """
        {
          "id": "quiet_hours",
          "name": "ساعات الهدوء",
          "enabled": true,
          "trigger": {"kind": "TIME_DAILY", "time": "22:00"},
          "conditions": [
            {"kind": "TIME_WINDOW", "start": "21:00", "end": "23:30"},
            {"kind": "DAY_OF_WEEK", "days": [0, 1, 2, 3, 4]}
          ],
          "action_id": "set_dnd_on",
          "requires_unlock": true,
          "max_runs_per_day": 1,
          "explanation": "تفسير مرئي للمستخدم",
          "user_consented_actions": []
        }
    """.trimIndent()

    @Test
    fun `parses full wire document`() {
        val r = RuleJson.decodeRule(fullRule)
        assertEquals("quiet_hours", r.id)
        assertEquals("ساعات الهدوء", r.name)
        assertEquals(Trigger.TimeDaily("22:00"), r.trigger)
        assertEquals(2, r.conditions.size)
        assertEquals(Condition.TimeWindow("21:00", "23:30"), r.conditions[0])
        assertEquals(Condition.DayOfWeek(setOf(0, 1, 2, 3, 4)), r.conditions[1])
        assertEquals("set_dnd_on", r.actionId)
        assertEquals(1, r.maxRunsPerDay)
    }

    @Test
    fun `defaults applied for omitted optional fields`() {
        val r = RuleJson.decodeRule("""
            {"id":"r","name":"n","enabled":false,
             "trigger":{"kind":"POWER_CONNECTED","state":true},
             "action_id":"set_theme_dark","explanation":"x"}
        """.trimIndent())
        assertEquals(true, r.requiresUnlock)
        assertEquals(10, r.maxRunsPerDay)
        assertEquals(emptyList<String>(), r.userConsentedActions)
        assertEquals(emptyList<Condition>(), r.conditions)
    }

    @Test
    fun `round trip preserves every field`() {
        val original = RuleJson.decodeRule(fullRule)
        val reparsed = RuleJson.decodeRule(RuleJson.encodeRule(original))
        assertEquals(original, reparsed)
    }

    @Test
    fun `all trigger kinds round trip`() {
        for (trigger in listOf(
            Trigger.TimeDaily("07:30"),
            Trigger.BatteryBelow(1),
            Trigger.BatteryBelow(99),
            Trigger.PowerConnected(false))) {
            val r = Rule(
                id = "r", name = "n", enabled = true, trigger = trigger,
                actionId = "set_dnd_on", explanation = "e")
            assertEquals(trigger, RuleJson.decodeRule(RuleJson.encodeRule(r)).trigger)
        }
    }

    @Test
    fun `unknown fields rejected`() {
        val ex = assertThrows(RuleJsonException::class.java) {
            RuleJson.decodeRule("""
                {"id":"r","name":"n","enabled":true,
                 "trigger":{"kind":"TIME_DAILY","time":"22:00","extra":1},
                 "action_id":"set_dnd_on","explanation":"x"}
            """.trimIndent())
        }
        assertEquals("extra", ex.field)
    }

    @Test
    fun `wrong types rejected with field path`() {
        assertThrows(RuleJsonException::class.java) {
            RuleJson.decodeRule("""
                {"id":"r","name":"n","enabled":"yes",
                 "trigger":{"kind":"TIME_DAILY","time":"22:00"},
                 "action_id":"set_dnd_on","explanation":"x"}
            """.trimIndent())
        }
        assertThrows(RuleJsonException::class.java) {
            RuleJson.decodeRule("""
                {"id":"r","name":"n","enabled":true,
                 "trigger":{"kind":"BATTERY_BELOW","threshold":"20"},
                 "action_id":"enable_battery_saver","explanation":"x"}
            """.trimIndent())
        }
        assertThrows(RuleJsonException::class.java) {
            RuleJson.decodeRule("""
                {"id":"r","name":"n","enabled":true,
                 "trigger":{"kind":"NOPE"},
                 "action_id":"set_dnd_on","explanation":"x"}
            """.trimIndent())
        }
    }

    @Test
    fun `duplicate keys rejected by the strict parser`() {
        assertThrows(MiniJson.JsonException::class.java) {
            RuleJson.decodeRule("""
                {"id":"r","id":"r2","name":"n","enabled":true,
                 "trigger":{"kind":"TIME_DAILY","time":"22:00"},
                 "action_id":"set_dnd_on","explanation":"x"}
            """.trimIndent())
        }
    }

    @Test
    fun `context wire round trip`() {
        val ctx = EventContext(801, 4, 15, true, false, 3)
        val parsed = RuleJson.parseContext(RuleJson.writeContext(ctx))
        assertEquals(ctx, parsed)
    }

    @Test
    fun `malformed json surfaces parser error`() {
        assertThrows(MiniJson.JsonException::class.java) {
            RuleJson.decodeRule("{not json")
        }
    }
}
