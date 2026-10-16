/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS OS settings — core domain model and persistence (Phase 3,
 * Workstream D core). Locale-free and Android-free so it is unit-test
 * able on any JVM; the app binds values to UI and resources.
 *
 * Persistence policy (security-relevant):
 *  * stored via a byte-store abstraction (app supplies SharedPreferences);
 *  * the wire format is the foundation KvStore line format (deterministic);
 *  * STRUCTURAL corruption (any malformed line, duplicate key, bad
 *    escape) → the WHOLE document is discarded and defaults are used,
 *    flagged as corrupted — partial salvage could resurrect tampered
 *    values, so it is deliberately not attempted;
 *  * unknown enum values or out-of-range fields fall back per-field
 *    with a recorded recovery note;
 *  * unknown keys are ignored (forward-compatible additions by older/
 *    newer readers do not break startup);
 *  * a schema version other than the current one → defaults + note
 *    (no silent "migrations" without an ADR).
 */
package com.drs.os.settings

import com.drs.os.foundation.KvStore

enum class AppLanguage(val code: String) {
    ARABIC("ar"),
    ENGLISH("en");

    companion object {
        fun fromCode(code: String): AppLanguage? = entries.firstOrNull { it.code == code }
    }
}

enum class ThemeMode { SYSTEM, LIGHT, DARK }

const val SETTINGS_SCHEMA_VERSION = 1

data class DrsSettings(
    val language: AppLanguage = AppLanguage.ARABIC,
    val theme: ThemeMode = ThemeMode.SYSTEM,
    /** Master switch for DRS Automation rules. */
    val automationMasterEnabled: Boolean = false,
    /** Dry-run first: default execution mode for new automation runs. */
    val automationDryRunDefault: Boolean = true,
    /** Shortcuts from DRS Settings to DRS Security Center. */
    val securityShortcutsEnabled: Boolean = true
)

object SettingsCodec {

    const val KEY_SCHEMA_VERSION = "schema_version"
    const val KEY_LANGUAGE = "language"
    const val KEY_THEME = "theme"
    const val KEY_AUTOMATION_MASTER = "automation_master_enabled"
    const val KEY_AUTOMATION_DRY_RUN = "automation_dry_run_default"
    const val KEY_SECURITY_SHORTCUTS = "security_shortcuts_enabled"

    private val KNOWN_KEYS = setOf(
        KEY_SCHEMA_VERSION, KEY_LANGUAGE, KEY_THEME, KEY_AUTOMATION_MASTER,
        KEY_AUTOMATION_DRY_RUN, KEY_SECURITY_SHORTCUTS)

    data class Parsed(
        val settings: DrsSettings,
        /** Field-level recoveries ("" structural flags are separate). */
        val recoveries: List<String>,
        /** True when the document was structurally corrupted. */
        val structuralCorruption: Boolean
    )

    fun encode(settings: DrsSettings): String = KvStore.encode(
        linkedMapOf(
            KEY_SCHEMA_VERSION to SETTINGS_SCHEMA_VERSION.toString(),
            KEY_LANGUAGE to settings.language.code,
            KEY_THEME to settings.theme.name,
            KEY_AUTOMATION_MASTER to settings.automationMasterEnabled.toString(),
            KEY_AUTOMATION_DRY_RUN to settings.automationDryRunDefault.toString(),
            KEY_SECURITY_SHORTCUTS to settings.securityShortcutsEnabled.toString()
        ))

    fun decode(raw: String): Parsed {
        val entries = try {
            KvStore.decode(raw)
        } catch (e: IllegalArgumentException) {
            return Parsed(DrsSettings(), listOf("corrupted-document"), structuralCorruption = true)
        }

        val recoveries = ArrayList<String>()
        for (key in entries.keys) {
            if (key !in KNOWN_KEYS) recoveries.add("ignored-unknown-key:$key")
        }

        val versionRaw = entries[KEY_SCHEMA_VERSION]
        val version = versionRaw?.toIntOrNull()
        if (versionRaw != null && version != SETTINGS_SCHEMA_VERSION) {
            recoveries.add("unknown-schema-version:$versionRaw")
            return Parsed(DrsSettings(), recoveries, structuralCorruption = false)
        }

        val language = entries[KEY_LANGUAGE]?.let {
            AppLanguage.fromCode(it) ?: run {
                recoveries.add("invalid-language:$it"); null
            }
        } ?: AppLanguage.ARABIC

        val theme = entries[KEY_THEME]?.let {
            try {
                ThemeMode.valueOf(it)
            } catch (e: IllegalArgumentException) {
                recoveries.add("invalid-theme:$it"); null
            }
        } ?: ThemeMode.SYSTEM

        fun booleanOr(key: String, default: Boolean): Boolean =
            entries[key]?.let { v ->
                when (v) {
                    "true" -> true
                    "false" -> false
                    else -> {
                        recoveries.add("invalid-$key:$v"); default
                    }
                }
            } ?: default

        return Parsed(
            settings = DrsSettings(
                language = language,
                theme = theme,
                automationMasterEnabled = booleanOr(KEY_AUTOMATION_MASTER, false),
                automationDryRunDefault = booleanOr(KEY_AUTOMATION_DRY_RUN, true),
                securityShortcutsEnabled = booleanOr(KEY_SECURITY_SHORTCUTS, true)
            ),
            recoveries = recoveries,
            structuralCorruption = false
        )
    }
}

/** Raw byte-store the host supplies (e.g. SharedPreferences wrapper). */
interface SettingsByteStore {
    /** Null = nothing persisted yet. */
    fun read(): String?

    /** False = write failed (caller must surface it, not hide it). */
    fun write(content: String): Boolean
}

enum class SettingsSource { FRESH, RESTORED_WITH_RECOVERIES, RESTORED_FROM_CORRUPTED }

data class LoadedSettings(
    val settings: DrsSettings,
    val source: SettingsSource,
    val recoveries: List<String>
)

sealed interface SaveResult {
    object Saved : SaveResult
    data class Failed(val reason: String) : SaveResult
}

class SettingsRepository(private val store: SettingsByteStore) {

    fun load(): LoadedSettings {
        val raw = store.read() ?: return LoadedSettings(
            DrsSettings(), SettingsSource.FRESH, emptyList())
        if (raw.isBlank()) return LoadedSettings(
            DrsSettings(), SettingsSource.FRESH, emptyList())
        val parsed = SettingsCodec.decode(raw)
        return when {
            parsed.structuralCorruption -> LoadedSettings(
                parsed.settings, SettingsSource.RESTORED_FROM_CORRUPTED, parsed.recoveries)
            parsed.recoveries.isNotEmpty() -> LoadedSettings(
                parsed.settings, SettingsSource.RESTORED_WITH_RECOVERIES, parsed.recoveries)
            else -> LoadedSettings(parsed.settings, SettingsSource.FRESH, emptyList())
        }
    }

    fun save(settings: DrsSettings): SaveResult =
        if (store.write(SettingsCodec.encode(settings))) {
            SaveResult.Saved
        } else {
            SaveResult.Failed("store-write-failed")
        }
}
