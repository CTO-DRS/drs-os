/*
 * SPDX-License-Identifier: Apache-2.0
 */
package com.drs.os.foundation

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class KvStoreTest {

    @Test
    fun `round trips plain ascii entries`() {
        val src = mapOf("alpha" to "1", "beta" to "two words")
        assertEquals(src, KvStore.decode(KvStore.encode(src)))
    }

    @Test
    fun `round trips arabic text`() {
        val src = mapOf("رسالة" to "مرحبا بالعالم", "lang" to "العربية")
        assertEquals(src, KvStore.decode(KvStore.encode(src)))
    }

    @Test
    fun `round trips dangerous characters inside values`() {
        val src = mapOf(
            "eq" to "key=value",
            "pct" to "100%25",
            "nl" to "line1\nline2",
            "cr" to "a\rb",
            "emoji" to "🌍 mixed العربية",
            "tilde~dash-under.dot" to "-._~"
        )
        assertEquals(src, KvStore.decode(KvStore.encode(src)))
    }

    @Test
    fun `encode output is deterministic and sorted`() {
        val a = KvStore.encode(linkedMapOf("z" to "1", "a" to "2", "m" to "3"))
        val b = KvStore.encode(linkedMapOf("a" to "2", "m" to "3", "z" to "1"))
        assertEquals(a, b)
        assertTrue(a.startsWith("a=2\n"))
    }

    @Test
    fun `encoded content is pure printable ascii`() {
        val encoded = KvStore.encode(mapOf("مفتاح" to "قيمة"))
        assertTrue(encoded.all { it.code in 0x21..0x7E || it == '\n' })
    }

    @Test
    fun `empty map round trip`() {
        assertEquals("", KvStore.encode(emptyMap()))
        assertEquals(emptyMap<String, String>(), KvStore.decode(""))
        assertEquals(emptyMap<String, String>(), KvStore.decode("\n"))
    }

    @Test
    fun `rejects missing separator`() {
        assertThrows(IllegalArgumentException::class.java) {
            KvStore.decode("this-line-has-no-separator\n")
        }
    }

    @Test
    fun `rejects blank inner lines`() {
        assertThrows(IllegalArgumentException::class.java) {
            KvStore.decode("a=1\n\nb=2\n")
        }
    }

    @Test
    fun `rejects duplicate keys`() {
        assertThrows(IllegalArgumentException::class.java) {
            KvStore.decode("a=1\na=2\n")
        }
    }

    @Test
    fun `rejects truncated percent escape`() {
        assertThrows(IllegalArgumentException::class.java) {
            KvStore.decode("key=abc%2")
        }
    }

    @Test
    fun `rejects invalid percent hex`() {
        assertThrows(IllegalArgumentException::class.java) {
            KvStore.decode("key=abc%ZZ")
        }
    }

    @Test
    fun `rejects raw non-encoded characters`() {
        // The encoder never emits raw non-ASCII or raw spaces.
        assertThrows(IllegalArgumentException::class.java) {
            KvStore.decode("key=مرحبا")
        }
        assertThrows(IllegalArgumentException::class.java) {
            KvStore.decode("key with space=1")
        }
    }

    @Test
    fun `large realistic settings payload round trips`() {
        val src = linkedMapOf(
            "schema_version" to "1",
            "language" to "ar",
            "theme" to "SYSTEM",
            "automation_master_enabled" to "false",
            "note" to "الوضع الافتراضي: تشغيل جاف التجريبي قبل التنفيذ الفعلي"
        )
        assertEquals(src, KvStore.decode(KvStore.encode(src)))
    }
}
