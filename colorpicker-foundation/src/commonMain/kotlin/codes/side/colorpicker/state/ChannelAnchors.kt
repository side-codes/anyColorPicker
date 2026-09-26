package codes.side.colorpicker.state

import codes.side.color.Cmyk
import codes.side.color.ColorChannel
import codes.side.color.ColorSpace
import codes.side.color.ColorValue
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

// Where a channel sits while another is shown on its own: on an independent track, and in the
// reference color a remembered hue is carried across families with. Each gives a clear color of
// middle lightness; a channel of an app's space sits at the middle of its reference range.
internal fun anchorOf(channel: ColorChannel): Double = when (channel) {
    Srgb.R, Srgb.G, Srgb.B, SrgbLinear.R, SrgbLinear.G, SrgbLinear.B, DisplayP3.R, DisplayP3.G, DisplayP3.B -> 0.0
    Lab.L -> 50.0
    Lab.A, Lab.B -> 0.0
    Lch.L -> 70.0
    Lch.C -> 50.0
    Oklab.L -> 0.7
    Oklab.A, Oklab.B -> 0.0
    OkLch.L -> 0.75
    OkLch.C -> 0.12
    Hsl.S -> 100.0
    Hsl.L -> 50.0
    Hsv.S, Hsv.V -> 100.0
    Hwb.W, Hwb.B -> 0.0
    Okhsl.S -> 0.85
    Okhsl.L -> 0.5
    Okhsv.S -> 0.85
    Okhsv.V -> 1.0
    Cmyk.C, Cmyk.M, Cmyk.Y, Cmyk.K -> 0.0
    else -> (channel.referenceRange.start + channel.referenceRange.endInclusive) / 2.0
}

// [space]'s color at [hue], its other channels at their anchors.
internal fun referenceColor(space: ColorSpace, hue: Double): ColorValue {
    val components = DoubleArray(space.channels.size) { index ->
        val channel = space.channels[index]
        if (channel.isHue) hue else anchorOf(channel)
    }
    return space.color(components)
}
