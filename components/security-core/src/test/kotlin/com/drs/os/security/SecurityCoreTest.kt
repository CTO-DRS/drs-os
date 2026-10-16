/*
 * SPDX-License-Identifier: Apache-2.0
 */
package com.drs.os.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

private class MapProvider(private val map: Map<String, CheckInput>) : CheckDataProvider {
    override fun provide(inputId: String): CheckInput? = map[inputId]
}

class SecurityCoreTest {

    private val defs = DrsChecklistV1.definitions

    private fun def(id: String) = defs.first { it.id == id }

    // ------------------------------------------------------------ evaluator

    @Test
    fun `missing provider input becomes UNAVAILABLE not a guess`() {
        val r = ChecklistEvaluator.evaluate(def("screen_lock"), MapProvider(emptyMap()))
        assertEquals(CheckState.UNAVAILABLE, r.state)
        assertEquals("missing-input:device_secure", (r.outcome as CheckOutcome.Unavailable).reason)
    }

    @Test
    fun `unavailable input propagates with its reason`() {
        val r = ChecklistEvaluator.evaluate(
            def("verified_boot"),
            MapProvider(mapOf(DrsChecklistV1.INPUT_VERIFIED_BOOT to
                CheckInput.Unavailable("requires platform integration"))))
        assertEquals(CheckState.UNAVAILABLE, r.state)
        assertEquals("requires platform integration", (r.outcome as CheckOutcome.Unavailable).reason)
    }

    @Test
    fun `not-checked input propagates`() {
        val r = ChecklistEvaluator.evaluate(
            def("screen_lock"),
            MapProvider(mapOf(DrsChecklistV1.INPUT_DEVICE_SECURE to
                CheckInput.NotChecked("user skipped"))))
        assertEquals(CheckState.NOT_CHECKED, r.state)
        assertEquals("user skipped", (r.outcome as CheckOutcome.NotChecked).reason)
    }

    @Test
    fun `throwing definition is contained as internal error`() {
        val hostile = CheckDefinition(id = "hostile", requiredInputs = listOf("x")) { _ ->
            error("boom")
        }
        val r = ChecklistEvaluator.evaluate(hostile, MapProvider(mapOf("x" to CheckInput.Available("1"))))
        assertEquals(CheckState.UNAVAILABLE, r.state)
        assertEquals("internal-error", (r.outcome as CheckOutcome.Unavailable).reason)
    }

    @Test
    fun `duplicate check ids are rejected`() {
        val dup = listOf(def("screen_lock"), def("screen_lock"))
        assertTrue(try {
            ChecklistEvaluator.evaluateAll(dup, MapProvider(emptyMap()))
            false
        } catch (e: IllegalArgumentException) {
            true
        })
    }

    @Test
    fun `evaluateAll preserves deterministic order`() {
        val results = ChecklistEvaluator.evaluateAll(
            defs, MapProvider(mapOf(
                DrsChecklistV1.INPUT_DEVICE_SECURE to CheckInput.Available("true"),
                DrsChecklistV1.INPUT_ENCRYPTION_ACTIVE to CheckInput.Available("true"),
                DrsChecklistV1.INPUT_SECURITY_PATCH to CheckInput.Available("2026-09-01"))))
        assertEquals(defs.map { it.id }, results.map { it.id })
    }

    // ---------------------------------------------------------- v1 checks

    @Test
    fun `screen lock passes and warns on both inputs`() {
        val pass = ChecklistEvaluator.evaluate(
            def("screen_lock"),
            MapProvider(mapOf(DrsChecklistV1.INPUT_DEVICE_SECURE to CheckInput.Available("true"))))
        assertEquals(CheckState.PASS, pass.state)
        assertTrue((pass.outcome as CheckOutcome.Pass).evidence.isNotEmpty())

        val warn = ChecklistEvaluator.evaluate(
            def("screen_lock"),
            MapProvider(mapOf(DrsChecklistV1.INPUT_DEVICE_SECURE to CheckInput.Available("false"))))
        assertEquals(CheckState.WARNING, warn.state)
        assertTrue((warn.outcome as CheckOutcome.Warning).explanation.isNotBlank())
    }

    @Test
    fun `verified boot green passes and colored states warn`() {
        val green = ChecklistEvaluator.evaluate(
            def("verified_boot"),
            MapProvider(mapOf(DrsChecklistV1.INPUT_VERIFIED_BOOT to CheckInput.Available("green"))))
        assertEquals(CheckState.PASS, green.state)
        for (state in listOf("yellow", "orange", "red")) {
            val r = ChecklistEvaluator.evaluate(
                def("verified_boot"),
                MapProvider(mapOf(DrsChecklistV1.INPUT_VERIFIED_BOOT to CheckInput.Available(state))))
            assertEquals(CheckState.WARNING, r.state)
        }
    }

    @Test
    fun `empty patch level warns, real patch passes with value in evidence`() {
        val warn = ChecklistEvaluator.evaluate(
            def("security_patch_level"),
            MapProvider(mapOf(DrsChecklistV1.INPUT_SECURITY_PATCH to CheckInput.Available(""))))
        assertEquals(CheckState.WARNING, warn.state)
        val pass = ChecklistEvaluator.evaluate(
            def("security_patch_level"),
            MapProvider(mapOf(DrsChecklistV1.INPUT_SECURITY_PATCH to CheckInput.Available("2026-09-01"))))
        assertEquals(CheckState.PASS, pass.state)
        assertTrue(
            (pass.outcome as CheckOutcome.Pass).evidence.single().detail.contains("2026-09-01"))
    }

    @Test
    fun `privileged inputs produce honest unavailability in standalone v1`() {
        // (a) Provider knows nothing → every check reports UNAVAILABLE
        //     with a missing-input reason; nothing is guessed.
        val none = ChecklistEvaluator.evaluateAll(defs, MapProvider(emptyMap()))
            .associateBy { it.id }
        assertTrue(none.values.all { it.state == CheckState.UNAVAILABLE })
        assertEquals(
            "missing-input:permission_usage_data",
            (none.getValue("permissions_inventory").outcome as CheckOutcome.Unavailable).reason)
        // (b) Provider explicitly reports the capability as unreadable →
        //     the check's own honest explanation surfaces.
        val explicit = ChecklistEvaluator.evaluate(
            def("permissions_inventory"),
            MapProvider(mapOf(DrsChecklistV1.INPUT_PERMISSION_USAGE to
                CheckInput.Unavailable("permission usage APIs are privileged; " +
                    "unavailable to a standalone app"))))
        assertEquals(CheckState.UNAVAILABLE, explicit.state)
        assertTrue(
            (explicit.outcome as CheckOutcome.Unavailable).reason.contains("privileged"))
    }

    @Test
    fun `determinism - same provider same results`() {
        val provider = MapProvider(mapOf(
            DrsChecklistV1.INPUT_DEVICE_SECURE to CheckInput.Available("true"),
            DrsChecklistV1.INPUT_ENCRYPTION_ACTIVE to CheckInput.Available("false")))
        assertEquals(
            ChecklistEvaluator.evaluateAll(defs, provider),
            ChecklistEvaluator.evaluateAll(defs, provider))
    }
}
