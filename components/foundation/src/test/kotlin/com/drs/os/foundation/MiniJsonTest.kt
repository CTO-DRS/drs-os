/*
 * SPDX-License-Identifier: Apache-2.0
 */
package com.drs.os.foundation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class MiniJsonTest {

    private fun obj(vararg pairs: Pair<String, MiniJson.JsonValue>): MiniJson.JsonValue.JObj =
        MiniJson.JsonValue.JObj(LinkedHashMap(pairs.toMap()))

    private fun str(s: String) = MiniJson.JsonValue.JStr(s)
    private fun num(n: Long) = MiniJson.JsonValue.JNum(n.toString())
    private fun bool(b: Boolean) = MiniJson.JsonValue.JBool(b)
    private fun arr(vararg items: MiniJson.JsonValue) = MiniJson.JsonValue.JArr(items.toList())

    @Test
    fun `parses a typical rule document`() {
        val text = """
            {"id":"quiet_hours","enabled":true,"trigger":{"kind":"TIME_DAILY","time":"22:00"},
             "conditions":[{"kind":"TIME_WINDOW","start":"21:00","end":"23:30"}],
             "action_id":"set_dnd_on","max_runs_per_day":1}
        """.trimIndent()
        val v = MiniJson.parse(text) as MiniJson.JsonValue.JObj
        assertEquals("quiet_hours", (v.entries["id"] as MiniJson.JsonValue.JStr).value)
        assertEquals(true, (v.entries["enabled"] as MiniJson.JsonValue.JBool).value)
        val trigger = v.entries["trigger"] as MiniJson.JsonValue.JObj
        assertEquals("TIME_DAILY", (trigger.entries["kind"] as MiniJson.JsonValue.JStr).value)
        val conds = v.entries["conditions"] as MiniJson.JsonValue.JArr
        assertEquals(1, conds.items.size)
        assertEquals(num(1), v.entries["max_runs_per_day"])
    }

    @Test
    fun `parses and preserves arabic text`() {
        val v = MiniJson.parse("""{"explanation":"يُفعّل وضع عدم الإزعاج تلقائيًا"}""")
            as MiniJson.JsonValue.JObj
        assertEquals(
            "يُفعّل وضع عدم الإزعاج تلقائيًا",
            (v.entries["explanation"] as MiniJson.JsonValue.JStr).value)
    }

    @Test
    fun `handles escapes including surrogate pairs`() {
        val v = MiniJson.parse("""{"a":"line\nbreak","b":"🌍","c":"quote\"inside","d":"\\slash"}""")
            as MiniJson.JsonValue.JObj
        assertEquals("line\nbreak", (v.entries["a"] as MiniJson.JsonValue.JStr).value)
        assertEquals("🌍", (v.entries["b"] as MiniJson.JsonValue.JStr).value)
        assertEquals("quote\"inside", (v.entries["c"] as MiniJson.JsonValue.JStr).value)
        assertEquals("\\slash", (v.entries["d"] as MiniJson.JsonValue.JStr).value)
    }

    @Test
    fun `parses null booleans arrays and nesting`() {
        val v = MiniJson.parse("""{"n":null,"t":true,"f":false,"a":[1,[2,3],{"deep":0}]}""")
            as MiniJson.JsonValue.JObj
        assertEquals(MiniJson.JsonValue.JNull, v.entries["n"])
        assertEquals(bool(true), v.entries["t"])
        val a = v.entries["a"] as MiniJson.JsonValue.JArr
        assertEquals(3, a.items.size)
        val inner = a.items[1] as MiniJson.JsonValue.JArr
        assertEquals(num(3), inner.items[1])
    }

    @Test
    fun `integer numbers expose toLongOrNull`() {
        val v = MiniJson.parse("""{"i":42,"neg":-7,"zero":0,"f":1.5,"e":2e3}""")
            as MiniJson.JsonValue.JObj
        assertEquals(42L, ((v.entries["i"] as MiniJson.JsonValue.JNum)).toLongOrNull())
        assertEquals(-7L, ((v.entries["neg"] as MiniJson.JsonValue.JNum)).toLongOrNull())
        assertEquals(0L, ((v.entries["zero"] as MiniJson.JsonValue.JNum)).toLongOrNull())
        assertNull((v.entries["f"] as MiniJson.JsonValue.JNum).toLongOrNull())
        assertNull((v.entries["e"] as MiniJson.JsonValue.JNum).toLongOrNull())
    }

    @Test
    fun `rejects malformed documents`() {
        val bad = listOf(
            "{\"a\":}",            // missing value
            "{\"a\":1,}",          // trailing comma
            "{a:1}",               // unquoted key
            "{\"a\":1} extra",     // trailing content
            "{\"a\":\"unterminated", // unterminated string
            "{\"a\":1",            // unterminated object
            "[1,2",                // unterminated array
            "{\"a\" 1}",           // missing colon
            "{\"a\":tru}",         // bad literal
            "{\"a\":01}",          // leading zero
            "{\"a\":1.}",          // bad fraction
            "{\"a\":+1}",          // leading plus
            "{\"a\":\"\\x\"}",     // bad escape
            "{\"a\":\"\\uD83C\"}", // unpaired high surrogate
            "{\"a\":\"\u0001\"}"   // raw control character
        )
        for (text in bad) {
            assertThrows(MiniJson.JsonException::class.java) { MiniJson.parse(text) }
        }
    }

    @Test
    fun `rejects duplicate keys`() {
        assertThrows(MiniJson.JsonException::class.java) {
            MiniJson.parse("""{"a":1,"a":2}""")
        }
    }

    @Test
    fun `rejects excessive nesting depth`() {
        val deep = "[".repeat(65) + "]".repeat(65)
        assertThrows(MiniJson.JsonException::class.java) { MiniJson.parse(deep) }
        // One level under the limit parses; the outermost array of a
        // 64-level nesting holds exactly one item (the next array down).
        val ok = "[".repeat(64) + "]".repeat(64)
        MiniJson.parse(ok)
        assertEquals(1, (MiniJson.parse(ok) as MiniJson.JsonValue.JArr).items.size)
    }

    @Test
    fun `write produces canonical minimal output`() {
        val v = obj("b" to bool(true), "a" to str("x\"y"), "n" to MiniJson.JsonValue.JNull)
        assertEquals("""{"b":true,"a":"x\"y","n":null}""", MiniJson.write(v))
    }

    @Test
    fun `write parse round trip preserves values`() {
        val v = obj(
            "ar" to str("الوضع الليلي"),
            "arr" to arr(num(1), num(-2), str("z")),
            "nested" to obj("flag" to bool(false)),
            "nul" to MiniJson.JsonValue.JNull
        )
        assertEquals(v, MiniJson.parse(MiniJson.write(v)))
    }

    @Test
    fun `write escapes control characters`() {
        assertEquals("\"a\\u0001b\"", MiniJson.writeString("a\u0001b"))
    }

    @Test
    fun `vectors file shape parses strictly`() {
        // Guards the differential-test data path with the same parser.
        val text = """[{"case_id":"x","rule":{},"context":{},"expected":{}}]"""
        val v = MiniJson.parse(text) as MiniJson.JsonValue.JArr
        assertEquals(1, v.items.size)
        assertTrue(v.items[0] is MiniJson.JsonValue.JObj)
    }
}
