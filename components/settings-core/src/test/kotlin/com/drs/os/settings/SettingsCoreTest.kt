/*
 * SPDX-License-Identifier: Apache-2.0
 */
package com.drs.os.settings

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private class MemStore(initial: String? = null) : SettingsByteStore {
    var data: String? = initial
    var failWrites: Boolean = false
    var writes: Int = 0
    override fun read(): String? = data
    override fun write(content: String): Boolean {
        writes++
        if (failWrites) return false
        data = content
        return true
    }
}

class SettingsCoreTest {

    // ------------------------------------------------------------ codec

    @Test
    fun `encode decode round trip preserves values`() {
        val s = DrsSettings(
            language = AppLanguage.ENGLISH, theme = ThemeMode.DARK,
            automationMasterEnabled = true, automationDryRunDefault = false,
            securityShortcutsEnabled = false)
        assertEquals(s, SettingsCodec.decode(SettingsCodec.encode(s)).settings)
    }

    @Test
    fun `defaults are arabic system secure-by-default`() {
        val s = DrsSettings()
        assertEquals(AppLanguage.ARABIC, s.language)
        assertEquals(ThemeMode.SYSTEM, s.theme)
        assertEquals(false, s.automationMasterEnabled)
        assertEquals(true, s.automationDryRunDefault)
    }

    @Test
    fun `stored document is kv line format with known keys`() {
        val raw = SettingsCodec.encode(DrsSettings())
        val lines = raw.trimEnd('\n').split('\n').toSet()
        assertEquals(
            setOf("schema_version=1", "language=ar", "theme=SYSTEM",
                "automation_master_enabled=false", "automation_dry_run_default=true",
                "security_shortcuts_enabled=true"),
            lines)
    }

    @Test
    fun `empty store decodes to defaults`() {
        val p = SettingsCodec.decode("")
        assertEquals(DrsSettings(), p.settings)
        assertTrue(p.recoveries.isEmpty())
    }

    @Test
    fun `structural corruption discards the whole document`() {
        for (corrupt in listOf(
            "no-separator-line\n",
            "language=ar\n\ngarbage\n",
            "language=ar\nlanguage=en\n",          // duplicate key
            "language=مرحبا\n",                     // raw non-encoded char
            "theme=DARK%\n"                         // truncated escape
        )) {
            val p = SettingsCodec.decode(corrupt)
            assertEquals(DrsSettings(), p.settings, "corrupt input: $corrupt")
            assertTrue(p.structuralCorruption, "corrupt input: $corrupt")
            assertTrue(p.recoveries.contains("corrupted-document"))
        }
    }

    @Test
    fun `invalid enum values fall back per field with recovery note`() {
        val p = SettingsCodec.decode(
            "schema_version=1\nlanguage=fr\ntheme=NEON\n" +
                "automation_master_enabled=yes\nautomation_dry_run_default=true\n" +
                "security_shortcuts_enabled=true\n")
        assertEquals(DrsSettings(), p.settings)   // all defaults
        assertTrue(p.recoveries.contains("invalid-language:fr"))
        assertTrue(p.recoveries.contains("invalid-theme:NEON"))
        assertTrue(p.recoveries.contains("invalid-automation_master_enabled:yes"))
        assertTrue(!p.structuralCorruption)
    }

    @Test
    fun `unknown keys are ignored and recorded`() {
        val p = SettingsCodec.decode(
            "schema_version=1\nlanguage=ar\ntelemetry_opt_in=true\n")
        assertEquals(AppLanguage.ARABIC, p.settings.language)
        assertTrue(p.recoveries.contains("ignored-unknown-key:telemetry_opt_in"))
    }

    @Test
    fun `unknown schema version uses defaults and flags`() {
        val p = SettingsCodec.decode("schema_version=2\nlanguage=en\ntheme=DARK\n")
        assertEquals(DrsSettings(), p.settings)
        assertTrue(p.recoveries.contains("unknown-schema-version:2"))
    }

    @Test
    fun `non-integer schema version is treated as unknown`() {
        val p = SettingsCodec.decode("schema_version=abc\n")
        assertEquals(DrsSettings(), p.settings)
        assertTrue(p.recoveries.contains("unknown-schema-version:abc"))
    }

    @Test
    fun `arabic content in stored settings round trips`() {
        // Language codes are stable, but the codec must remain byte-safe
        // for any future free-text field (percent-encoded kv).
        val raw = "note=" + com.drs.os.foundation.KvStore.percentEncode("الوضع الليلي")
        val p = SettingsCodec.decode("schema_version=1\n$raw\n")
        assertTrue(p.recoveries.contains("ignored-unknown-key:note"))
        assertEquals(DrsSettings(), p.settings)
    }

    // ------------------------------------------------------- repository

    @Test
    fun `repository load on empty store yields fresh defaults`() {
        val loaded = SettingsRepository(MemStore()).load()
        assertEquals(SettingsSource.FRESH, loaded.source)
        assertEquals(DrsSettings(), loaded.settings)
    }

    @Test
    fun `repository round trip through the byte store`() {
        val store = MemStore()
        val repo = SettingsRepository(store)
        repo.save(DrsSettings(theme = ThemeMode.LIGHT, language = AppLanguage.ENGLISH))
        val loaded = repo.load()
        assertEquals(SettingsSource.FRESH, loaded.source)
        assertEquals(ThemeMode.LIGHT, loaded.settings.theme)
        assertEquals(AppLanguage.ENGLISH, loaded.settings.language)
    }

    @Test
    fun `repository surfaces corruption honestly`() {
        val repo = SettingsRepository(MemStore("\u0000\u0001\u0002not-a-document"))
        val loaded = repo.load()
        assertEquals(SettingsSource.RESTORED_FROM_CORRUPTED, loaded.source)
        assertEquals(DrsSettings(), loaded.settings)
    }

    @Test
    fun `repository surfaces per-field recovery`() {
        val repo = SettingsRepository(MemStore("schema_version=1\nlanguage=xx\n"))
        val loaded = repo.load()
        assertEquals(SettingsSource.RESTORED_WITH_RECOVERIES, loaded.source)
        assertTrue(loaded.recoveries.contains("invalid-language:xx"))
    }

    @Test
    fun `failed write is reported not hidden`() {
        val store = MemStore().apply { failWrites = true }
        val result = SettingsRepository(store).save(DrsSettings())
        assertEquals(SaveResult.Failed("store-write-failed"), result)
        assertEquals(0, store.writes - 1 + 0) // write attempted exactly once
        assertTrue(store.data == null)
    }

    @Test
    fun `whitespace-only store behaves like empty`() {
        val loaded = SettingsRepository(MemStore("\n  \n")).load()
        assertEquals(SettingsSource.FRESH, loaded.source)
        assertEquals(DrsSettings(), loaded.settings)
    }
}
