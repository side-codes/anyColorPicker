package codes.side.color

import codes.side.color.internal.CssColorParser

/**
 * Text that is not a CSS color this library reads. [index] is where in the text the problem starts:
 * the offending token, or the text's length when the color ends too soon.
 */
public class CssColorParseException internal constructor(
    reason: String,
    public val index: Int,
    text: String,
) : IllegalArgumentException("$reason at index $index: ${if (text.length <= 100) text else "${text.take(100)}…"}")

/**
 * The CSS color [text], in the space it is written in: `hsl()` gives an [Hsl] color, and hex, named
 * colors and `rgb()` give [Srgb].
 *
 * It reads CSS Color 4: hex, the named colors and `transparent`, `rgb()` and `hsl()` in the comma
 * and space syntaxes, `hwb()`, `lab()`, `lch()`, `oklab()`, `oklch()`, and `color()` for srgb,
 * srgb-linear, display-p3, xyz, xyz-d65 and xyz-d50. Values clamp as CSS clamps them at parse time.
 *
 * [knownSpaces] are the spaces `color(--name …)` can name: the library's HSV, Okhsl, Okhsv and CMYK
 * as `--hsv`, `--okhsl`, `--okhsv` and `--cmyk`, and an app's own spaces by their ids. CSS's own
 * spaces are always known.
 *
 * `currentcolor`, system colors, `calc()`, relative colors, `color-mix()` and the color spaces
 * rec2020, a98-rgb, prophoto-rgb and display-p3-linear are not supported.
 *
 * @throws CssColorParseException if [text] is not a color this reads.
 * @throws IllegalArgumentException if two different [knownSpaces] would be written with one name.
 */
public fun ColorValue.Companion.parseCss(text: String, knownSpaces: Collection<ColorSpace> = ColorSpaces.all): ColorValue =
    CssColorParser(text, knownSpaces).parse()

/** As [parseCss], but null when [text] is not a color this reads. */
public fun ColorValue.Companion.parseCssOrNull(text: String, knownSpaces: Collection<ColorSpace> = ColorSpaces.all): ColorValue? =
    try {
        parseCss(text, knownSpaces)
    } catch (e: CssColorParseException) {
        null
    }

