/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS OS Security Center — core domain model (Phase 3, Workstream B).
 *
 * Non-negotiable properties:
 *  * Four honest states only: PASS, WARNING, UNAVAILABLE, NOT_CHECKED.
 *    There is no "invented green": a check is PASS only when evidence
 *    says so, and UNAVAILABLE with an explanation when the platform
 *    cannot supply the input (e.g. no privileged APIs in the current
 *    integration stage).
 *  * Deterministic: identical inputs → identical results.
 *  * Locale-free: this module produces stable string KEYS and evidence;
 *    human-readable translations live in the app resources (ar/en).
 *  * No AI, no scoring beyond counts of the four states.
 */
package com.drs.os.security

enum class CheckState {
    PASS,
    WARNING,
    UNAVAILABLE,
    NOT_CHECKED
}

data class CheckEvidence(
    /** Stable id, e.g. "keyguard_is_device_secure". */
    val id: String,
    /** Human-readable detail (short, non-sensitive). */
    val detail: String
)

/** What the platform layer supplies to a check. */
sealed interface CheckInput {
    /** A usable value; semantics are defined per input id. */
    data class Available(val value: String) : CheckInput

    /** The platform answered: this capability cannot be read here. */
    data class Unavailable(val reason: String) : CheckInput

    /** The check was deliberately skipped (user or policy). */
    data class NotChecked(val reason: String? = null) : CheckInput
}

interface CheckDataProvider {
    /** Null means the provider does not know this input id at all. */
    fun provide(inputId: String): CheckInput?
}

sealed interface CheckOutcome {
    data class Pass(val evidence: List<CheckEvidence>) : CheckOutcome
    data class Warning(val explanation: String, val evidence: List<CheckEvidence>) : CheckOutcome
    data class Unavailable(val reason: String, val evidence: List<CheckEvidence> = emptyList()) : CheckOutcome
    data class NotChecked(val reason: String?) : CheckOutcome
}

/**
 * One deterministic check. [evaluate] receives the inputs named in
 * [requiredInputs]; [ChecklistEvaluator] guarantees all of them are
 * present and Available before calling it.
 */
data class CheckDefinition(
    val id: String,
    val requiredInputs: List<String>,
    val evaluate: (Map<String, CheckInput.Available>) -> CheckOutcome
)

data class CheckResult(
    val id: String,
    val state: CheckState,
    val outcome: CheckOutcome
)
