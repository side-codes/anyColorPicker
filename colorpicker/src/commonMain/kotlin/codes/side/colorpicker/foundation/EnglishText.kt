package codes.side.colorpicker.foundation

import codes.side.color.Cmyk
import codes.side.color.ColorChannel
import codes.side.color.ColorSpace
import codes.side.color.DisplayP3
import codes.side.color.Hsl
import codes.side.color.Hsv
import codes.side.color.Hwb
import codes.side.color.Lab
import codes.side.color.Lch
import codes.side.color.OkLch
import codes.side.color.Okhsl
import codes.side.color.Okhsv
import codes.side.color.Oklab
import codes.side.color.Srgb
import codes.side.color.SrgbLinear
import codes.side.color.XyzD50
import codes.side.color.XyzD65
import codes.side.colorpicker.ui.PlaneActionLabels
import kotlin.math.ceil
import kotlin.math.log10

/**
 * The library's own words, in English, with numbers in whatever locale the given [NumberFormatter] was made for. The
 * bodies of [ColorPickerStrings] call these; they are plain functions so every target's tests can check them.
 */
internal object EnglishText {

    private enum class Format { Bytes, Whole, Thousandths, Degrees, Percent, PercentOfOne }

    // A library channel's words: the label above its slider, what a screen reader calls it, how it reads inside a
    // sentence ("Saturation and lightness", "40% saturation"), and how its value reads.
    private class Entry(val name: String, val spoken: String, val midSentence: String, val format: Format)

    // A channel named by a word, lower-cased inside a sentence.
    private fun word(name: String, format: Format) = Entry(name, name, name.lowercase(), format)

    // A channel named by a symbol, which keeps its case inside a sentence and may be spoken differently.
    private fun symbol(name: String, format: Format, spoken: String = name) = Entry(name, spoken, spoken, format)

    private fun entryOf(channel: ColorChannel): Entry? = when (channel) {
        Srgb.R, DisplayP3.R -> word("Red", Format.Bytes)
        Srgb.G, DisplayP3.G -> word("Green", Format.Bytes)
        Srgb.B, DisplayP3.B -> word("Blue", Format.Bytes)
        SrgbLinear.R -> word("Red", Format.Thousandths)
        SrgbLinear.G -> word("Green", Format.Thousandths)
        SrgbLinear.B -> word("Blue", Format.Thousandths)
        XyzD65.X, XyzD50.X -> symbol("X", Format.Thousandths)
        XyzD65.Y, XyzD50.Y -> symbol("Y", Format.Thousandths)
        XyzD65.Z, XyzD50.Z -> symbol("Z", Format.Thousandths)
        Lab.L -> symbol("L", Format.Whole, spoken = "L*")
        Lab.A -> symbol("a", Format.Whole, spoken = "a*")
        Lab.B -> symbol("b", Format.Whole, spoken = "b*")
        Lch.L -> word("Lightness", Format.Whole)
        Lch.C -> word("Chroma", Format.Whole)
        Oklab.L, OkLch.L -> word("Lightness", Format.PercentOfOne)
        Oklab.A -> symbol("a", Format.Thousandths)
        Oklab.B -> symbol("b", Format.Thousandths)
        OkLch.C -> word("Chroma", Format.Thousandths)
        Lch.H, OkLch.H, Hsl.H, Hsv.H, Hwb.H, Okhsl.H, Okhsv.H -> word("Hue", Format.Degrees)
        Hsl.S, Hsv.S -> word("Saturation", Format.Percent)
        Hsl.L -> word("Lightness", Format.Percent)
        Hsv.V -> word("Value", Format.Percent)
        Hwb.W -> word("Whiteness", Format.Percent)
        Hwb.B -> word("Blackness", Format.Percent)
        Okhsl.S, Okhsv.S -> word("Saturation", Format.PercentOfOne)
        Okhsl.L -> word("Lightness", Format.PercentOfOne)
        Okhsv.V -> word("Value", Format.PercentOfOne)
        Cmyk.C -> word("Cyan", Format.PercentOfOne)
        Cmyk.M -> word("Magenta", Format.PercentOfOne)
        Cmyk.Y -> word("Yellow", Format.PercentOfOne)
        Cmyk.K -> word("Key", Format.PercentOfOne)
        else -> null
    }

    /** Whether [channel] is one of the library's, with words of its own rather than its id. */
    fun hasEntry(channel: ColorChannel): Boolean = entryOf(channel) != null

    fun channelName(channel: ColorChannel): String = entryOf(channel)?.name ?: channel.id

    fun channelSpokenName(channel: ColorChannel): String = entryOf(channel)?.spoken ?: channel.id

    private fun midSentence(channel: ColorChannel): String = entryOf(channel)?.midSentence ?: channel.id

    fun channelValue(channel: ColorChannel, value: Double, numbers: NumberFormatter): String {
        val entry = entryOf(channel) ?: return appChannelValue(channel, value, numbers)
        return when (entry.format) {
            Format.Bytes -> numbers.integer(roundHalfAwayFromZero(value * 255.0))
            Format.Whole -> numbers.integer(roundHalfAwayFromZero(value))
            Format.Thousandths -> numbers.decimal(roundHalfAwayFromZero(value, 3), 3)
            Format.Degrees -> degrees(numbers.integer(roundHalfAwayFromZero(value)))
            Format.Percent -> numbers.percent(roundHalfAwayFromZero(value))
            Format.PercentOfOne -> numbers.percent(roundHalfAwayFromZero(value * 100.0))
        }
    }

    // An app's channel shows the decimals its step needs, and a hue its degree sign.
    private fun appChannelValue(channel: ColorChannel, value: Double, numbers: NumberFormatter): String {
        val places = decimalsFor(channel.step)
        val number = if (places == 0) {
            numbers.integer(roundHalfAwayFromZero(value))
        } else {
            numbers.decimal(roundHalfAwayFromZero(value, places), places)
        }
        return if (channel.isHue) degrees(number) else number
    }

    private fun degrees(number: String): String = "$number°"

    fun alphaName(): String = "Alpha"

    /** Alpha as its slider shows it, 0 to 255. */
    fun alphaValue(alpha: Double, numbers: NumberFormatter): String = numbers.integer(roundHalfAwayFromZero(alpha * 255.0))

    /**
     * Where the thumb of a slider with no channel sits, in percent of its track. A value past an end reads as that end
     * and NaN as the start, where the thumb is drawn; coerceIn alone would pass NaN through to the rounding, which
     * throws.
     */
    fun sliderPosition(fraction: Float, numbers: NumberFormatter): String {
        val onTrack = if (fraction.isNaN()) 0f else fraction.coerceIn(0f, 1f)
        return numbers.percent(roundHalfAwayFromZero(onTrack * 100.0))
    }

    fun spaceName(space: ColorSpace): String = when (space) {
        Srgb -> "RGB"
        SrgbLinear -> "Linear RGB"
        DisplayP3 -> "Display P3"
        XyzD65 -> "XYZ D65"
        XyzD50 -> "XYZ D50"
        Lab -> "Lab"
        Lch -> "LCH"
        Oklab -> "Oklab"
        OkLch -> "OkLCh"
        Hsl -> "HSL"
        Hwb -> "HWB"
        Hsv -> "HSV"
        Okhsl -> "Okhsl"
        Okhsv -> "Okhsv"
        Cmyk -> "CMYK"
        else -> space.id
    }

    /** "Saturation and lightness". */
    fun planeDescription(x: ColorChannel, y: ColorChannel): String = "${channelSpokenName(x)} and ${midSentence(y)}"

    /** "40% saturation, 60% lightness". */
    fun planeValue(x: ColorChannel, xValue: Double, y: ColorChannel, yValue: Double, numbers: NumberFormatter): String =
        "${channelValue(x, xValue, numbers)} ${midSentence(x)}, ${channelValue(y, yValue, numbers)} ${midSentence(y)}"

    fun planeActions(x: ColorChannel, y: ColorChannel): PlaneActionLabels = PlaneActionLabels(
        increaseX = "Increase ${midSentence(x)}",
        decreaseX = "Decrease ${midSentence(x)}",
        increaseY = "Increase ${midSentence(y)}",
        decreaseY = "Decrease ${midSentence(y)}",
    )

    fun planeAxisActions(): PlaneActionLabels = PlaneActionLabels(
        increaseX = "Increase horizontally",
        decreaseX = "Decrease horizontally",
        increaseY = "Increase vertically",
        decreaseY = "Decrease vertically",
    )

    fun dialogTitle(): String = "Select color"

    fun confirm(): String = "OK"

    fun dismiss(): String = "Cancel"

    fun originalColor(): String = "Original color"

    fun newColor(): String = "New color"

    fun restoreOriginal(): String = "Restore original color"

    // The decimals that show a change of [step]: 0 for 1 or more, 2 for 0.01, 3 for 1/255. The allowance
    // keeps a power of ten whose logarithm lands a hair past an integer from taking one decimal too many.
    private fun decimalsFor(step: Double): Int = ceil(-log10(step) - 1e-9).toInt().coerceAtLeast(0)
}
