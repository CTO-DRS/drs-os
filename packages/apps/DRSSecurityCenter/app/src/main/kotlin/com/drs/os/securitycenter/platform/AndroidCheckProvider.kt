/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The ONLY source of check inputs in the standalone app. Reads strictly
 * permissionless platform APIs; everything else is honestly
 * Unavailable. When DRS OS becomes a platform privileged app, the
 * privileged inputs are added HERE with a permission justification —
 * not by silently broadening the surface.
 */
package com.drs.os.securitycenter.platform

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.os.Build
import com.drs.os.security.CheckInput
import com.drs.os.security.CheckDataProvider
import com.drs.os.security.DrsChecklistV1

class AndroidCheckProvider(private val context: Context) : CheckDataProvider {

    override fun provide(inputId: String): CheckInput? = when (inputId) {
        DrsChecklistV1.INPUT_DEVICE_SECURE -> {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            CheckInput.Available(if (km.isDeviceSecure) "true" else "false")
        }
        DrsChecklistV1.INPUT_ENCRYPTION_ACTIVE -> {
            try {
                val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE)
                    as DevicePolicyManager
                val active = dpm.storageEncryptionStatus ==
                    DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE ||
                    dpm.storageEncryptionStatus ==
                    DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER
                CheckInput.Available(if (active) "true" else "false")
            } catch (e: Exception) {
                CheckInput.Unavailable("encryption status unreadable: platform error")
            }
        }
        DrsChecklistV1.INPUT_SECURITY_PATCH ->
            CheckInput.Available(Build.VERSION.SECURITY_PATCH ?: "")
        DrsChecklistV1.INPUT_VERIFIED_BOOT -> CheckInput.Unavailable(
            "verified-boot state requires platform integration (system-level " +
                "property access); unavailable to a standalone app")
        DrsChecklistV1.INPUT_PERMISSION_USAGE -> CheckInput.Unavailable(
            "permission-usage APIs are privileged; unavailable to a standalone app")
        DrsChecklistV1.INPUT_AUTOMATION_DIGEST -> CheckInput.Unavailable(
            "DRS Automation rule state is cross-app; read path pending integration")
        else -> null
    }
}
