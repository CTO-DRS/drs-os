/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * DRS OS foundation — line-based key/value persistence codec.
 *
 * Design rules:
 *  * Fully deterministic: encode() sorts keys, so the same map always
 *    produces byte-identical output.
 *  * Strict decode: any malformed line is an error — the caller decides
 *    the recovery policy (e.g. settings-core falls back to defaults).
 *    Silence-accepting parsers hide corruption; this one refuses to.
 *  * Round-trip safe for arbitrary UTF-8 text (Arabic, newlines, '=',
 *    '%') via percent-encoding.
 *  * No dependencies beyond the Kotlin standard library.
 */
package com.drs.os.foundation

object KvStore {

    private val UNRESERVED = setOf(
        'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M',
        'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z',
        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm',
        'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z',
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        '-', '.', '_', '~'
    )

    fun percentEncode(text: String): String {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val sb = StringBuilder(bytes.size)
        for (b in bytes) {
            val c = (b.toInt() and 0xFF).toChar()
            if (c in UNRESERVED) {
                sb.append(c)
            } else {
                sb.append('%')
                val hex = Integer.toHexString(b.toInt() and 0xFF).uppercase()
                if (hex.length == 1) sb.append('0')
                sb.append(hex)
            }
        }
        return sb.toString()
    }

    fun percentDecode(text: String): String {
        val out = java.io.ByteArrayOutputStream()
        var i = 0
        while (i < text.length) {
            val c = text[i]
            if (c == '%') {
                if (i + 3 > text.length) {
                    throw IllegalArgumentException(
                        "percent-decode: truncated escape sequence at index $i")
                }
                val hex = text.substring(i + 1, i + 3)
                val byte = hex.toIntOrNull(16)
                    ?: throw IllegalArgumentException(
                        "percent-decode: invalid escape '%$hex'")
                out.write(byte)
                i += 3
            } else {
                // Non-ASCII input characters are not allowed: the encoder
                // emits pure ASCII, so this indicates non-encoded content.
                if (c.code > 0x7E || c.code < 0x21) {
                    throw IllegalArgumentException(
                        "percent-decode: raw character U+%04X at index %d"
                            .format(c.code, i))
                }
                out.write(c.code)
                i += 1
            }
        }
        return out.toString(Charsets.UTF_8)
    }

    /**
     * Encode a flat map to the canonical line format:
     * `encodedKey=encodedValue` per line, keys sorted lexicographically
     * (by encoded form). The result ends with a trailing newline unless
     * the map is empty.
     */
    fun encode(entries: Map<String, String>): String {
        val sorted = entries.toSortedMap(compareBy { it })
        val sb = StringBuilder()
        for ((k, v) in sorted) {
            sb.append(percentEncode(k))
            sb.append('=')
            sb.append(percentEncode(v))
            sb.append('\n')
        }
        return sb.toString()
    }

    /**
     * Decode the line format. Strict rules:
     *  * empty input (or only a final newline) yields an empty map;
     *  * every other line MUST contain exactly one '=' separator
     *    (the FIRST '=' splits key from value; further '=' inside the
     *    value are fine because the encoded form never contains a raw
     *    '=' — a raw one in a decoded position is an error caught by
     *    percent-decode);
     *  * duplicate keys are rejected;
     *  * blank lines other than the trailing one are rejected.
     *
     * @throws IllegalArgumentException on any structural violation.
     */
    fun decode(content: String): Map<String, String> {
        val result = LinkedHashMap<String, String>()
        if (content.isEmpty()) return result
        // A single trailing newline is the canonical terminator of an
        // otherwise empty document; treat it as empty content. Anything
        // beyond that is parsed strictly.
        val body = if (content.endsWith('\n'))
            content.substring(0, content.length - 1) else content
        if (body.isEmpty()) return result
        val lines = body.split('\n')
        for ((index, line) in lines.withIndex()) {
            if (line.isEmpty()) {
                throw IllegalArgumentException(
                    "decode: blank line at line ${index + 1}")
            }
            val sep = line.indexOf('=')
            if (sep < 0) {
                throw IllegalArgumentException(
                    "decode: missing '=' separator at line ${index + 1}")
            }
            val key = percentDecode(line.substring(0, sep))
            val value = percentDecode(line.substring(sep + 1))
            if (result.containsKey(key)) {
                throw IllegalArgumentException(
                    "decode: duplicate key '$key' at line ${index + 1}")
            }
            result[key] = value
        }
        return result
    }
}
