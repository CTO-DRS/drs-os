/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS OS deterministic automation — host-layer helpers (spec §4).
 *
 * The engine itself executes nothing. These helpers give the host the
 * exact machinery the spec requires it to provide, implemented once,
 * testable on any JVM, and free of Android imports:
 *
 *  * calendar-minute dedup: at most one evaluation per rule per
 *    calendar minute (TIME_DAILY edge semantics);
 *  * per-rule per-day run counters fed back as `runs_today`;
 *  * append-only in-memory audit with a persistence snapshot;
 *  * an execution coordinator that turns fired ActionRequests into
 *    ExecutionRecords through a closed executor map, with a hard
 *    per-trigger action limit, cancellation support, and stable error
 *    classifications (no free-text, no personal data in records).
 */
package com.drs.os.engine.host

import com.drs.os.engine.ActionRequest
import com.drs.os.engine.AuditRecord
import com.drs.os.engine.Decision
import com.drs.os.engine.Evaluator
import com.drs.os.engine.EventContext
import com.drs.os.engine.Rule
import com.drs.os.foundation.KvStore
import com.drs.os.foundation.MiniJson

/** Cooperative cancellation signal handed to executors. */
class CancellationToken {
    @Volatile
    var cancelled: Boolean = false
        private set

    fun cancel() {
        cancelled = true
    }
}

sealed interface ExecutionResult {
    /** The action completed. */
    object Executed : ExecutionResult

    /**
     * The action did not complete. [errorType] is a stable
     * classification, never free text, so records carry no personal
     * data and stay comparable across releases.
     */
    data class Failed(val errorType: String) : ExecutionResult
}

/**
 * Executes one registered action id. Executors live in the host
 * (Android app / platform service) — the coordinator only routes to
 * executors that were explicitly registered.
 */
interface ActionExecutor {
    val actionId: String
    fun execute(request: ActionRequest, token: CancellationToken): ExecutionResult
}

enum class ExecutionStatus { EXECUTED, FAILED, CANCELLED, SKIPPED_LIMIT, NO_EXECUTOR }

data class ExecutionRecord(
    val ruleId: String,
    val actionId: String,
    val status: ExecutionStatus,
    val errorType: String? = null
)

data class CoordinatorLimits(
    /**
     * Hard ceiling on action executions per trigger pass. Requests
     * beyond the limit are recorded as SKIPPED_LIMIT, never executed.
     */
    val maxActionsPerTrigger: Int = 16
)

object ExecutionCoordinator {

    /**
     * Run action requests in the given (already deterministic) order.
     * Exactly one record per request. If cancellation is requested, the
     * remaining requests are recorded as CANCELLED — the audit trail
     * shows what did not happen, too.
     */
    fun run(
        requests: List<ActionRequest>,
        executors: Map<String, ActionExecutor>,
        token: CancellationToken,
        limits: CoordinatorLimits = CoordinatorLimits()
    ): List<ExecutionRecord> {
        val records = ArrayList<ExecutionRecord>(requests.size)
        var executedCount = 0
        for (request in requests) {
            when {
                token.cancelled ->
                    records.add(
                        ExecutionRecord(
                            request.ruleId, request.actionId,
                            ExecutionStatus.CANCELLED))
                executedCount >= limits.maxActionsPerTrigger ->
                    records.add(
                        ExecutionRecord(
                            request.ruleId, request.actionId,
                            ExecutionStatus.SKIPPED_LIMIT))
                else -> {
                    val executor = executors[request.actionId]
                    records.add(
                        when {
                            executor == null ->
                                ExecutionRecord(
                                    request.ruleId, request.actionId,
                                    ExecutionStatus.NO_EXECUTOR)
                            else -> {
                                executedCount++
                                val result = try {
                                    executor.execute(request, token)
                                } catch (e: Exception) {
                                    ExecutionResult.Failed("uncaught-exception")
                                }
                                when (result) {
                                    is ExecutionResult.Executed ->
                                        ExecutionRecord(
                                            request.ruleId, request.actionId,
                                            ExecutionStatus.EXECUTED)
                                    is ExecutionResult.Failed ->
                                        ExecutionRecord(
                                            request.ruleId, request.actionId,
                                            ExecutionStatus.FAILED,
                                            result.errorType)
                                }
                            }
                        }
                    )
                }
            }
        }
        return records
    }
}

/** Persisted host state (dedup keys, run counters, audit tail). */
interface HostStateStore {
    /** (epochDay, minuteOfDay) of the last recorded evaluation, if any. */
    fun lastEvaluationMinute(ruleId: String): Pair<Long, Int>?

    fun recordEvaluation(ruleId: String, epochDay: Long, minuteOfDay: Int)

    fun runsToday(ruleId: String, epochDay: Long): Int

    fun countRun(ruleId: String, epochDay: Long)

    fun appendAudit(record: AuditRecord)

    fun auditLog(): List<AuditRecord>
}

/**
 * In-memory implementation with a deterministic key/value snapshot for
 * persistence and recovery. Snapshot format:
 *   eval.<ruleId> = "<epochDay>,<minute>"
 *   runs.<epochDay>.<ruleId> = <count>
 *   audit.<seq padded to 12 digits> = <json flat map>
 */
class InMemoryHostStateStore private constructor(
    private val evalMinutes: LinkedHashMap<String, Pair<Long, Int>>,
    private val runCounts: LinkedHashMap<String, Int>,
    private val audit: MutableList<AuditRecord>
) : HostStateStore {

    constructor() : this(LinkedHashMap(), LinkedHashMap(), ArrayList())

    companion object {
        private const val AUDIT_CAPACITY = 500
        private const val EVAL_PREFIX = "eval."
        private const val RUNS_PREFIX = "runs."
        private const val AUDIT_PREFIX = "audit."

        /** Serialize state for durable storage (host chooses the sink). */
        fun snapshot(store: InMemoryHostStateStore): Map<String, String> {
            val out = LinkedHashMap<String, String>()
            for ((ruleId, pair) in store.evalMinutes) {
                out[EVAL_PREFIX + ruleId] = "${pair.first},${pair.second}"
            }
            for ((key, count) in store.runCounts) {
                out[RUNS_PREFIX + key] = count.toString()
            }
            var seq = 0
            for (record in store.audit) {
                val seqKey = seq.toString().padStart(12, '0')
                out[AUDIT_PREFIX + seqKey] =
                    MiniJson.write(MiniJson.JsonValue.JObj(flatten(record.toFlatMap())))
                seq++
            }
            return out
        }

        /** Rebuild state from a snapshot (recovery after restart). */
        fun restore(data: Map<String, String>): InMemoryHostStateStore {
            val evals = LinkedHashMap<String, Pair<Long, Int>>()
            val runs = LinkedHashMap<String, Int>()
            val audit = ArrayList<AuditRecord>()
            val sorted = data.toSortedMap()
            for ((key, value) in sorted) {
                when {
                    key.startsWith(EVAL_PREFIX) -> {
                        val parts = value.split(',')
                        if (parts.size != 2) {
                            throw IllegalArgumentException("corrupt eval entry: $key")
                        }
                        evals[key.removePrefix(EVAL_PREFIX)] =
                            Pair(parts[0].toLong(), parts[1].toInt())
                    }
                    key.startsWith(RUNS_PREFIX) ->
                        runs[key.removePrefix(RUNS_PREFIX)] = value.toInt()
                    key.startsWith(AUDIT_PREFIX) -> {
                        val parsed = MiniJson.parse(value) as MiniJson.JsonValue.JObj
                        val flat = LinkedHashMap<String, String>()
                        for ((k, v) in parsed.entries) {
                            flat[k] = (v as MiniJson.JsonValue.JStr).value
                        }
                        audit.add(AuditRecord.fromFlatMap(flat))
                    }
                    else -> throw IllegalArgumentException("unknown snapshot key: $key")
                }
            }
            return InMemoryHostStateStore(evals, runs, audit)
        }

        private fun flatten(flat: Map<String, String>): LinkedHashMap<String, MiniJson.JsonValue> {
            val out = LinkedHashMap<String, MiniJson.JsonValue>()
            for ((k, v) in flat) out[k] = MiniJson.JsonValue.JStr(v)
            return out
        }
    }

    override fun lastEvaluationMinute(ruleId: String): Pair<Long, Int>? =
        evalMinutes[ruleId]

    override fun recordEvaluation(ruleId: String, epochDay: Long, minuteOfDay: Int) {
        evalMinutes[ruleId] = Pair(epochDay, minuteOfDay)
    }

    override fun runsToday(ruleId: String, epochDay: Long): Int =
        runCounts["$epochDay.$ruleId"] ?: 0

    override fun countRun(ruleId: String, epochDay: Long) {
        val key = "$epochDay.$ruleId"
        runCounts[key] = (runCounts[key] ?: 0) + 1
    }

    override fun appendAudit(record: AuditRecord) {
        audit.add(record)
        if (audit.size > AUDIT_CAPACITY) {
            audit.removeAt(0)
        }
    }

    override fun auditLog(): List<AuditRecord> = audit.toList()
}

data class HostEvaluation(
    /** Null when the evaluation was suppressed (duplicate minute). */
    val decision: Decision?,
    val actionRequests: List<ActionRequest>,
    val skippedDuplicate: Boolean
)

object HostRuntime {

    /**
     * Calendar-minute dedup (spec §4): at most one evaluation per rule
     * per calendar minute. Returns false when this rule was already
     * evaluated in (epochDay, minuteOfDay).
     */
    fun shouldEvaluate(
        rule: Rule,
        epochDay: Long,
        minuteOfDay: Int,
        state: HostStateStore
    ): Boolean {
        val last = state.lastEvaluationMinute(rule.id) ?: return true
        return !(last.first == epochDay && last.second == minuteOfDay)
    }

    /**
     * One host-driven evaluation of [rule] at the given instant.
     * Supplies runs_today from the store, evaluates, records the
     * dedup key and the mandatory audit record, and counts a run when
     * the decision fires. [timestamp] is host-supplied (engine stays
     * clock-free).
     */
    fun evaluateWithHost(
        rule: Rule,
        epochDay: Long,
        minuteOfDay: Int,
        weekday: Int,
        battery: Int,
        charging: Boolean,
        deviceUnlocked: Boolean,
        timestamp: String,
        state: HostStateStore
    ): HostEvaluation {
        if (!shouldEvaluate(rule, epochDay, minuteOfDay, state)) {
            return HostEvaluation(null, emptyList(), skippedDuplicate = true)
        }
        val ctx = EventContext(
            nowMinutes = minuteOfDay,
            weekday = weekday,
            battery = battery,
            charging = charging,
            deviceUnlocked = deviceUnlocked,
            runsToday = state.runsToday(rule.id, epochDay)
        )
        val decision = Evaluator.evaluate(rule, ctx)
        state.recordEvaluation(rule.id, epochDay, minuteOfDay)
        state.appendAudit(
            AuditRecord(
                ruleId = rule.id,
                reason = decision.reason.wire,
                actionId = decision.actionId,
                explanation = decision.explanation,
                context = ctx,
                timestamp = timestamp))
        val requests =
            if (decision.fired && decision.actionId != null) {
                state.countRun(rule.id, epochDay)
                listOf(ActionRequest(rule.id, decision.actionId, decision.explanation))
            } else {
                emptyList()
            }
        return HostEvaluation(decision, requests, skippedDuplicate = false)
    }
}
