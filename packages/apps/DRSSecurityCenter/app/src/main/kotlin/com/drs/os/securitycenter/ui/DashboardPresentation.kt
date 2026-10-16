/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Dashboard presentation mapping (pure, JVM-testable). Converts the
 * security-core results into UI state with loading/error semantics and
 * honest banner logic. No Android imports.
 */
package com.drs.os.securitycenter.ui

import com.drs.os.security.CheckDataProvider
import com.drs.os.security.ChecklistEvaluator
import com.drs.os.security.ChecklistPresenter
import com.drs.os.security.CheckItemUi
import com.drs.os.security.ChecklistSummaryUi
import com.drs.os.security.CheckResult
import com.drs.os.security.CheckState

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Ready(
        val items: List<CheckItemUi>,
        val summary: ChecklistSummaryUi
    ) : DashboardUiState

    data object Failed : DashboardUiState
}

object DashboardPresentation {

    /** Deterministic mapping used by the ViewModel and unit tests. */
    fun present(results: List<CheckResult>): DashboardUiState.Ready = DashboardUiState.Ready(
        items = ChecklistPresenter.toUiItems(results),
        summary = ChecklistPresenter.summarize(results)
    )

    /** Full evaluation pass; any provider failure becomes Failed (never fake data). */
    fun evaluate(provider: CheckDataProvider): DashboardUiState {
        return try {
            present(ChecklistEvaluator.evaluateAll(drsChecklist(), provider))
        } catch (e: Exception) {
            DashboardUiState.Failed
        }
    }
}

/**
 * Resolves the stable string keys produced by security-core's
 * ChecklistPresenter to app resources. Unknown keys fall back to a
 * generic resource instead of crashing or rendering a raw key.
 */
object ResKeys {

    val CHECK_IDS = listOf(
        "screen_lock",
        "device_encryption",
        "security_patch_level",
        "verified_boot",
        "permissions_inventory",
        "automation_rules_review"
    )

    private val TITLES = mapOf(
        "screen_lock" to R.string.check_screen_lock_title,
        "device_encryption" to R.string.check_device_encryption_title,
        "security_patch_level" to R.string.check_security_patch_level_title,
        "verified_boot" to R.string.check_verified_boot_title,
        "permissions_inventory" to R.string.check_permissions_inventory_title,
        "automation_rules_review" to R.string.check_automation_rules_review_title
    )

    private val DETAILS = CHECK_IDS.associateWith { id ->
        mapOf(
            "pass" to detailRes(id, "pass"),
            "warning" to detailRes(id, "warning"),
            "unavailable" to detailRes(id, "unavailable"),
            "notchecked" to detailRes(id, "notchecked")
        )
    }

    private fun detailRes(id: String, suffix: String): Int = when (id) {
        "screen_lock" -> when (suffix) {
            "pass" -> R.string.check_screen_lock_pass_detail
            "warning" -> R.string.check_screen_lock_warning_detail
            "unavailable" -> R.string.check_screen_lock_unavailable_detail
            else -> R.string.check_screen_lock_notchecked_detail
        }
        "device_encryption" -> when (suffix) {
            "pass" -> R.string.check_device_encryption_pass_detail
            "warning" -> R.string.check_device_encryption_warning_detail
            "unavailable" -> R.string.check_device_encryption_unavailable_detail
            else -> R.string.check_device_encryption_notchecked_detail
        }
        "security_patch_level" -> when (suffix) {
            "pass" -> R.string.check_security_patch_level_pass_detail
            "warning" -> R.string.check_security_patch_level_warning_detail
            "unavailable" -> R.string.check_security_patch_level_unavailable_detail
            else -> R.string.check_security_patch_level_notchecked_detail
        }
        "verified_boot" -> when (suffix) {
            "pass" -> R.string.check_verified_boot_pass_detail
            "warning" -> R.string.check_verified_boot_warning_detail
            "unavailable" -> R.string.check_verified_boot_unavailable_detail
            else -> R.string.check_verified_boot_notchecked_detail
        }
        "permissions_inventory" -> when (suffix) {
            "pass" -> R.string.check_permissions_inventory_pass_detail
            "warning" -> R.string.check_permissions_inventory_warning_detail
            "unavailable" -> R.string.check_permissions_inventory_unavailable_detail
            else -> R.string.check_permissions_inventory_notchecked_detail
        }
        else -> when (suffix) {
            "pass" -> R.string.check_automation_rules_review_pass_detail
            "warning" -> R.string.check_automation_rules_review_warning_detail
            "unavailable" -> R.string.check_automation_rules_review_unavailable_detail
            else -> R.string.check_automation_rules_review_notchecked_detail
        }
    }

    fun titleRes(key: String): Int =
        TITLES[key] ?: R.string.app_name  // fallback: app name is honest, never a raw key

    fun detailResFor(id: String, suffix: String): Int =
        DETAILS[id]?.get(suffix) ?: R.string.error_generic

    fun bannerRes(overall: CheckState): Int = when (overall) {
        CheckState.PASS -> R.string.banner_pass
        CheckState.WARNING -> R.string.banner_warning
        CheckState.UNAVAILABLE -> R.string.banner_unavailable
        CheckState.NOT_CHECKED -> R.string.banner_notchecked
    }

    fun stateRes(state: CheckState): Int = when (state) {
        CheckState.PASS -> R.string.state_pass
        CheckState.WARNING -> R.string.state_warning
        CheckState.UNAVAILABLE -> R.string.state_unavailable
        CheckState.NOT_CHECKED -> R.string.state_notchecked
    }
}

private fun drsChecklist() = com.drs.os.security.DrsChecklistV1.definitions
