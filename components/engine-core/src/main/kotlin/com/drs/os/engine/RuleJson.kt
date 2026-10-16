/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS OS deterministic automation — wire-format codec for rules and
 * contexts. The wire format is exactly the JSON shape used by the
 * Python reference model and the committed differential test vectors,
 * so one parser serves: persistence, import/export, and the
 * differential test that keeps Kotlin and Python semantics identical.
 *
 * Strictness: unknown fields are rejected (data models evolve via the
 * schema version, not by silently ignoring new fields), wrong types are
 * rejected with the field path, duplicate keys are rejected by the
 * underlying MiniJson parser.
 */
package com.drs.os.engine

import com.drs.os.foundation.MiniJson
import com.drs.os.foundation.MiniJson.JsonValue

class RuleJsonException(val field: String, message: String) :
    IllegalArgumentException("$field: $message")

object RuleJson {

    // ------------------------------------------------------------ helpers

    private fun obj(v: JsonValue): JsonValue.JObj =
        v as? JsonValue.JObj ?: throw RuleJsonException("", "expected object")

    private fun str(o: JsonValue.JObj, key: String): String =
        (o.entries[key] as? JsonValue.JStr)?.value
            ?: throw RuleJsonException(key, "expected string")

    private fun strOrNull(o: JsonValue.JObj, key: String): String? =
        when (val v = o.entries[key]) {
            null -> null
            JsonValue.JNull -> null
            is JsonValue.JStr -> v.value
            else -> throw RuleJsonException(key, "expected string or null")
        }

    private fun bool(o: JsonValue.JObj, key: String): Boolean =
        (o.entries[key] as? JsonValue.JBool)?.value
            ?: throw RuleJsonException(key, "expected boolean")

    private fun boolOr(o: JsonValue.JObj, key: String, default: Boolean): Boolean =
        if (o.entries.containsKey(key)) bool(o, key) else default

    private fun int(o: JsonValue.JObj, key: String): Int {
        val n = o.entries[key] as? JsonValue.JNum
            ?: throw RuleJsonException(key, "expected number")
        return n.toLongOrNull()?.let { if (it in Int.MIN_VALUE..Int.MAX_VALUE) it.toInt() else null }
            ?: throw RuleJsonException(key, "expected integer")
    }

    private fun intOr(o: JsonValue.JObj, key: String, default: Int): Int =
        if (o.entries.containsKey(key)) int(o, key) else default

    private fun stringList(o: JsonValue.JObj, key: String): List<String> {
        val arr = o.entries[key] as? JsonValue.JArr
            ?: throw RuleJsonException(key, "expected array")
        return arr.items.map {
            (it as? JsonValue.JStr)?.value
                ?: throw RuleJsonException(key, "expected array of strings")
        }
    }

    private fun requireFields(o: JsonValue.JObj, allowed: Set<String>) {
        for (k in o.entries.keys) {
            if (k !in allowed) {
                throw RuleJsonException(k, "unknown field")
            }
        }
    }

    // ------------------------------------------------------------ triggers

    private const val TRIGGER_FIELDS = "kind,time,threshold,state"

    private fun parseTrigger(v: JsonValue): Trigger {
        val o = obj(v)
        requireFields(o, TRIGGER_FIELDS.split(',').toSet())
        return when (val kind = str(o, "kind")) {
            "TIME_DAILY" -> Trigger.TimeDaily(str(o, "time"))
            "BATTERY_BELOW" -> Trigger.BatteryBelow(int(o, "threshold"))
            "POWER_CONNECTED" -> Trigger.PowerConnected(bool(o, "state"))
            else -> throw RuleJsonException("trigger.kind", "unknown kind '$kind'")
        }
    }

    private fun writeTrigger(t: Trigger): JsonValue.JObj {
        val entries = LinkedHashMap<String, JsonValue>()
        when (t) {
            is Trigger.TimeDaily -> {
                entries["kind"] = JsonValue.JStr("TIME_DAILY")
                entries["time"] = JsonValue.JStr(t.time)
            }
            is Trigger.BatteryBelow -> {
                entries["kind"] = JsonValue.JStr("BATTERY_BELOW")
                entries["threshold"] = JsonValue.JNum(t.threshold.toString())
            }
            is Trigger.PowerConnected -> {
                entries["kind"] = JsonValue.JStr("POWER_CONNECTED")
                entries["state"] = JsonValue.JBool(t.state)
            }
        }
        return JsonValue.JObj(entries)
    }

    // ---------------------------------------------------------- conditions

    private const val CONDITION_FIELDS = "kind,start,end,days"

    private fun parseCondition(v: JsonValue): Condition {
        val o = obj(v)
        requireFields(o, CONDITION_FIELDS.split(',').toSet())
        return when (val kind = str(o, "kind")) {
            "TIME_WINDOW" -> Condition.TimeWindow(str(o, "start"), str(o, "end"))
            "DAY_OF_WEEK" -> {
                val arr = o.entries["days"] as? JsonValue.JArr
                    ?: throw RuleJsonException("days", "expected array")
                Condition.DayOfWeek(
                    arr.items.map { v ->
                        (v as? JsonValue.JNum)?.toLongOrNull()
                            ?.let { if (it in Int.MIN_VALUE..Int.MAX_VALUE) it.toInt() else null }
                            ?: throw RuleJsonException("days", "expected integer")
                    }.toSet())
            }
            else -> throw RuleJsonException("condition.kind", "unknown kind '$kind'")
        }
    }

    private fun writeCondition(c: Condition): JsonValue.JObj {
        val entries = LinkedHashMap<String, JsonValue>()
        when (c) {
            is Condition.TimeWindow -> {
                entries["kind"] = JsonValue.JStr("TIME_WINDOW")
                entries["start"] = JsonValue.JStr(c.start)
                entries["end"] = JsonValue.JStr(c.end)
            }
            is Condition.DayOfWeek -> {
                entries["kind"] = JsonValue.JStr("DAY_OF_WEEK")
                entries["days"] = JsonValue.JArr(
                    c.days.sorted().map { JsonValue.JNum(it.toString()) })
            }
        }
        return JsonValue.JObj(entries)
    }

    // --------------------------------------------------------------- rule

    private const val RULE_FIELDS =
        "id,name,enabled,trigger,conditions,action_id,requires_unlock," +
            "max_runs_per_day,explanation,user_consented_actions"

    fun parseRule(v: JsonValue): Rule {
        val o = obj(v)
        requireFields(o, RULE_FIELDS.split(',').toSet())
        val conditionsArr = o.entries["conditions"]?.let {
            (it as? JsonValue.JArr ?: throw RuleJsonException("conditions", "expected array"))
        }
        return Rule(
            id = str(o, "id"),
            name = str(o, "name"),
            enabled = bool(o, "enabled"),
            trigger = parseTrigger(
                o.entries["trigger"]
                    ?: throw RuleJsonException("trigger", "required")),
            conditions = conditionsArr?.items?.map { parseCondition(it) } ?: emptyList(),
            actionId = str(o, "action_id"),
            requiresUnlock = boolOr(o, "requires_unlock", true),
            maxRunsPerDay = intOr(o, "max_runs_per_day", 10),
            explanation = str(o, "explanation"),
            userConsentedActions = if (o.entries.containsKey("user_consented_actions"))
                stringList(o, "user_consented_actions") else emptyList()
        )
    }

    fun writeRule(rule: Rule): JsonValue.JObj {
        val entries = LinkedHashMap<String, JsonValue>()
        entries["id"] = JsonValue.JStr(rule.id)
        entries["name"] = JsonValue.JStr(rule.name)
        entries["enabled"] = JsonValue.JBool(rule.enabled)
        entries["trigger"] = writeTrigger(rule.trigger)
        entries["conditions"] = JsonValue.JArr(rule.conditions.map { writeCondition(it) })
        entries["action_id"] = JsonValue.JStr(rule.actionId)
        entries["requires_unlock"] = JsonValue.JBool(rule.requiresUnlock)
        entries["max_runs_per_day"] = JsonValue.JNum(rule.maxRunsPerDay.toString())
        entries["explanation"] = JsonValue.JStr(rule.explanation)
        entries["user_consented_actions"] =
            JsonValue.JArr(rule.userConsentedActions.map { JsonValue.JStr(it) })
        return JsonValue.JObj(entries)
    }

    fun encodeRule(rule: Rule): String = MiniJson.write(writeRule(rule))

    fun decodeRule(json: String): Rule = parseRule(MiniJson.parse(json))

    // ------------------------------------------------------------ context

    private const val CONTEXT_FIELDS =
        "now_minutes,weekday,battery,charging,device_unlocked,runs_today"

    fun parseContext(v: JsonValue): EventContext {
        val o = obj(v)
        requireFields(o, CONTEXT_FIELDS.split(',').toSet())
        return EventContext(
            nowMinutes = int(o, "now_minutes"),
            weekday = int(o, "weekday"),
            battery = int(o, "battery"),
            charging = bool(o, "charging"),
            deviceUnlocked = bool(o, "device_unlocked"),
            runsToday = int(o, "runs_today")
        )
    }

    fun writeContext(ctx: EventContext): JsonValue.JObj {
        val entries = LinkedHashMap<String, JsonValue>()
        entries["now_minutes"] = JsonValue.JNum(ctx.nowMinutes.toString())
        entries["weekday"] = JsonValue.JNum(ctx.weekday.toString())
        entries["battery"] = JsonValue.JNum(ctx.battery.toString())
        entries["charging"] = JsonValue.JBool(ctx.charging)
        entries["device_unlocked"] = JsonValue.JBool(ctx.deviceUnlocked)
        entries["runs_today"] = JsonValue.JNum(ctx.runsToday.toString())
        return JsonValue.JObj(entries)
    }
}
