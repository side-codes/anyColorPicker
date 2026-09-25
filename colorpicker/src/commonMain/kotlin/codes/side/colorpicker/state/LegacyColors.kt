package codes.side.colorpicker.state

import codes.side.color.Cmyk
import codes.side.color.ColorValue
import codes.side.color.Hsl
import codes.side.color.Lab
import codes.side.color.OkLch
import codes.side.color.Okhsl
import codes.side.color.Okhsv
import codes.side.color.Oklab
import codes.side.color.Srgb
import codes.side.colorpicker.model.CmykColor
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.LabColor
import codes.side.colorpicker.model.OKLAB_AB_RANGE
import codes.side.colorpicker.model.OkhslColor
import codes.side.colorpicker.model.OkhsvColor
import codes.side.colorpicker.model.OklabColor
import codes.side.colorpicker.model.OklchColor
import codes.side.colorpicker.model.PickerColor
import codes.side.colorpicker.model.RgbColor

// Between ColorValue and the model package's classes, which hold Floats in their own units (HSL's
// saturation and lightness 0–1) and refuse anything outside their ranges.

internal fun PickerColor.toColorValue(): ColorValue = when (this) {
    is HslColor -> Hsl(hue.toDouble(), saturation * 100.0, lightness * 100.0, alpha.toDouble())
    is RgbColor -> Srgb(red.toDouble(), green.toDouble(), blue.toDouble(), alpha.toDouble())
    is CmykColor -> Cmyk(cyan.toDouble(), magenta.toDouble(), yellow.toDouble(), key.toDouble(), alpha.toDouble())
    is LabColor -> Lab(l.toDouble(), a.toDouble(), b.toDouble(), alpha.toDouble())
    is OklabColor -> Oklab(l.toDouble(), a.toDouble(), b.toDouble(), alpha.toDouble())
    is OklchColor -> OkLch(l.toDouble(), chroma.toDouble(), hue.toDouble(), alpha.toDouble())
    is OkhslColor -> Okhsl(hue.toDouble(), saturation.toDouble(), lightness.toDouble(), alpha.toDouble())
    is OkhsvColor -> Okhsv(hue.toDouble(), saturation.toDouble(), value.toDouble(), alpha.toDouble())
}

internal fun hslColorOf(hsl: ColorValue, hue: Double): HslColor = HslColor(
    hue = hue.toFloat(),
    saturation = fraction(hsl[Hsl.S], 100.0),
    lightness = fraction(hsl[Hsl.L], 100.0),
    alpha = hsl.alpha.toFloat(),
)

internal fun rgbColorOf(rgb: ColorValue): RgbColor = RgbColor(
    red = fraction(rgb[Srgb.R]),
    green = fraction(rgb[Srgb.G]),
    blue = fraction(rgb[Srgb.B]),
    alpha = rgb.alpha.toFloat(),
)

internal fun cmykColorOf(cmyk: ColorValue): CmykColor = CmykColor(
    cyan = fraction(cmyk[Cmyk.C]),
    magenta = fraction(cmyk[Cmyk.M]),
    yellow = fraction(cmyk[Cmyk.Y]),
    key = fraction(cmyk[Cmyk.K]),
    alpha = cmyk.alpha.toFloat(),
)

internal fun labColorOf(lab: ColorValue): LabColor = LabColor(
    l = bounded(lab[Lab.L], 0f..100f),
    a = bounded(lab[Lab.A], -128f..127f),
    b = bounded(lab[Lab.B], -128f..127f),
    alpha = lab.alpha.toFloat(),
)

internal fun oklabColorOf(oklab: ColorValue): OklabColor = OklabColor(
    l = fraction(oklab[Oklab.L]),
    a = bounded(oklab[Oklab.A], -OKLAB_AB_RANGE..OKLAB_AB_RANGE),
    b = bounded(oklab[Oklab.B], -OKLAB_AB_RANGE..OKLAB_AB_RANGE),
    alpha = oklab.alpha.toFloat(),
)

internal fun oklchColorOf(oklch: ColorValue, hue: Double): OklchColor = OklchColor(
    l = fraction(oklch[OkLch.L]),
    chroma = bounded(oklch[OkLch.C], 0f..OKLAB_AB_RANGE),
    hue = hue.toFloat(),
    alpha = oklch.alpha.toFloat(),
)

internal fun okhslColorOf(okhsl: ColorValue, hue: Double): OkhslColor = OkhslColor(
    hue = hue.toFloat(),
    saturation = fraction(okhsl[Okhsl.S]),
    lightness = fraction(okhsl[Okhsl.L]),
    alpha = okhsl.alpha.toFloat(),
)

internal fun okhsvColorOf(okhsv: ColorValue, hue: Double): OkhsvColor = OkhsvColor(
    hue = hue.toFloat(),
    saturation = fraction(okhsv[Okhsv.S]),
    value = fraction(okhsv[Okhsv.V]),
    alpha = okhsv.alpha.toFloat(),
)

private fun fraction(value: Double?, scale: Double = 1.0): Float = bounded(value?.div(scale), 0f..1f)

private fun bounded(value: Double?, range: ClosedFloatingPointRange<Float>): Float = (value ?: 0.0).toFloat().coerceIn(range)
