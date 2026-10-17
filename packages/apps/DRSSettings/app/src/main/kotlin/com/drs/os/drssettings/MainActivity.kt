/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS Settings — MainActivity and screens. Language selection uses the
 * platform per-app locales mechanism (AppCompatDelegate); theme uses
 * UiModeManager-compatible AppCompat night mode; every persisted value
 * goes through the tested settings-core repository.
 */
package com.drs.os.drssettings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.core.os.LocaleListCompat
import com.drs.os.drssettings.ui.theme.DrsTheme
import com.drs.os.settings.AppLanguage
import com.drs.os.settings.DrsSettings
import com.drs.os.settings.LoadedSettings
import com.drs.os.settings.SaveResult
import com.drs.os.settings.SettingsByteStore
import com.drs.os.settings.SettingsRepository
import com.drs.os.settings.ThemeMode

/** SharedPreferences-backed byte store (settings-core contract). */
class AndroidSettingsByteStore(context: Context) : SettingsByteStore {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("drs_settings", Context.MODE_PRIVATE)

    override fun read(): String? = prefs.getString("settings_v1", null)

    override fun write(content: String): Boolean =
        prefs.edit().putString("settings_v1", content).commit()
}

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val repository = SettingsRepository(AndroidSettingsByteStore(applicationContext))
        val loaded = repository.load()
        // Apply persisted language/theme BEFORE first frame.
        applyLanguage(loaded.settings.language)
        applyTheme(loaded.settings.theme)
        setContent {
            DrsTheme {
                SettingsApp(repository = repository, initial = loaded)
            }
        }
    }
}

private fun applyLanguage(language: AppLanguage) {
    AppCompatDelegate.setApplicationLocales(
        LocaleListCompat.forLanguageTags(language.code))
}

private fun applyTheme(theme: ThemeMode) {
    AppCompatDelegate.setDefaultNightMode(
        when (theme) {
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        })
}

@Composable
fun SettingsApp(repository: SettingsRepository, initial: LoadedSettings) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(initial.settings) }
    var banner by remember { mutableStateOf<String?>(null) }

    fun update(transform: (DrsSettings) -> DrsSettings) {
        val next = transform(settings)
        when (val result = repository.save(next)) {
            SaveResult.Saved -> {
                settings = next
                banner = null
            }
            is SaveResult.Failed -> banner = context.getString(R.string.error_store_write_failed)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)

        if (initial.source == com.drs.os.settings.SettingsSource.RESTORED_FROM_CORRUPTED) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(
                    stringResource(R.string.recovered_from_corrupted),
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        banner?.let {
            Card(modifier = Modifier.fillMaxWidth()) {
                Text(it, modifier = Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
            }
        }

        SectionCard(title = stringResource(R.string.entry_language)) {
            LanguageSection(settings, onPick = { language ->
                update { it.copy(language = language) }
                applyLanguage(language)
            })
        }

        SectionCard(title = stringResource(R.string.entry_theme)) {
            ThemeSection(settings, onPick = { theme ->
                update { it.copy(theme = theme) }
                applyTheme(theme)
            })
        }

        SectionCard(title = stringResource(R.string.entry_privacy_security)) {
            PrivacySecuritySection(context)
        }

        SectionCard(title = stringResource(R.string.entry_automation)) {
            AutomationSection(settings,
                onMaster = { v -> update { it.copy(automationMasterEnabled = v) } },
                onDryRun = { v -> update { it.copy(automationDryRunDefault = v) } })
        }

        SectionCard(title = stringResource(R.string.entry_about)) {
            AboutSection()
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun LanguageSection(settings: DrsSettings, onPick: (AppLanguage) -> Unit) {
    Text(stringResource(R.string.language_hint), style = MaterialTheme.typography.bodyMedium)
    RadioRow(
        label = stringResource(R.string.language_arabic),
        selected = settings.language == AppLanguage.ARABIC,
        onClick = { onPick(AppLanguage.ARABIC) })
    RadioRow(
        label = stringResource(R.string.language_english),
        selected = settings.language == AppLanguage.ENGLISH,
        onClick = { onPick(AppLanguage.ENGLISH) })
}

@Composable
private fun ThemeSection(settings: DrsSettings, onPick: (ThemeMode) -> Unit) {
    Text(stringResource(R.string.theme_hint), style = MaterialTheme.typography.bodyMedium)
    RadioRow(stringResource(R.string.theme_system), settings.theme == ThemeMode.SYSTEM) {
        onPick(ThemeMode.SYSTEM)
    }
    RadioRow(stringResource(R.string.theme_light), settings.theme == ThemeMode.LIGHT) {
        onPick(ThemeMode.LIGHT)
    }
    RadioRow(stringResource(R.string.theme_dark), settings.theme == ThemeMode.DARK) {
        onPick(ThemeMode.DARK)
    }
}

@Composable
private fun PrivacySecuritySection(context: Context) {
    Text(stringResource(R.string.privacy_shortcuts_note), style = MaterialTheme.typography.bodyMedium)
    val securityCenterInstalled = remember {
        try {
            context.packageManager.getPackageInfo("com.drs.os.securitycenter", 0)
            true
        } catch (e: Exception) {
            false
        }
    }
    if (securityCenterInstalled) {
        Button(onClick = {
            try {
                context.startActivity(
                    context.packageManager.getLaunchIntentForPackage("com.drs.os.securitycenter"))
            } catch (e: ActivityNotFoundException) {
                // gated by the installed check; defensive only
            }
        }) { Text(stringResource(R.string.privacy_security_open_center)) }
    } else {
        Text(
            stringResource(R.string.privacy_security_center_missing),
            style = MaterialTheme.typography.bodyMedium
        )
    }
    OutlinedButton(onClick = {
        try {
            context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS))
        } catch (e: ActivityNotFoundException) {
            // system settings always exist; defensive only
        }
    }) { Text(stringResource(R.string.privacy_security_open_system)) }
}

@Composable
private fun AutomationSection(
    settings: DrsSettings,
    onMaster: (Boolean) -> Unit,
    onDryRun: (Boolean) -> Unit
) {
    Text(stringResource(R.string.automation_scope_note), style = MaterialTheme.typography.bodyMedium)
    SwitchRow(
        label = stringResource(R.string.automation_master) + " — " +
            stringResource(if (settings.automationMasterEnabled) R.string.automation_state_on
            else R.string.automation_state_off),
        checked = settings.automationMasterEnabled,
        onChange = onMaster)
    SwitchRow(
        label = stringResource(R.string.automation_dry_run_default),
        checked = settings.automationDryRunDefault,
        onChange = onDryRun)
}

@Composable
private fun AboutSection() {
    Text(
        stringResource(R.string.about_version) + " " + BuildConfig.VERSION_NAME,
        style = MaterialTheme.typography.bodyLarge
    )
    Text(
        stringResource(R.string.about_version_code) + " " + BuildConfig.VERSION_CODE.toString(),
        style = MaterialTheme.typography.bodyLarge
    )
    Text(stringResource(R.string.about_policy_ai), style = MaterialTheme.typography.bodyMedium)
    Text(stringResource(R.string.about_provenance_note), style = MaterialTheme.typography.bodyMedium)
    Text(stringResource(R.string.about_notice), style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun RadioRow(label: String, selected: Boolean, onClick: () -> Unit) {
    // Phase 5 a11y fix (F-1): the WHOLE row is now the click target via
    // row-level selectable semantics (role = RadioButton). Previously only
    // the radio button itself responded to touches; the label was a dead
    // zone — a real touch-target/accessibility defect. RadioButton's own
    // onClick is nulled to avoid duplicate activation from one tap.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            )
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    // Phase 5 a11y fix (F-1): row-level toggleable semantics (role =
    // Switch); the label toggles the switch. Switch's own
    // onCheckedChange is nulled to avoid double-handling one tap.
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = onChange
            )
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = null)
    }
}
