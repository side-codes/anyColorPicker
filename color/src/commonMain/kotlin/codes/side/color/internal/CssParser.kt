package codes.side.color.internal

import codes.side.color.ColorSpace
import codes.side.color.ColorValue
import codes.side.color.CssColorParseException
import codes.side.color.DisplayP3
import codes.side.color.HexAlpha
import codes.side.color.Hsl
import codes.side.color.Hwb
import codes.side.color.Lab
import codes.side.color.Lch
import codes.side.color.OkLch
import codes.side.color.Oklab
import codes.side.color.Srgb
import codes.side.color.SrgbLinear
import codes.side.color.XyzD50
import codes.side.color.XyzD65
import kotlin.math.PI

/** Reads one CSS color from [text]; [codes.side.color.parseCss] says what it accepts. */
internal class CssColorParser(private val text: String, knownSpaces: Collection<ColorSpace>) {
    private val known = dashedSpaces(knownSpaces)
    private val tokens = cssTokens(text)
    private var position = 0

    fun parse(): ColorValue {
        val color = when (val token = next()) {
            is CssToken.Hash -> hexColor(token.digits, HexAlpha.Last) ?: fail(token, "A hex color is 3, 4, 6 or 8 hex digits")
            is CssToken.Ident -> {
                if (peek().isDelim('(')) fail(peek(), "Unexpected space before (")
                keyword(token)
            }
            is CssToken.Function -> function(token)
            else -> fail(token, "Expected a color")
        }
        val rest = next()
        if (rest !is CssToken.End) fail(rest, "Unexpected ${describe(rest)} after the color")
        return color
    }

    private fun keyword(token: CssToken.Ident): ColorValue {
        val name = token.name.asciiLowercase()
        if (name == "transparent") return Srgb(0.0, 0.0, 0.0, 0.0)
        val rgb = NAMED_COLORS[name]
        if (rgb != null) return Srgb((rgb shr 16) / 255.0, (rgb shr 8 and 0xFF) / 255.0, (rgb and 0xFF) / 255.0)
        when (name) {
            "currentcolor" -> fail(token, "currentcolor is not supported")
            in SYSTEM_COLORS -> fail(token, "System colors such as ${token.name} are not supported")
            else -> fail(token, "Unknown color name ${token.name}")
        }
    }

    private fun function(token: CssToken.Function): ColorValue {
        val name = token.name.asciiLowercase()
        val space = when (name) {
            "rgb", "rgba" -> Srgb
            "hsl", "hsla" -> Hsl
            "hwb" -> Hwb
            "lab" -> Lab
            "lch" -> Lch
            "oklab" -> Oklab
            "oklch" -> OkLch
            "color" -> null
            in UNSUPPORTED_FUNCTIONS, in MATH_FUNCTIONS -> fail(token, "$name() is not supported")
            else -> fail(token, "Unknown color function ${token.name}()")
        }
        val first = peek()
        if (first is CssToken.Ident && first.name.asciiLowercase() == "from") fail(first, "Relative colors are not supported")
        return when (space) {
            null -> colorFunction()
            Srgb, Hsl -> commasOrSpaces(space, name)
            else -> spaces(space, name, component(name, 0, 3), rgb = false, clampsLightness = space != Hwb)
        }
    }

    // rgb() and hsl(), where a comma after the first component picks the comma syntax.
    private fun commasOrSpaces(space: ColorSpace, name: String): ColorValue {
        val rgb = space == Srgb
        val first = component(name, 0, 3)
        if (!peek().isDelim(',')) return spaces(space, name, first, rgb, clampsLightness = false)
        val values = arrayListOf(first)
        repeat(2) {
            expectComma(name)
            values += component(name, values.size, 3)
        }
        var alpha: Value? = null
        if (peek().isDelim(',')) {
            next()
            alpha = component(name, 3, 3)
        }
        expectClose(name)
        for (value in values + listOfNotNull(alpha)) {
            if (value.kind == Kind.None) fail(value.token, "none is not allowed in the comma syntax")
        }
        if (rgb) {
            val mixed = values.firstOrNull { it.kind != values[0].kind }
            if (mixed != null) fail(mixed.token, "The comma syntax takes all numbers or all percentages")
        } else {
            val number = values.drop(1).firstOrNull { it.kind != Kind.Percentage }
            if (number != null) fail(number.token, "The comma syntax takes percentages for saturation and lightness")
        }
        return build(space, values, alpha, rgb, clampsLightness = false)
    }

    // The space syntax after its first component: the others, an optional `/ alpha`, and `)`.
    private fun spaces(space: ColorSpace, name: String, first: Value, rgb: Boolean, clampsLightness: Boolean): ColorValue {
        val count = space.channels.size
        val values = arrayListOf(first)
        while (values.size < count) values += component(name, values.size, count)
        var alpha: Value? = null
        if (peek().isDelim('/')) {
            next()
            alpha = component(name, count, count)
        }
        expectClose(name)
        return build(space, values, alpha, rgb, clampsLightness)
    }

    private fun colorFunction(): ColorValue {
        val token = next()
        if (token !is CssToken.Ident) fail(token, "Expected a color space after color(")
        val space = if (token.name.startsWith("--")) {
            known[token.name] ?: fail(token, "Unknown color space ${token.name}; an app's space parses when it is among knownSpaces")
        } else {
            val id = token.name.asciiLowercase()
            predefinedSpace(id) ?: when (id) {
                in UNSUPPORTED_SPACES -> fail(token, "The $id color space is not supported")
                in FUNCTION_SPACES -> fail(token, "$id is written $id(), not color($id)")
                else -> fail(token, "Unknown color space ${token.name}")
            }
        }
        val count = space.channels.size
        return spaces(space, "color", component("color", 0, count), rgb = false, clampsLightness = false)
    }

    // [rgb]: rgb()'s numbers are out of 255 and its components clamp to 0..255. [clampsLightness]:
    // lab(), lch(), oklab() and oklch() clamp lightness to its reference range.
    private fun build(space: ColorSpace, values: List<Value>, alpha: Value?, rgb: Boolean, clampsLightness: Boolean): ColorValue {
        val components = DoubleArray(values.size)
        var missing = 0
        values.forEachIndexed { i, value ->
            val channel = space.channels[i]
            var number = when (value.kind) {
                Kind.None -> {
                    missing = missing or (1 shl i)
                    return@forEachIndexed
                }
                Kind.Number -> if (rgb) value.number / 255.0 else value.number
                Kind.Percentage -> {
                    if (channel.isHue) fail(value.token, "A hue takes a number or an angle, not a percentage")
                    // Where 100% is 100, p% is p: multiplying and dividing lands an ulp off for many p.
                    val end = channel.referenceRange.endInclusive
                    if (end == 100.0) value.number else finiteOrLargest(value.number * end / 100.0)
                }
                Kind.Angle -> {
                    if (!channel.isHue) fail(value.token, "Only a hue takes an angle")
                    value.number
                }
            }
            if (rgb) number = number.coerceIn(0.0, 1.0)
            if (clampsLightness && i == 0) number = number.coerceIn(channel.referenceRange)
            channel.limit?.let { number = number.coerceIn(it) }
            components[i] = number
        }
        var alphaValue: Double? = 1.0
        when (alpha?.kind) {
            null -> {}
            Kind.None -> alphaValue = null
            Kind.Number -> alphaValue = alpha.number.coerceIn(0.0, 1.0)
            Kind.Percentage -> alphaValue = (alpha.number / 100.0).coerceIn(0.0, 1.0)
            Kind.Angle -> fail(alpha.token, "Alpha takes a number or a percentage, not an angle")
        }
        return space.color(components, alphaValue, missing)
    }

    // A number, percentage, angle or none, the [index]th of [count] components, or alpha when they are equal.
    private fun component(name: String, index: Int, count: Int): Value = when (val token = next()) {
        is CssToken.Number -> Value(token, Kind.Number, token.value)
        is CssToken.Percentage -> Value(token, Kind.Percentage, token.value)
        is CssToken.Dimension -> Value(token, Kind.Angle, finiteOrLargest(degrees(token)))
        is CssToken.Ident -> {
            if (token.name.asciiLowercase() != "none") fail(token, "Unexpected ${token.name}")
            Value(token, Kind.None, 0.0)
        }
        is CssToken.Function -> {
            val function = token.name.asciiLowercase()
            if (function in MATH_FUNCTIONS) fail(token, "$function() is not supported")
            fail(token, "Unexpected ${token.name}()")
        }
        is CssToken.End -> if (index < count) fail(token, "$name() takes $count components, found $index") else fail(token, "Expected alpha")
        is CssToken.Delim -> when (token.char) {
            ',' -> if (name in COMMA_FUNCTIONS) fail(token, "Unexpected comma") else fail(token, "$name() takes no commas")
            ')', '/' -> if (index < count) fail(token, "$name() takes $count components, found $index") else fail(token, "Expected alpha")
            else -> fail(token, "Unexpected ${token.char}")
        }
        is CssToken.Hash -> fail(token, "Unexpected ${describe(token)}")
    }

    private fun degrees(token: CssToken.Dimension): Double = when (token.unit.asciiLowercase()) {
        "deg" -> token.value
        "grad" -> token.value * 0.9
        "rad" -> token.value * 180.0 / PI
        "turn" -> token.value * 360.0
        else -> fail(token, "Unknown unit ${token.unit}")
    }

    private fun expectComma(name: String) {
        val token = next()
        if (token.isDelim(',')) return
        if (token.isDelim(')') || token is CssToken.End) fail(token, "$name() takes 3 components")
        fail(token, "Expected a comma: $name() separates its components with commas or with spaces, not both")
    }

    private fun expectClose(name: String) {
        val token = next()
        if (token.isDelim(')')) return
        if (token is CssToken.End) fail(token, "Expected ) to close $name()")
        fail(token, "Unexpected ${describe(token)} in $name()")
    }

    private fun next(): CssToken = tokens[position].also { if (it !is CssToken.End) position++ }

    private fun peek(): CssToken = tokens[position]

    private fun describe(token: CssToken): String = if (token is CssToken.End) "end of text" else text.substring(token.start, token.end)

    private fun fail(token: CssToken, reason: String): Nothing = throw CssColorParseException(reason, token.start, text)

    private enum class Kind { Number, Percentage, Angle, None }

    private class Value(val token: CssToken, val kind: Kind, val number: Double)
}

/**
 * How CSS names [space]: by its id when CSS defines the space or the id starts with `--`, and as
 * `--id` otherwise, as for the library's HSV, Okhsl, Okhsv and CMYK.
 */
internal fun cssName(space: ColorSpace): String = when {
    space.id.startsWith("--") || predefinedSpace(space.id) != null || space.id in FUNCTION_SPACES -> space.id
    else -> "--${space.id}"
}

/** The CSS spaces written with a function of their own rather than `color()`. */
internal val FUNCTION_SPACES: Set<String> = setOf("hsl", "hwb", "lab", "lch", "oklab", "oklch")

private fun predefinedSpace(id: String): ColorSpace? = when (id) {
    "srgb" -> Srgb
    "srgb-linear" -> SrgbLinear
    "display-p3" -> DisplayP3
    "xyz", "xyz-d65" -> XyzD65
    "xyz-d50" -> XyzD50
    else -> null
}

private fun dashedSpaces(spaces: Collection<ColorSpace>): Map<String, ColorSpace> {
    val byName = HashMap<String, ColorSpace>()
    for (space in spaces) {
        val name = cssName(space)
        if (!name.startsWith("--")) continue
        val existing = byName.put(name, space)
        require(existing == null || existing === space) { "Two different instances among the known spaces are both written $name" }
    }
    return byName
}

private val COMMA_FUNCTIONS = setOf("rgb", "rgba", "hsl", "hsla")

private val UNSUPPORTED_SPACES = setOf("a98-rgb", "prophoto-rgb", "rec2020", "display-p3-linear")

private val UNSUPPORTED_FUNCTIONS = setOf("color-mix", "light-dark", "contrast-color", "device-cmyk", "alpha", "ictcp", "jzazbz", "jzczhz")

private val MATH_FUNCTIONS = setOf(
    "calc", "min", "max", "clamp", "round", "mod", "rem", "sin", "cos", "tan", "asin", "acos", "atan", "atan2",
    "pow", "sqrt", "hypot", "log", "exp", "abs", "sign", "var", "env", "attr",
)

private val SYSTEM_COLORS = setOf(
    "accentcolor", "accentcolortext", "activetext", "buttonborder", "buttonface", "buttontext", "canvas",
    "canvastext", "field", "fieldtext", "graytext", "highlight", "highlighttext", "linktext", "mark",
    "marktext", "selecteditem", "selecteditemtext", "visitedtext", "activeborder", "activecaption",
    "appworkspace", "background", "buttonhighlight", "buttonshadow", "captiontext", "inactiveborder",
    "inactivecaption", "inactivecaptiontext", "infobackground", "infotext", "menu", "menutext", "scrollbar",
    "threeddarkshadow", "threedface", "threedhighlight", "threedlightshadow", "threedshadow", "window",
    "windowframe", "windowtext",
)
