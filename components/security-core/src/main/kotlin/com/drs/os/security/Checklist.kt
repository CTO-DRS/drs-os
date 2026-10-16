/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS OS Security Center — v1 deterministic check definitions.
 *
 * Every check consumes only inputs its provider actually supplied.
 * Input contracts (values are strings, semantics pinned here):
 *
 *   device_secure          "true" | "false"      (e.g. KeyguardManager.isDeviceSecure — no permission)
 *   encryption_active      "true" | "false"      (e.g. DevicePolicyManager.storageEncryptionStatus)
 *   security_patch         patch string or ""    (e.g. Build.VERSION.SECURITY_PATCH)
 *   verified_boot_state    "green"|"yellow"|"orange"|"red"|""   (platform-supplied; often unavailable outside system builds)
 *   permission_usage_data  any non-empty digest  (privileged; NOT readable by a normal app)
 *   automation_rules_digest any non-empty digest (cross-app; pending integration)
 *
 * The default provider in the standalone app will honestly return
 * Unavailable for the inputs it cannot read — that produces UNAVAILABLE
 * states with explanations, never simulated success.
 */
package com.drs.os.security

object DrsChecklistV1 {

    const val INPUT_DEVICE_SECURE = "device_secure"
    const val INPUT_ENCRYPTION_ACTIVE = "encryption_active"
    const val INPUT_SECURITY_PATCH = "security_patch"
    const val INPUT_VERIFIED_BOOT = "verified_boot_state"
    const val INPUT_PERMISSION_USAGE = "permission_usage_data"
    const val INPUT_AUTOMATION_DIGEST = "automation_rules_digest"

    private fun ev(id: String, detail: String) = CheckEvidence(id, detail)

    val definitions: List<CheckDefinition> = listOf(
        CheckDefinition(
            id = "screen_lock",
            requiredInputs = listOf(INPUT_DEVICE_SECURE)
        ) { inputs ->
            when (inputs.getValue(INPUT_DEVICE_SECURE).value) {
                "true" -> CheckOutcome.Pass(listOf(
                    ev("device_secure", "screen lock is configured")))
                else -> CheckOutcome.Warning(
                    explanation = "no screen lock configured",
                    evidence = listOf(ev("device_secure", "screen lock is not configured")))
            }
        },
        CheckDefinition(
            id = "device_encryption",
            requiredInputs = listOf(INPUT_ENCRYPTION_ACTIVE)
        ) { inputs ->
            when (inputs.getValue(INPUT_ENCRYPTION_ACTIVE).value) {
                "true" -> CheckOutcome.Pass(listOf(
                    ev("encryption_active", "device storage encryption active")))
                else -> CheckOutcome.Warning(
                    explanation = "device storage encryption not active",
                    evidence = listOf(
                        ev("encryption_active", "device storage encryption not active")))
            }
        },
        CheckDefinition(
            id = "security_patch_level",
            requiredInputs = listOf(INPUT_SECURITY_PATCH)
        ) { inputs ->
            val patch = inputs.getValue(INPUT_SECURITY_PATCH).value
            if (patch.isNotBlank()) {
                CheckOutcome.Pass(listOf(
                    ev("security_patch", "platform reports patch level: $patch")))
            } else {
                CheckOutcome.Warning(
                    explanation = "platform did not report a security patch level",
                    evidence = listOf(
                        ev("security_patch", "patch level empty or unreadable")))
            }
        },
        CheckDefinition(
            id = "verified_boot",
            requiredInputs = listOf(INPUT_VERIFIED_BOOT)
        ) { inputs ->
            when (val state = inputs.getValue(INPUT_VERIFIED_BOOT).value) {
                "green" -> CheckOutcome.Pass(listOf(
                    ev("verified_boot_state", "chain verified: green")))
                "yellow", "orange", "red" -> CheckOutcome.Warning(
                    explanation = "verified boot state requires attention: $state",
                    evidence = listOf(
                        ev("verified_boot_state", "chain state: $state")))
                else -> CheckOutcome.Unavailable(
                    reason = "verified-boot state not readable in this integration stage")
            }
        },
        CheckDefinition(
            id = "permissions_inventory",
            requiredInputs = listOf(INPUT_PERMISSION_USAGE)
        ) { inputs ->
            val digest = inputs.getValue(INPUT_PERMISSION_USAGE).value
            if (digest.isNotBlank()) {
                CheckOutcome.Pass(listOf(
                    ev("permission_usage_data", "usage data readable (${digest.length} chars)")))
            } else {
                CheckOutcome.Unavailable(
                    reason = "permission usage APIs are privileged; " +
                        "unavailable to a standalone app")
            }
        },
        CheckDefinition(
            id = "automation_rules_review",
            requiredInputs = listOf(INPUT_AUTOMATION_DIGEST)
        ) { inputs ->
            val digest = inputs.getValue(INPUT_AUTOMATION_DIGEST).value
            if (digest.isNotBlank()) {
                CheckOutcome.Pass(listOf(
                    ev("automation_rules_digest", "rules digest readable (${digest.length} chars)")))
            } else {
                CheckOutcome.Unavailable(
                    reason = "automation rule state lives in DRS Automation; " +
                        "cross-app read path pending integration")
            }
        }
    )
}
