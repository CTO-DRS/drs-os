/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * System status screen: every displayed value comes from a real API
 * through AndroidCheckProvider, with its source in the row label.
 * Unreadable capabilities are labeled Unavailable with the platform
 * reason — no invented values anywhere.
 */
package com.drs.os.securitycenter.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.drs.os.security.CheckInput
import com.drs.os.security.DrsChecklistV1
import com.drs.os.securitycenter.R
import com.drs.os.securitycenter.platform.AndroidCheckProvider

@Composable
fun StatusScreen() {
    val appContext = LocalContext.current.applicationContext
    val provider = remember { AndroidCheckProvider(appContext) }

    val rows = remember {
        listOf(
            R.string.status_row_device_secure to DrsChecklistV1.INPUT_DEVICE_SECURE,
            R.string.status_row_encryption to DrsChecklistV1.INPUT_ENCRYPTION_ACTIVE,
            R.string.status_row_patch to DrsChecklistV1.INPUT_SECURITY_PATCH,
            R.string.status_row_verified_boot to DrsChecklistV1.INPUT_VERIFIED_BOOT,
            R.string.status_row_permissions to DrsChecklistV1.INPUT_PERMISSION_USAGE,
            R.string.status_row_automation to DrsChecklistV1.INPUT_AUTOMATION_DIGEST
        )
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(stringResource(R.string.status_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.status_source_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        rows.forEach { (labelRes, inputId) ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(labelRes), style = MaterialTheme.typography.titleLarge)
                    when (val input = provider.provide(inputId)) {
                        is CheckInput.Available -> Text(
                            text = when (input.value) {
                                "true" -> stringResource(R.string.status_value_true)
                                "false" -> stringResource(R.string.status_value_false)
                                "" -> stringResource(R.string.status_unavailable)
                                else -> stringResource(R.string.status_patch_prefix) + " " + input.value
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                        is CheckInput.Unavailable -> Text(
                            text = stringResource(R.string.status_unavailable) + " — " + input.reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        is CheckInput.NotChecked -> Text(
                            text = stringResource(R.string.state_notchecked),
                            style = MaterialTheme.typography.bodyMedium
                        )
                        null -> Text(
                            text = stringResource(R.string.status_unavailable),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
