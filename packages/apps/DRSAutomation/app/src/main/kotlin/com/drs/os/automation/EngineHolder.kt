/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * Automation runtime wiring (Android host side).
 *
 *  * PrefsHostStateStore — a HostStateStore whose in-memory state is
 *    loaded from and persisted to the AutomationKeyValueStore (dedup
 *    keys, per-day run counters, audit tail survive process restarts).
 *  * EngineHolder — owns the rule list: loads it once from durable
 *    storage, validates every save through the engine's own validator
 *    (a rule that fails validateRule is never persisted, never run),
 *    and flags store corruption honestly instead of salvaging.
 */
package com.drs.os.automation

import com.drs.os.automation.host.AutomationKeyValueStore
import com.drs.os.automation.host.RuleStoreCodec
import com.drs.os.engine.AuditRecord
import com.drs.os.engine.Rule
import com.drs.os.engine.host.HostStateStore
import com.drs.os.engine.host.InMemoryHostStateStore
import com.drs.os.engine.validateRule
import com.drs.os.foundation.KvStore

class PrefsHostStateStore(
    private val kv: AutomationKeyValueStore,
    private val key: String = RuleStoreCodec.STATE_KEY
) : HostStateStore {

    private val mem: InMemoryHostStateStore = load()

    private fun load(): InMemoryHostStateStore {
        val raw = kv.read(key) ?: return InMemoryHostStateStore()
        return try {
            InMemoryHostStateStore.restore(KvStore.decode(raw))
        } catch (e: IllegalArgumentException) {
            // Corrupt state snapshot: start fresh. The rules store is
            // unaffected; the loss is bounded to dedup windows and the
            // audit tail, and it is visible (fresh timestamps), not faked.
            InMemoryHostStateStore()
        }
    }

    fun persist() {
        kv.write(key, KvStore.encode(InMemoryHostStateStore.snapshot(mem)))
    }

    override fun lastEvaluationMinute(ruleId: String): Pair<Long, Int>? =
        mem.lastEvaluationMinute(ruleId)

    override fun recordEvaluation(ruleId: String, epochDay: Long, minuteOfDay: Int) =
        mem.recordEvaluation(ruleId, epochDay, minuteOfDay)

    override fun runsToday(ruleId: String, epochDay: Long): Int =
        mem.runsToday(ruleId, epochDay)

    override fun countRun(ruleId: String, epochDay: Long) = mem.countRun(ruleId, epochDay)

    override fun appendAudit(record: AuditRecord) = mem.appendAudit(record)

    override fun auditLog(): List<AuditRecord> = mem.auditLog()
}

class EngineHolder(private val kv: AutomationKeyValueStore) {

    val stateStore = PrefsHostStateStore(kv)

    val loadedCorrupted: Boolean
    private val rulesById = LinkedHashMap<String, Rule>()

    init {
        val decoded = RuleStoreCodec.decodeRules(kv.read(RuleStoreCodec.RULES_KEY))
        loadedCorrupted = decoded.corrupted
        for (rule in decoded.rules) rulesById[rule.id] = rule
    }

    fun rules(): List<Rule> = rulesById.values.sortedBy { it.id }

    /**
     * Validate-then-persist. Returns the validation errors; an empty
     * list means the rule was saved. Invalid rules are NEVER stored.
     */
    fun save(rule: Rule): List<String> {
        val errors = validateRule(rule)
        if (errors.isNotEmpty()) return errors.map { it.toString() }
        rulesById[rule.id] = rule
        persistRules()
        return emptyList()
    }

    fun delete(ruleId: String): Boolean {
        val removed = rulesById.remove(ruleId) != null
        if (removed) persistRules()
        return removed
    }

    fun setEnabled(ruleId: String, enabled: Boolean): Boolean {
        val rule = rulesById[ruleId] ?: return false
        rulesById[ruleId] = rule.copy(enabled = enabled)
        persistRules()
        return true
    }

    private fun persistRules() {
        kv.write(RuleStoreCodec.RULES_KEY, RuleStoreCodec.encodeRules(rules()))
    }
}
