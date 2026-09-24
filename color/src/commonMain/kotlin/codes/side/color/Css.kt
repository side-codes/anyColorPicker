package codes.side.color

import codes.side.color.internal.CssColorParser
import codes.side.color.internal.cssString

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

/**
 * This color as CSS, in its own space and never mapped into a gamut, so extended values survive:
 * sRGB, display-p3 and XYZ as `color(srgb …)`, `color(display-p3 …)` and `color(xyz-d65 …)`; HSL,
 * HWB, Lab, LCH, Oklab and OkLCh as their own functions; the library's HSV, Okhsl, Okhsv and CMYK,
 * and an app's spaces, as `color(--name …)`, which [parseCss] reads back.
 *
 * A missing component is written `none`. Alpha is left out when it is 1.
 *
 * Numbers are rounded to [precision] digits, counted from the first digit before the decimal point,
 * or from the point when there is none, as color.js does: 53.2408 → 53.241, 0.123456 → 0.12346, and
 * a float residue such as 3e-17 → 0.
 *
 * [legacy] writes sRGB as `rgb()` or `rgba()` with components out of 255, and HSL as `hsl()` or
 * `hsla()`, in the comma syntax older readers want. It has no `none`, so a missing component or
 * alpha is written 0, and a reader clamps sRGB components to `0..255`. Other spaces have no comma
 * syntax and are written as usual.
 */
public fun ColorValue.toCssString(precision: Int = 5, legacy: Boolean = false): String {
    require(precision in 1..17) { "Precision is 1 to 17 digits, was $precision" }
    return cssString(this, precision, legacy)
}
