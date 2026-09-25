package codes.side.colorpicker.ui

import codes.side.color.Cmyk
import codes.side.color.ColorChannel
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
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.log10
import kotlin.math.roundToInt
import kotlin.math.roundToLong

// The components' default text, in English. Every entry is a default that a parameter overrides.

/**
 * A library channel's text: the [label] above its slider, what a screen reader calls it ([spoken]),
 * and its value as both show it ([format]).
 */
internal class ChannelText(
    val label: String,
    val spoken: String,
    val format: (Double) -> String,
)

private fun text(label: String, format: (Double) -> String, spoken: String = label) = ChannelText(label, spoken, format)

/** A library channel's text, or null for a channel of an app's space. */
internal fun libraryText(channel: ColorChannel): ChannelText? = when (channel) {
    Srgb.R, DisplayP3.R -> text("Red", ::bytes)
    Srgb.G, DisplayP3.G -> text("Green", ::bytes)
    Srgb.B, DisplayP3.B -> text("Blue", ::bytes)
    SrgbLinear.R -> text("Red", ::thousandths)
    SrgbLinear.G -> text("Green", ::thousandths)
    SrgbLinear.B -> text("Blue", ::thousandths)
    XyzD65.X, XyzD50.X -> text("X", ::thousandths)
    XyzD65.Y, XyzD50.Y -> text("Y", ::thousandths)
    XyzD65.Z, XyzD50.Z -> text("Z", ::thousandths)
    Lab.L -> text("L", ::whole, spoken = "L*")
    Lab.A -> text("a", ::whole, spoken = "a*")
    Lab.B -> text("b", ::whole, spoken = "b*")
    Lch.L -> text("Lightness", ::whole)
    Lch.C -> text("Chroma", ::whole)
    Oklab.L, OkLch.L -> text("Lightness", ::percentOfOne)
    Oklab.A -> text("a", ::thousandths)
    Oklab.B -> text("b", ::thousandths)
    OkLch.C -> text("Chroma", ::thousandths)
    Lch.H, OkLch.H, Hsl.H, Hsv.H, Hwb.H, Okhsl.H, Okhsv.H -> text("Hue", ::degrees)
    Hsl.S, Hsv.S -> text("Saturation", ::percent)
    Hsl.L -> text("Lightness", ::percent)
    Hsv.V -> text("Value", ::percent)
    Hwb.W -> text("Whiteness", ::percent)
    Hwb.B -> text("Blackness", ::percent)
    Okhsl.S, Okhsv.S -> text("Saturation", ::percentOfOne)
    Okhsl.L -> text("Lightness", ::percentOfOne)
    Okhsv.V -> text("Value", ::percentOfOne)
    Cmyk.C -> text("Cyan", ::percentOfOne)
    Cmyk.M -> text("Magenta", ::percentOfOne)
    Cmyk.Y -> text("Yellow", ::percentOfOne)
    Cmyk.K -> text("Key", ::percentOfOne)
    else -> null
}

/** The label above [channel]'s slider: its name, or an app channel's id. */
internal fun channelLabel(channel: ColorChannel): String = libraryText(channel)?.label ?: channel.id

/** What a screen reader calls [channel]: its label, except Lab's, which are read as L*, a* and b*. */
internal fun channelSpokenLabel(channel: ColorChannel): String = libraryText(channel)?.spoken ?: channel.id

/**
 * [channel]'s [value] as its slider shows it and a screen reader announces it. An app's channel shows
 * the decimals its step needs, and a hue its degree sign.
 */
internal fun channelValueText(channel: ColorChannel, value: Double): String {
    libraryText(channel)?.let { return it.format(value) }
    val number = decimals(value, decimalsFor(channel.step))
    return if (channel.isHue) "$number°" else number
}

/** The alpha slider's label. */
internal const val ALPHA_LABEL: String = "Alpha"

/** Alpha as its slider shows it, 0 to 255. */
internal fun alphaValueText(alpha: Double): String = bytes(alpha)

/** What a screen reader calls a plane over [x] and [y]: "Saturation and lightness". */
internal fun planeLabel(x: ColorChannel, y: ColorChannel): String =
    "${channelSpokenLabel(x)} and ${inSentence(channelSpokenLabel(y))}"

/** A plane's values as a screen reader announces them: "40% saturation, 60% lightness". */
internal fun planeValueText(x: ColorChannel, xValue: Double, y: ColorChannel, yValue: Double): String =
    "${channelValueText(x, xValue)} ${inSentence(channelSpokenLabel(x))}, ${channelValueText(y, yValue)} ${inSentence(channelSpokenLabel(y))}"

/** A plane's four accessibility actions, named after its channels. */
internal fun planeActionLabels(x: ColorChannel, y: ColorChannel): PlaneActionLabels {
    val xName = inSentence(channelSpokenLabel(x))
    val yName = inSentence(channelSpokenLabel(y))
    return PlaneActionLabels(
        increaseX = "Increase $xName",
        decreaseX = "Decrease $xName",
        increaseY = "Increase $yName",
        decreaseY = "Decrease $yName",
    )
}

// A label as it reads mid-sentence: a capitalized word loses its capital, and a symbol such as L* or X
// keeps it.
private fun inSentence(label: String): String =
    if (label.length > 1 && label[1].isLowerCase()) label.replaceFirstChar { it.lowercaseChar() } else label

private fun bytes(value: Double): String = (value * 255.0).roundToInt().toString()

private fun whole(value: Double): String = value.roundToInt().toString()

private fun thousandths(value: Double): String = decimals(value, 3)

private fun degrees(value: Double): String = "${value.roundToInt()}°"

private fun percent(value: Double): String = "${value.roundToInt()}%"

private fun percentOfOne(value: Double): String = "${(value * 100.0).roundToInt()}%"

/** [value] with [places] decimals, rounded half away from zero, with no sign on a value that rounds to zero. */
internal fun decimals(value: Double, places: Int): String {
    var scale = 1L
    repeat(places) { scale *= 10L }
    val scaled = (abs(value) * scale).roundToLong()
    val sign = if (value < 0.0 && scaled != 0L) "-" else ""
    if (places == 0) return "$sign$scaled"
    val fraction = (scaled % scale).toString().padStart(places, '0')
    return "$sign${scaled / scale}.$fraction"
}

// The decimals that show a change of [step]: 0 for 1 or more, 2 for 0.01, 3 for 1/255. The allowance
// keeps a power of ten whose logarithm lands a hair past an integer from taking one decimal too many.
private fun decimalsFor(step: Double): Int = ceil(-log10(step) - 1e-9).toInt().coerceAtLeast(0)
