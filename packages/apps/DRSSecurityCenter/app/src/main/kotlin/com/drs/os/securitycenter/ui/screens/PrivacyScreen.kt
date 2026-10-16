/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Privacy screen: separates (a) what THIS app can actually inspect —
 * computed from the package's own declared permissions (zero) — from
 * (b) device privacy settings, reachable only as shortcuts into the
 * system's own UI. Buttons gate on real resolvability: a screen that
 * does not exist on the device is disabled with an explanation, never
 * a dead button.
 */
package com.drs.os.securitycenter.ui.screens

import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.drs.os.securitycenter.R

private data class Shortcut(val labelRes: Int, val action: String)

private val SHORTCUTS = listOf(
    Shortcut(R.string.privacy_open_system_settings, Settings.ACTION_SETTINGS),
    Shortcut(R.string.privacy_open_security_settings, Settings.ACTION_SECURITY_SETTINGS),
    Shortcut(R.string.privacy_open_privacy_settings, Settings.ACTION_PRIVACY_SETTINGS)
)

@Composable
fun PrivacyScreen() {
    val context = LocalContext.current
    val packageManager = context.packageManager

    // Real data, computed at composition: the permissions this package
    // itself requests. v1 declares none — the UI shows that truth.
    val ownPermissions = remember {
        try {
            packageManager.getPackageInfo(
                context.packageName, PackageManager.GET_PERMISSIONS
            ).requestedPermissions?.toList() ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(stringResource(R.string.privacy_title), style = MaterialTheme.typography.headlineMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.privacy_what_this_app_can_inspect),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = if (ownPermissions.isEmpty()) {
                        stringResource(R.string.privacy_no_permissions_requested)
                    } else {
                        // Defensive: if a future version adds permissions,
                        // they are listed honestly, not hidden.
                        ownPermissions.joinToString("\n")
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    stringResource(R.string.privacy_device_settings),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    stringResource(R.string.privacy_device_settings_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                SHORTCUTS.forEach { shortcut ->
                    val resolvable = remember(shortcut.action) {
                        try {
                            Intent(shortcut.action).resolveActivity(packageManager) != null
                        } catch (e: Exception) {
                            false
                        }
                    }
                    Button(
                        onClick = {
                            try {
                                context.startActivity(Intent(shortcut.action))
                            } catch (e: Exception) {
                                // Surface is gated by resolvability; this is a
                                // last-resort guard, not a silent path.
                            }
                        },
                        enabled = resolvable,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (resolvable) stringResource(shortcut.labelRes)
                            else stringResource(R.string.privacy_open_unavailable)
                        )
                    }
                }
            }
        }
    }
}
