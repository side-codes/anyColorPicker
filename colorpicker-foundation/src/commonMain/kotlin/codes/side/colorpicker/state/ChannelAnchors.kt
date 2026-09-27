package codes.side.colorpicker.state

import codes.side.color.AnalogousCategory
import codes.side.color.Cmyk
import codes.side.color.ColorChannel
import codes.side.color.ColorSpace
import codes.side.color.ColorValue
import codes.side.color.HslColorSpace
import codes.side.color.HsvColorSpace
import codes.side.color.HwbColorSpace
import codes.side.color.Lab
import codes.side.color.Lch
import codes.side.color.Okhsl
import codes.side.color.Okhsv
import codes.side.color.Oklab
import codes.side.color.Oklch
import codes.side.color.RgbColorSpace

// Where a channel sits while another is shown on its own: on an independent track, and in the
// reference color a remembered hue is carried across families with. Each gives a clear color of
// middle lightness. RGB, HSL, HSV and HWB channels sit by their kind of space, so an app's own sit
// where the library's do: at the middle of any other channel's reference range, HWB's whiteness and
// blackness are grey, and its hue track would show none.
internal fun anchorOf(channel: ColorChannel): Double {
    val range = channel.referenceRange
    return when (channel) {
        Lab.L -> 50.0
        Lab.A, Lab.B -> 0.0
        Lch.L -> 70.0
        Lch.C -> 50.0
        Oklab.L -> 0.7
        Oklab.A, Oklab.B -> 0.0
        Oklch.L -> 0.75
        Oklch.C -> 0.12
        Okhsl.S -> 0.85
        Okhsl.L -> 0.5
        Okhsv.S -> 0.85
        Okhsv.V -> 1.0
        Cmyk.C, Cmyk.M, Cmyk.Y, Cmyk.K -> 0.0
        else -> when (channel.space) {
            is RgbColorSpace, is HwbColorSpace -> range.start
            is HsvColorSpace -> range.endInclusive
            is HslColorSpace -> if (channel.analogous == AnalogousCategory.Colorfulness) range.endInclusive else (range.start + range.endInclusive) / 2.0
            else -> (range.start + range.endInclusive) / 2.0
        }
    }
}

// [space]'s color at [hue], its other channels at their anchors.
internal fun referenceColor(space: ColorSpace, hue: Double): ColorValue {
    val components = DoubleArray(space.channels.size) { index ->
        val channel = space.channels[index]
        if (channel.isHue) hue else anchorOf(channel)
    }
    return space.color(components)
}
