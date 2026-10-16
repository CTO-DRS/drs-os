/*
 * SPDX-License-Identifier: Apache-2.0
 */
package com.drs.os.security

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PresentationTest {

    private fun r(id: String, state: CheckState) = CheckResult(id, state, when (state) {
        CheckState.PASS -> CheckOutcome.Pass(emptyList())
        CheckState.WARNING -> CheckOutcome.Warning("w", emptyList())
        CheckState.UNAVAILABLE -> CheckOutcome.Unavailable("u")
        CheckState.NOT_CHECKED -> CheckOutcome.NotChecked(null)
    })

    @Test
    fun `severity ordering is warning over unavailable over pass over notchecked`() {
        assertEquals(3, ChecklistPresenter.severity(CheckState.WARNING))
        assertEquals(2, ChecklistPresenter.severity(CheckState.UNAVAILABLE))
        assertEquals(1, ChecklistPresenter.severity(CheckState.PASS))
        assertEquals(0, ChecklistPresenter.severity(CheckState.NOT_CHECKED))
    }

    @Test
    fun `overall picks the most attention-worthy state`() {
        assertEquals(CheckState.NOT_CHECKED, ChecklistPresenter.overall(emptyList()))
        assertEquals(
            CheckState.WARNING,
            ChecklistPresenter.overall(listOf(r("a", CheckState.PASS), r("b", CheckState.WARNING))))
        assertEquals(
            CheckState.UNAVAILABLE,
            ChecklistPresenter.overall(listOf(r("a", CheckState.PASS), r("b", CheckState.UNAVAILABLE))))
        assertEquals(
            CheckState.PASS,
            ChecklistPresenter.overall(listOf(r("a", CheckState.PASS), r("b", CheckState.PASS))))
        assertEquals(
            CheckState.NOT_CHECKED,
            ChecklistPresenter.overall(listOf(r("a", CheckState.NOT_CHECKED), r("b", CheckState.NOT_CHECKED))))
        // A healthy list with one unchecked item is NOT presented as PASS.
        assertEquals(
            CheckState.NOT_CHECKED,
            ChecklistPresenter.overall(listOf(r("a", CheckState.PASS), r("b", CheckState.NOT_CHECKED))))
    }

    @Test
    fun `summary counts all four states`() {
        val s = ChecklistPresenter.summarize(listOf(
            r("a", CheckState.PASS), r("b", CheckState.PASS),
            r("c", CheckState.WARNING), r("d", CheckState.UNAVAILABLE),
            r("e", CheckState.NOT_CHECKED)))
        assertEquals(2, s.pass)
        assertEquals(1, s.warning)
        assertEquals(1, s.unavailable)
        assertEquals(1, s.notChecked)
        assertEquals(CheckState.WARNING, s.overall)
    }

    @Test
    fun `ui items carry stable resource keys`() {
        val items = ChecklistPresenter.toUiItems(listOf(r("screen_lock", CheckState.PASS)))
        val item = items.single()
        assertEquals("check_screen_lock_title", item.titleKey)
        assertEquals("state_pass", item.stateKey)
        assertEquals("check_screen_lock_pass_detail", item.detailKey)
    }

    @Test
    fun `ui items sorted by severity then id`() {
        val items = ChecklistPresenter.toUiItems(listOf(
            r("alpha", CheckState.PASS),
            r("zulu", CheckState.WARNING),
            r("mike", CheckState.UNAVAILABLE),
            r("bravo", CheckState.NOT_CHECKED)))
        assertEquals(listOf("zulu", "mike", "alpha", "bravo"), items.map { it.id })
        assertTrue(items.zipWithNext().all { (a, b) -> a.severityRank >= b.severityRank })
    }

    @Test
    fun `unavailable evidence includes the reason line`() {
        val items = ChecklistPresenter.toUiItems(listOf(r("x", CheckState.UNAVAILABLE)))
        assertTrue(items.single().evidenceLines.contains("u"))
    }

    @Test
    fun `v1 checklist keys are derivable for every definition`() {
        // Guards the ar/en resource parity contract: every definition id
        // must map to title + 4 detail suffixes.
        for (d in DrsChecklistV1.definitions) {
            val items = ChecklistPresenter.toUiItems(listOf(
                ChecklistEvaluator.evaluate(d, MapProvider(emptyMap()))))
            val item = items.single()
            assertEquals("check_${d.id}_title", item.titleKey)
            assertEquals("check_${d.id}_unavailable_detail", item.detailKey)
        }
    }

    private class MapProvider(private val map: Map<String, CheckInput>) : CheckDataProvider {
        override fun provide(inputId: String): CheckInput? = map[inputId]
    }
}
