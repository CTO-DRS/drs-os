/*
 * SPDX-License-Identifier: Apache-2.0
 */
package com.drs.os.engine.host

import com.drs.os.engine.ActionRequest
import com.drs.os.engine.AuditRecord
import com.drs.os.engine.Condition
import com.drs.os.engine.DecisionReason
import com.drs.os.engine.EventContext
import com.drs.os.engine.Rule
import com.drs.os.engine.Trigger
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class HostRuntimeTest {

    private fun morningTheme() = Rule(
        id = "morning", name = "Morning theme", enabled = true,
        trigger = Trigger.TimeDaily("07:30"),
        conditions = listOf(Condition.TimeWindow("07:00", "08:00")),
        actionId = "set_theme_light", requiresUnlock = true,
        maxRunsPerDay = 1, explanation = "تغيير الثيم صباحاً")

    private fun evaluate(
        store: HostStateStore,
        rule: Rule = morningTheme(),
        epochDay: Long = 20_000L,
        minute: Int = 450,   // 07:30
        unlocked: Boolean = true,
        battery: Int = 80,
        timestamp: String = "2026-10-16T07:30:00Z"
    ) = HostRuntime.evaluateWithHost(
        rule, epochDay, minute, weekday = 2, battery = battery, charging = false,
        deviceUnlocked = unlocked, timestamp = timestamp, state = store)

    @Test
    fun `fired evaluation records dedup key audit and run count`() {
        val store = InMemoryHostStateStore()
        val ev = evaluate(store)
        assertEquals(false, ev.skippedDuplicate)
        assertEquals(DecisionReason.FIRED, ev.decision?.reason)
        assertEquals(1, ev.actionRequests.size)
        assertEquals(Pair(20_000L, 450), store.lastEvaluationMinute("morning"))
        assertEquals(1, store.runsToday("morning", 20_000L))
        assertEquals(1, store.auditLog().size)
        val audit = store.auditLog().single()
        assertEquals("fired", audit.reason)
        assertEquals("set_theme_light", audit.actionId)
        assertEquals("2026-10-16T07:30:00Z", audit.timestamp)
        assertEquals(450, audit.context.nowMinutes)
    }

    @Test
    fun `duplicate evaluation in same calendar minute is suppressed`() {
        val store = InMemoryHostStateStore()
        evaluate(store)
        val second = evaluate(store, timestamp = "2026-10-16T07:30:30Z")
        assertTrue(second.skippedDuplicate)
        assertEquals(null, second.decision)
        assertEquals(0, second.actionRequests.size)
        assertEquals(1, store.auditLog().size)   // suppressed: no extra audit
        assertEquals(1, store.runsToday("morning", 20_000L))
    }

    @Test
    fun `next minute evaluates again - host dedup is per calendar minute`() {
        val store = InMemoryHostStateStore()
        // Level-style trigger so it is active on every minute; the daily
        // cap (max 1) must still bound the repetitions.
        val batteryRule = Rule(
            id = "b", name = "b", enabled = true,
            trigger = Trigger.BatteryBelow(20),
            actionId = "enable_battery_saver", maxRunsPerDay = 1,
            explanation = "موفّر الطاقة")
        evaluate(store, rule = batteryRule, battery = 10)
        val next = evaluate(store, rule = batteryRule, minute = 451, battery = 10)
        assertFalse(next.skippedDuplicate)
        assertEquals(DecisionReason.MAX_RUNS_REACHED, next.decision?.reason)
        assertEquals(1, store.runsToday("b", 20_000L))
    }

    @Test
    fun `day rollover resets per-day counter and dedup window`() {
        val store = InMemoryHostStateStore()
        evaluate(store, epochDay = 20_000L)
        val nextDay = evaluate(store, epochDay = 20_001L)
        assertFalse(nextDay.skippedDuplicate)
        assertEquals(DecisionReason.FIRED, nextDay.decision?.reason)
        assertEquals(1, store.runsToday("morning", 20_001L))
        assertEquals(0, store.runsToday("morning", 20_002L))
    }

    @Test
    fun `requires-unlock blocks firing but still audits`() {
        val store = InMemoryHostStateStore()
        val ev = evaluate(store, unlocked = false)
        assertEquals(DecisionReason.REQUIRES_UNLOCK, ev.decision?.reason)
        assertEquals(0, ev.actionRequests.size)
        assertEquals(0, store.runsToday("morning", 20_000L))
        assertEquals(1, store.auditLog().size)
        assertEquals("requires-unlock", store.auditLog().single().reason)
    }

    @Test
    fun `audit records contain no personal data beyond rule explanation`() {
        val store = InMemoryHostStateStore()
        evaluate(store)
        val flat = store.auditLog().single().toFlatMap()
        val expectedKeys = setOf(
            "rule_id", "reason", "action_id", "explanation",
            "ctx_now_minutes", "ctx_weekday", "ctx_battery", "ctx_charging",
            "ctx_device_unlocked", "ctx_runs_today", "timestamp")
        assertEquals(expectedKeys, flat.keys.toSet())
        // Context values stay numeric/boolean state; no free-text fields
        // beyond the rule's own user-visible explanation.
        assertTrue(flat["ctx_battery"]!!.toInt() in 0..100)
    }

    @Test
    fun `persistence recovery - dedup and counters survive restart`() {
        val original = InMemoryHostStateStore()
        evaluate(original)
        val snapshot = InMemoryHostStateStore.snapshot(original)
        // simulate restart into a fresh store restored from durable state
        val restored = InMemoryHostStateStore.restore(snapshot)
        assertEquals(Pair(20_000L, 450), restored.lastEvaluationMinute("morning"))
        assertEquals(1, restored.runsToday("morning", 20_000L))
        assertEquals(1, restored.auditLog().size)
        val afterRestart = evaluate(restored, timestamp = "2026-10-16T07:30:59Z")
        assertTrue(afterRestart.skippedDuplicate)
        // a rejected snapshot key fails loudly (strict recovery)
        assertTrue(try {
            InMemoryHostStateStore.restore(mapOf("mystery.key" to "1"))
            false
        } catch (e: IllegalArgumentException) {
            true
        })
    }

    @Test
    fun `audit snapshot round trip preserves every field`() {
        val store = InMemoryHostStateStore()
        evaluate(store)
        val snap = InMemoryHostStateStore.snapshot(store)
        val restored = InMemoryHostStateStore.restore(snap)
        assertEquals(store.auditLog(), restored.auditLog())
    }

    // ------------------------------------------------ execution coordinator

    private class RecordingExecutor(
        override val actionId: String,
        private val behavior: (ActionRequest) -> ExecutionResult
    ) : ActionExecutor {
        val seen = ArrayList<ActionRequest>()
        override fun execute(request: ActionRequest, token: CancellationToken): ExecutionResult {
            seen.add(request)
            return behavior(request)
        }
    }

    private val req1 = ActionRequest("morning", "set_theme_light", "e1")
    private val req2 = ActionRequest("morning2", "set_dnd_on", "e2")

    @Test
    fun `executes registered actions in deterministic order`() {
        val exLight = RecordingExecutor("set_theme_light") { ExecutionResult.Executed }
        val exDnd = RecordingExecutor("set_dnd_on") { ExecutionResult.Executed }
        val records = ExecutionCoordinator.run(
            listOf(req1, req2), mapOf("set_theme_light" to exLight, "set_dnd_on" to exDnd),
            CancellationToken())
        assertEquals(
            listOf(ExecutionStatus.EXECUTED, ExecutionStatus.EXECUTED),
            records.map { it.status })
        assertEquals(listOf(req1), exLight.seen)
        assertEquals(listOf(req2), exDnd.seen)
    }

    @Test
    fun `unregistered action is recorded not guessed`() {
        val records = ExecutionCoordinator.run(
            listOf(req1), emptyMap(), CancellationToken())
        assertEquals(ExecutionStatus.NO_EXECUTOR, records.single().status)
    }

    @Test
    fun `failure carries stable error type`() {
        val failing = RecordingExecutor("set_theme_light") {
            ExecutionResult.Failed("platform-api-unavailable")
        }
        val records = ExecutionCoordinator.run(
            listOf(req1), mapOf("set_theme_light" to failing), CancellationToken())
        assertEquals(ExecutionStatus.FAILED, records.single().status)
        assertEquals("platform-api-unavailable", records.single().errorType)
    }

    @Test
    fun `executor exception is contained and classified`() {
        val throwing = RecordingExecutor("set_theme_light") {
            throw IllegalStateException("boom")
        }
        val records = ExecutionCoordinator.run(
            listOf(req1), mapOf("set_theme_light" to throwing), CancellationToken())
        assertEquals(ExecutionStatus.FAILED, records.single().status)
        assertEquals("uncaught-exception", records.single().errorType)
        // audit-style record carries no exception message
        assertFalse(records.single().toString().contains("boom"))
    }

    @Test
    fun `cancellation skips remaining requests and records them`() {
        val token = CancellationToken()
        val slow = RecordingExecutor("set_theme_light") {
            token.cancel()
            ExecutionResult.Executed
        }
        val records = ExecutionCoordinator.run(
            listOf(req1, req2), mapOf("set_theme_light" to slow, "set_dnd_on" to slow),
            token)
        assertEquals(ExecutionStatus.EXECUTED, records[0].status)
        assertEquals(ExecutionStatus.CANCELLED, records[1].status)
    }

    @Test
    fun `per-trigger action limit is enforced with explicit records`() {
        val ex = RecordingExecutor("set_theme_light") { ExecutionResult.Executed }
        val many = (1..5).map { ActionRequest("r$it", "set_theme_light", "e") }
        val records = ExecutionCoordinator.run(
            many, mapOf("set_theme_light" to ex), CancellationToken(),
            CoordinatorLimits(maxActionsPerTrigger = 3))
        assertEquals(
            listOf("EXECUTED", "EXECUTED", "EXECUTED", "SKIPPED_LIMIT", "SKIPPED_LIMIT"),
            records.map { it.status.name })
    }
}
