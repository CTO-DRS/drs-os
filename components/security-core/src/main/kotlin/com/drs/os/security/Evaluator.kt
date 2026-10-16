/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS OS Security Center — deterministic checklist evaluation.
 */
package com.drs.os.security

object ChecklistEvaluator {

    /**
     * Evaluate one definition against the provider. Guarantees:
     *  * any missing input id → UNAVAILABLE("missing-input:<id>") — the
     *    provider's gaps are surfaced, never guessed around;
     *  * any NotChecked input → NOT_CHECKED (propagated, reason kept);
     *  * any Unavailable input → UNAVAILABLE (propagated, reason kept);
     *  * an exception thrown by a definition is contained and reported
     *    as UNAVAILABLE("internal-error") — a broken check must not
     *    become a fake PASS or crash the dashboard.
     */
    fun evaluate(definition: CheckDefinition, provider: CheckDataProvider): CheckResult {
        val available = LinkedHashMap<String, CheckInput.Available>()
        for (inputId in definition.requiredInputs) {
            when (val input = provider.provide(inputId)) {
                null ->
                    return result(definition.id, CheckOutcome.Unavailable(
                        reason = "missing-input:$inputId"))
                is CheckInput.NotChecked ->
                    return result(definition.id, CheckOutcome.NotChecked(input.reason))
                is CheckInput.Unavailable ->
                    return result(definition.id, CheckOutcome.Unavailable(
                        reason = input.reason))
                is CheckInput.Available ->
                    available[inputId] = input
            }
        }
        val outcome = try {
            definition.evaluate(available)
        } catch (e: Exception) {
            CheckOutcome.Unavailable(reason = "internal-error")
        }
        return result(definition.id, outcome)
    }

    /**
     * Evaluate definitions in the given order (deterministic; the list
     * order is the presentation order). Duplicate ids are rejected —
     * they would make summaries lie.
     */
    fun evaluateAll(
        definitions: List<CheckDefinition>,
        provider: CheckDataProvider
    ): List<CheckResult> {
        val ids = definitions.map { it.id }
        require(ids.size == ids.toSet().size) { "duplicate check ids: $ids" }
        return definitions.map { evaluate(it, provider) }
    }

    private fun result(id: String, outcome: CheckOutcome): CheckResult =
        CheckResult(
            id = id,
            state = when (outcome) {
                is CheckOutcome.Pass -> CheckState.PASS
                is CheckOutcome.Warning -> CheckState.WARNING
                is CheckOutcome.Unavailable -> CheckState.UNAVAILABLE
                is CheckOutcome.NotChecked -> CheckState.NOT_CHECKED
            },
            outcome = outcome
        )
}
