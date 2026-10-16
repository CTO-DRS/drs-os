/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS Automation — pure rule persistence codec (JVM-testable).
 * Wire format: KvStore line codec mapping ruleId → RuleJson document.
 * Corrupted persistence yields an EMPTY rule set plus a recovery flag —
 * never a partially-salvaged (possibly tampered) rule set.
 */
package com.drs.os.automation.host

import com.drs.os.engine.Rule
import com.drs.os.engine.RuleJson
import com.drs.os.foundation.KvStore

object RuleStoreCodec {

    const val RULES_KEY = "rules_v1"
    const val STATE_KEY = "state_v1"

    data class DecodedRules(
        val rules: List<Rule>,
        val corrupted: Boolean
    )

    fun encodeRules(rules: List<Rule>): String {
        val entries = LinkedHashMap<String, String>()
        for (rule in rules.sortedBy { it.id }) {
            entries[rule.id] = RuleJson.encodeRule(rule)
        }
        return KvStore.encode(entries)
    }

    fun decodeRules(raw: String?): DecodedRules {
        if (raw.isNullOrBlank()) return DecodedRules(emptyList(), corrupted = false)
        val entries = try {
            KvStore.decode(raw)
        } catch (e: IllegalArgumentException) {
            return DecodedRules(emptyList(), corrupted = true)
        }
        val rules = ArrayList<Rule>(entries.size)
        for ((id, json) in entries) {
            val rule = try {
                RuleJson.decodeRule(json)
            } catch (e: IllegalArgumentException) {
                return DecodedRules(emptyList(), corrupted = true)
            }
            if (rule.id != id) {
                return DecodedRules(emptyList(), corrupted = true)
            }
            rules.add(rule)
        }
        return DecodedRules(rules.sortedBy { it.id }, corrupted = false)
    }
}

/**
 * Storage abstraction. The Android host binds it to SharedPreferences;
 * tests bind it to memory. Values are opaque strings owned by the
 * codecs.
 */
interface AutomationKeyValueStore {
    fun read(key: String): String?
    fun write(key: String, value: String): Boolean
}
