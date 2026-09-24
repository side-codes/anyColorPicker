package codes.side.color.internal

import codes.side.color.CssColorParseException

/** A CSS Syntax 3 token spanning `start..<end` of its text. */
internal sealed class CssToken(val start: Int, val end: Int) {
    class Ident(start: Int, end: Int, val name: String) : CssToken(start, end)

    /** A name and its opening parenthesis. */
    class Function(start: Int, end: Int, val name: String) : CssToken(start, end)

    class Number(start: Int, end: Int, val value: Double) : CssToken(start, end)

    class Percentage(start: Int, end: Int, val value: Double) : CssToken(start, end)

    class Dimension(start: Int, end: Int, val value: Double, val unit: String) : CssToken(start, end)

    /** `#` and the name after it. */
    class Hash(start: Int, end: Int, val digits: String) : CssToken(start, end)

    /** Any other single character: `,`, `/`, `)` and whatever a color has no use for. */
    class Delim(start: Int, val char: Char) : CssToken(start, start + 1)

    class End(at: Int) : CssToken(at, at)

    fun isDelim(char: Char): Boolean = this is Delim && this.char == char
}

/**
 * The tokens of [text], cut as CSS Syntax 3 cuts them, with whitespace and comments dropped: a
 * color's grammar never needs them, since juxtaposed values are separate tokens already. Escapes
 * are refused rather than decoded.
 */
internal fun cssTokens(text: String): List<CssToken> {
    val tokens = ArrayList<CssToken>()
    var i = 0
    while (i < text.length) {
        val c = text[i]
        when {
            c.isCssWhitespace() -> i++
            text.startsWith("/*", i) -> {
                val end = text.indexOf("*/", i + 2)
                i = if (end < 0) text.length else end + 2
            }
            c == '\\' -> throw CssColorParseException("Escaped characters are not supported", i, text)
            startsNumber(text, i) -> i = numeric(text, i, tokens)
            startsIdent(text, i) -> {
                val end = nameEnd(text, i)
                val name = text.substring(i, end)
                i = if (end < text.length && text[end] == '(') {
                    tokens += CssToken.Function(i, end + 1, name)
                    end + 1
                } else {
                    tokens += CssToken.Ident(i, end, name)
                    end
                }
            }
            c == '#' && i + 1 < text.length && text[i + 1].isNameChar() -> {
                val end = nameEnd(text, i + 1)
                tokens += CssToken.Hash(i, end, text.substring(i + 1, end))
                i = end
            }
            else -> {
                tokens += CssToken.Delim(i, c)
                i++
            }
        }
    }
    tokens += CssToken.End(text.length)
    return tokens
}

/**
 * Lowercase in ASCII only, as CSS compares names: `blacK`, whose K is the Kelvin sign, is not
 * `black`, though Unicode lowercases the two alike.
 */
internal fun String.asciiLowercase(): String {
    if (none { it in 'A'..'Z' }) return this
    return buildString(length) {
        for (c in this@asciiLowercase) append(if (c in 'A'..'Z') c + ('a' - 'A') else c)
    }
}

/** A number too large for a Double becomes the largest of its sign, as CSS clamps an out-of-range value. */
internal fun finiteOrLargest(value: Double): Double = when (value) {
    Double.POSITIVE_INFINITY -> Double.MAX_VALUE
    Double.NEGATIVE_INFINITY -> -Double.MAX_VALUE
    else -> value
}

// Consumes the number at [start] and any unit or percent sign after it; returns where it ends.
private fun numeric(text: String, start: Int, tokens: MutableList<CssToken>): Int {
    var i = start
    if (text[i] == '+' || text[i] == '-') i++
    while (i < text.length && text[i].isAsciiDigit()) i++
    if (i + 1 < text.length && text[i] == '.' && text[i + 1].isAsciiDigit()) {
        i += 2
        while (i < text.length && text[i].isAsciiDigit()) i++
    }
    if (i < text.length && (text[i] == 'e' || text[i] == 'E')) {
        val sign = if (i + 1 < text.length && (text[i + 1] == '+' || text[i + 1] == '-')) 1 else 0
        if (i + 1 + sign < text.length && text[i + 1 + sign].isAsciiDigit()) {
            i += 1 + sign
            while (i < text.length && text[i].isAsciiDigit()) i++
        }
    }
    val value = finiteOrLargest(text.substring(start, i).toDouble())
    return when {
        startsIdent(text, i) -> {
            val end = nameEnd(text, i)
            tokens += CssToken.Dimension(start, end, value, text.substring(i, end))
            end
        }
        i < text.length && text[i] == '%' -> {
            tokens += CssToken.Percentage(start, i + 1, value)
            i + 1
        }
        else -> {
            tokens += CssToken.Number(start, i, value)
            i
        }
    }
}

private fun startsNumber(text: String, i: Int): Boolean {
    var j = i
    if (text[j] == '+' || text[j] == '-') j++
    if (j >= text.length) return false
    if (text[j].isAsciiDigit()) return true
    return text[j] == '.' && j + 1 < text.length && text[j + 1].isAsciiDigit()
}

private fun startsIdent(text: String, i: Int): Boolean {
    if (i >= text.length) return false
    val c = text[i]
    if (c == '-') return i + 1 < text.length && (text[i + 1] == '-' || text[i + 1].isNameStart())
    return c.isNameStart()
}

private fun nameEnd(text: String, start: Int): Int {
    var i = start
    while (i < text.length && text[i].isNameChar()) i++
    if (i < text.length && text[i] == '\\') throw CssColorParseException("Escaped characters are not supported", i, text)
    return i
}

/** True when [name] is a CSS custom name: `--` and then letters, digits, `-`, `_` or characters past ASCII. */
internal fun isDashedName(name: String): Boolean =
    name.length > 2 && name.startsWith("--") && (2 until name.length).all { name[it].isNameChar() }

private fun Char.isAsciiDigit(): Boolean = this in '0'..'9'

private fun Char.isNameStart(): Boolean = this in 'a'..'z' || this in 'A'..'Z' || this == '_' || code >= 0x80

private fun Char.isNameChar(): Boolean = isNameStart() || isAsciiDigit() || this == '-'

private fun Char.isCssWhitespace(): Boolean = this == ' ' || this == '\t' || this == '\n' || this == '\r' || this == '\u000C'
