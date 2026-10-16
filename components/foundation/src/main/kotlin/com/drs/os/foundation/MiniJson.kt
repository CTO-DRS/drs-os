/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS OS foundation — minimal strict JSON reader/writer.
 *
 * Scope (deliberate): a small, auditable parser covering the JSON the
 * project actually needs (config documents, rule definitions, test
 * vectors). Strictness rules:
 *  * RFC 8259 syntax only: no comments, no trailing commas, no
 *    single-quoted strings, no NaN/Infinity;
 *  * duplicate object keys are rejected (duplication is always a data
 *    error, never "last one wins");
 *  * unpaired surrogate escapes are rejected;
 *  * raw control characters inside strings are rejected;
 *  * nesting depth is bounded (default 64) to make hostile inputs
 *    fail fast instead of exhausting the stack;
 *  * numbers are preserved as their raw lexeme and exposed as Long
 *    only when they are integral — no silent float conversion.
 *
 * The writer emits a canonical form: no whitespace, object keys in
 * insertion order, shortest escapes. parse(write(v)) == v round-trips.
 */
package com.drs.os.foundation

object MiniJson {

    sealed interface JsonValue {
        data class JObj(val entries: LinkedHashMap<String, JsonValue>) : JsonValue
        data class JArr(val items: List<JsonValue>) : JsonValue
        data class JStr(val value: String) : JsonValue
        data class JNum(val raw: String) : JsonValue {
            fun toLongOrNull(): Long? = if (raw.matches(INTEGER_RE)) raw.toLong() else null
        }
        data class JBool(val value: Boolean) : JsonValue
        object JNull : JsonValue
    }

    private val INTEGER_RE = Regex("""^-?(0|[1-9]\d*)$""")
    private const val MAX_DEPTH = 64

    class JsonException(message: String, val position: Int) :
        IllegalArgumentException("$message (at index $position)")

    // ---------------------------------------------------------------- parse

    fun parse(text: String): JsonValue {
        val p = Parser(text)
        p.skipWs()
        val v = p.parseValue(0)
        p.skipWs()
        if (!p.atEnd()) {
            throw p.fail("trailing content after top-level value")
        }
        return v
    }

    private class Parser(val text: String) {
        var pos = 0

        fun atEnd() = pos >= text.length
        fun fail(msg: String): JsonException = JsonException(msg, pos)

        fun skipWs() {
            while (pos < text.length && text[pos].let { it == ' ' || it == '\t' || it == '\n' || it == '\r' }) {
                pos++
            }
        }

        fun peek(): Char {
            if (atEnd()) throw fail("unexpected end of input")
            return text[pos]
        }

        fun expect(c: Char) {
            if (atEnd() || text[pos] != c) throw fail("expected '$c'")
            pos++
        }

        fun parseValue(depth: Int): JsonValue {
            return when (peek()) {
                '{' -> parseObject(depth + 1)
                '[' -> parseArray(depth + 1)
                '"' -> JsonValue.JStr(parseString())
                't' -> parseLiteral("true", JsonValue.JBool(true))
                'f' -> parseLiteral("false", JsonValue.JBool(false))
                'n' -> parseLiteral("null", JsonValue.JNull)
                else -> parseNumber()
            }
        }

        fun parseLiteral(word: String, value: JsonValue): JsonValue {
            if (pos + word.length > text.length ||
                text.substring(pos, pos + word.length) != word) {
                throw fail("invalid literal")
            }
            pos += word.length
            return value
        }

        fun parseObject(depth: Int): JsonValue {
            if (depth > MAX_DEPTH) throw fail("maximum nesting depth $MAX_DEPTH exceeded")
            expect('{')
            val entries = LinkedHashMap<String, JsonValue>()
            skipWs()
            if (peek() == '}') { pos++; return JsonValue.JObj(entries) }
            while (true) {
                skipWs()
                if (peek() != '"') throw fail("expected string key")
                val key = parseString()
                skipWs()
                expect(':')
                skipWs()
                val value = parseValue(depth)
                if (entries.containsKey(key)) {
                    throw fail("duplicate object key '$key'")
                }
                entries[key] = value
                skipWs()
                when (peek()) {
                    ',' -> { pos++; }
                    '}' -> { pos++; return JsonValue.JObj(entries) }
                    else -> throw fail("expected ',' or '}'")
                }
            }
        }

        fun parseArray(depth: Int): JsonValue {
            if (depth > MAX_DEPTH) throw fail("maximum nesting depth $MAX_DEPTH exceeded")
            expect('[')
            val items = ArrayList<JsonValue>()
            skipWs()
            if (peek() == ']') { pos++; return JsonValue.JArr(items) }
            while (true) {
                skipWs()
                items.add(parseValue(depth))
                skipWs()
                when (peek()) {
                    ',' -> { pos++; }
                    ']' -> { pos++; return JsonValue.JArr(items) }
                    else -> throw fail("expected ',' or ']'")
                }
            }
        }

        fun parseString(): String {
            expect('"')
            val sb = StringBuilder()
            while (true) {
                if (atEnd()) throw fail("unterminated string")
                val c = text[pos]
                when {
                    c == '"' -> { pos++; return sb.toString() }
                    c == '\\' -> {
                        pos++
                        if (atEnd()) throw fail("unterminated escape")
                        when (val e = text[pos]) {
                            '"' -> sb.append('"')
                            '\\' -> sb.append('\\')
                            '/' -> sb.append('/')
                            'b' -> sb.append('\b')
                            'f' -> sb.append('\u000C')
                            'n' -> sb.append('\n')
                            'r' -> sb.append('\r')
                            't' -> sb.append('\t')
                            'u' -> {
                                pos++
                                sb.append(parseUnicodeEscape())
                                continue  // pos already advanced past 4 hex digits
                            }
                            else -> throw fail("invalid escape '\\$e'")
                        }
                        pos++
                    }
                    c.code < 0x20 -> throw fail("raw control character in string")
                    else -> { sb.append(c); pos++ }
                }
            }
        }

        /**
         * Parses the 4 hex digits after a `\u`. Handles surrogate pairs:
         * a high surrogate must be immediately followed by `\uDC00-\uDFFF`.
         */
        fun parseUnicodeEscape(): String {
            if (pos + 4 > text.length) throw fail("truncated unicode escape")
            val hex = text.substring(pos, pos + 4)
            val code = hex.toIntOrNull(16)
                ?: throw fail("invalid unicode escape '\\u$hex'")
            pos += 4
            val ch = code.toChar()
            if (ch.isHighSurrogate()) {
                if (pos + 6 > text.length || text[pos] != '\\' || text[pos + 1] != 'u') {
                    throw fail("unpaired high surrogate in escape")
                }
                val hex2 = text.substring(pos + 2, pos + 6)
                val low = hex2.toIntOrNull(16)
                    ?: throw fail("invalid low surrogate escape '\\u$hex2'")
                val lowCh = low.toChar()
                if (!lowCh.isLowSurrogate()) throw fail("unpaired high surrogate in escape")
                pos += 6
                return String(charArrayOf(ch, lowCh))
            }
            if (ch.isLowSurrogate()) throw fail("unpaired low surrogate in escape")
            return ch.toString()
        }

        fun parseNumber(): JsonValue {
            val start = pos
            if (atEnd()) throw fail("expected value")
            if (peek() == '-') pos++
            if (atEnd() || !text[pos].isDigit()) throw fail("invalid number")
            if (text[pos] == '0') {
                pos++
            } else {
                while (pos < text.length && text[pos].isDigit()) pos++
            }
            // fraction / exponent are accepted but preserved raw; DRS data
            // models use integers, so consumers opt in via toLongOrNull.
            if (pos < text.length && text[pos] == '.') {
                pos++
                if (atEnd() || !text[pos].isDigit()) throw fail("invalid fraction")
                while (pos < text.length && text[pos].isDigit()) pos++
            }
            if (pos < text.length && (text[pos] == 'e' || text[pos] == 'E')) {
                pos++
                if (pos < text.length && (text[pos] == '+' || text[pos] == '-')) pos++
                if (atEnd() || !text[pos].isDigit()) throw fail("invalid exponent")
                while (pos < text.length && text[pos].isDigit()) pos++
            }
            return JsonValue.JNum(text.substring(start, pos))
        }
    }

    // ---------------------------------------------------------------- write

    fun write(value: JsonValue): String {
        val sb = StringBuilder()
        writeValue(value, sb)
        return sb.toString()
    }

    fun writeString(s: String): String {
        val sb = StringBuilder()
        writeEscaped(s, sb)
        return sb.toString()
    }

    private fun writeValue(value: JsonValue, sb: StringBuilder) {
        when (value) {
            is JsonValue.JObj -> {
                sb.append('{')
                var first = true
                for ((k, v) in value.entries) {
                    if (!first) sb.append(',')
                    first = false
                    writeEscaped(k, sb)
                    sb.append(':')
                    writeValue(v, sb)
                }
                sb.append('}')
            }
            is JsonValue.JArr -> {
                sb.append('[')
                var first = true
                for (v in value.items) {
                    if (!first) sb.append(',')
                    first = false
                    writeValue(v, sb)
                }
                sb.append(']')
            }
            is JsonValue.JStr -> writeEscaped(value.value, sb)
            is JsonValue.JNum -> {
                if (!value.raw.matches(INTEGER_RE) &&
                    !value.raw.matches(Regex("""^-?(0|[1-9]\d*)(\.\d+)?([eE][+-]?\d+)?$"""))) {
                    throw IllegalArgumentException("unserializable number lexeme '${value.raw}'")
                }
                sb.append(value.raw)
            }
            is JsonValue.JBool -> sb.append(if (value.value) "true" else "false")
            JsonValue.JNull -> sb.append("null")
        }
    }

    private fun writeEscaped(s: String, sb: StringBuilder) {
        sb.append('"')
        for (c in s) {
            when {
                c == '"' -> sb.append("\\\"")
                c == '\\' -> sb.append("\\\\")
                c == '\n' -> sb.append("\\n")
                c == '\r' -> sb.append("\\r")
                c == '\t' -> sb.append("\\t")
                c == '\b' -> sb.append("\\b")
                c == '\u000C' -> sb.append("\\f")
                c.code < 0x20 -> sb.append("\\u%04x".format(c.code))
                else -> sb.append(c)
            }
        }
        sb.append('"')
    }
}
