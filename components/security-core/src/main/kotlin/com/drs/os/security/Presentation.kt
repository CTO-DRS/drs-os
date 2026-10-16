/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS OS Security Center — presentation-state mapping (locale-free).
 * Maps CheckResults to UI models: stable resource keys, severity
 * ordering, and honest summary counts. The Compose layer binds these
 * keys to ar/en strings; RTL and typography live there too.
 */
package com.drs.os.security

data class CheckItemUi(
    val id: String,
    val state: CheckState,
    /** 3 = needs attention, 2 = cannot verify, 1 = pass, 0 = not checked. */
    val severityRank: Int,
    /** Res key, e.g. "check_screen_lock_title". */
    val titleKey: String,
    /** Res key for the state word, e.g. "state_pass". */
    val stateKey: String,
    /** Res key for the explanation sentence, e.g. "check_screen_lock_pass_detail". */
    val detailKey: String,
    /** Free-form evidence lines produced by the check (already honest text). */
    val evidenceLines: List<String>
)

data class ChecklistSummaryUi(
    val pass: Int,
    val warning: Int,
    val unavailable: Int,
    val notChecked: Int,
    /** The state that should drive the dashboard banner. */
    val overall: CheckState
)

object ChecklistPresenter {

    fun severity(state: CheckState): Int = when (state) {
        CheckState.WARNING -> 3
        CheckState.UNAVAILABLE -> 2
        CheckState.PASS -> 1
        CheckState.NOT_CHECKED -> 0
    }

    /**
     * Overall state = the most severe present state, with UNAVAILABLE
     * outranked by WARNING only. A NOT_CHECKED-only list stays
     * NOT_CHECKED (never presented as healthy).
     */
    fun overall(results: List<CheckResult>): CheckState {
        val states = results.map { it.state }
        return when {
            states.isEmpty() -> CheckState.NOT_CHECKED
            states.any { it == CheckState.WARNING } -> CheckState.WARNING
            states.any { it == CheckState.UNAVAILABLE } -> CheckState.UNAVAILABLE
            states.all { it == CheckState.PASS } -> CheckState.PASS
            else -> CheckState.NOT_CHECKED
        }
    }

    fun summarize(results: List<CheckResult>): ChecklistSummaryUi = ChecklistSummaryUi(
        pass = results.count { it.state == CheckState.PASS },
        warning = results.count { it.state == CheckState.WARNING },
        unavailable = results.count { it.state == CheckState.UNAVAILABLE },
        notChecked = results.count { it.state == CheckState.NOT_CHECKED },
        overall = overall(results)
    )

    /** Presentation list sorted by severity (attention first), id as tie-break. */
    fun toUiItems(results: List<CheckResult>): List<CheckItemUi> =
        results.map { r ->
            val outcome = r.outcome
            CheckItemUi(
                id = r.id,
                state = r.state,
                severityRank = severity(r.state),
                titleKey = "check_${r.id}_title",
                stateKey = "state_${r.state.name.lowercase()}",
                detailKey = "check_${r.id}_${detailSuffix(outcome)}_detail",
                evidenceLines = when (outcome) {
                    is CheckOutcome.Pass -> outcome.evidence.map { it.detail }
                    is CheckOutcome.Warning -> outcome.evidence.map { it.detail }
                    is CheckOutcome.Unavailable ->
                        listOf(outcome.reason) + outcome.evidence.map { it.detail }
                    is CheckOutcome.NotChecked ->
                        listOfNotNull(outcome.reason)
                }
            )
        }.sortedWith(compareByDescending<CheckItemUi> { it.severityRank }.thenBy { it.id })

    private fun detailSuffix(outcome: CheckOutcome): String = when (outcome) {
        is CheckOutcome.Pass -> "pass"
        is CheckOutcome.Warning -> "warning"
        is CheckOutcome.Unavailable -> "unavailable"
        is CheckOutcome.NotChecked -> "notchecked"
    }
}
