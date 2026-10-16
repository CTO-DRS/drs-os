/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Differential verification: the Kotlin engine must produce exactly the
 * decisions the Phase 2 Python reference model produced for the same
 * rule/context pairs. The vectors are committed
 * (tools/automation-reference/test_vectors.json) and byte-stable —
 * regenerate with tools/automation-reference/generate_test_vectors.py
 * and diff to prove it.
 */
package com.drs.os.engine

import com.drs.os.foundation.MiniJson
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.TestFactory
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.stream.Stream

class DifferentialVectorsTest {

    private data class VectorCase(
        val caseId: String,
        val rule: Rule,
        val ctx: EventContext,
        val expectedFired: Boolean,
        val expectedReason: String,
        val expectedActionId: String?,
        val expectedExplanation: String
    )

    private fun vectorsPath(): Path {
        val fromProperty = System.getProperty("drs.vectors")
        val candidates = listOfNotNull(
            fromProperty,
            "tools/automation-reference/test_vectors.json",
            "../../tools/automation-reference/test_vectors.json",
            "../../../tools/automation-reference/test_vectors.json"
        )
        for (c in candidates) {
            val p = Paths.get(c)
            if (Files.exists(p)) return p
        }
        throw IllegalStateException(
            "differential vectors file not found; looked in: $candidates — " +
                "run the suite from the repository root or set -Ddrs.vectors=<path>")
    }

    private fun loadVectors(): List<VectorCase> {
        val text = Files.readString(vectorsPath())
        val root = MiniJson.parse(text) as MiniJson.JsonValue.JArr
        assertTrue(root.items.size >= 30, "expected a substantial vector set")
        return root.items.map { item ->
            val obj = item as MiniJson.JsonValue.JObj
            val caseId = (obj.entries["case_id"] as MiniJson.JsonValue.JStr).value
            val rule = RuleJson.parseRule(
                obj.entries["rule"]
                    ?: throw AssertionError("vector $caseId: missing rule"))
            val ctx = RuleJson.parseContext(
                obj.entries["context"]
                    ?: throw AssertionError("vector $caseId: missing context"))
            val expected = obj.entries["expected"] as MiniJson.JsonValue.JObj
            VectorCase(
                caseId = caseId,
                rule = rule,
                ctx = ctx,
                expectedFired =
                    (expected.entries["fired"] as MiniJson.JsonValue.JBool).value,
                expectedReason =
                    (expected.entries["reason"] as MiniJson.JsonValue.JStr).value,
                expectedActionId = when (val a = expected.entries["action_id"]) {
                    is MiniJson.JsonValue.JStr -> a.value
                    is MiniJson.JsonValue.JNull -> null
                    else -> throw AssertionError("vector $caseId: bad action_id")
                },
                expectedExplanation =
                    (expected.entries["explanation"] as MiniJson.JsonValue.JStr).value
            )
        }
    }

    @TestFactory
    fun kotlinEngineMatchesPythonReference(): Stream<DynamicTest> =
        loadVectors().map { c ->
            DynamicTest.dynamicTest(c.caseId) {
                val decision = Evaluator.evaluate(c.rule, c.ctx)
                assertEquals(c.expectedFired, decision.fired, "fired mismatch")
                assertEquals(c.expectedReason, decision.reason.wire, "reason mismatch")
                assertEquals(c.expectedActionId, decision.actionId, "action_id mismatch")
                assertEquals(c.expectedExplanation, decision.explanation, "explanation mismatch")
            }
        }.stream()
}
