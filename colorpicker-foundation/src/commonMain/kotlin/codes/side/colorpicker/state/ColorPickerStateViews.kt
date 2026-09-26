package codes.side.colorpicker.state

import codes.side.color.CmykColor
import codes.side.color.DisplayP3Color
import codes.side.color.HslColor
import codes.side.color.HsvColor
import codes.side.color.HwbColor
import codes.side.color.LabColor
import codes.side.color.LchColor
import codes.side.color.OkLchColor
import codes.side.color.OkhslColor
import codes.side.color.OkhsvColor
import codes.side.color.OklabColor
import codes.side.color.SrgbColor
import codes.side.color.SrgbLinearColor
import codes.side.color.XyzD50Color
import codes.side.color.XyzD65Color
import codes.side.color.toCmyk
import codes.side.color.toDisplayP3
import codes.side.color.toHsl
import codes.side.color.toHsv
import codes.side.color.toHwb
import codes.side.color.toLab
import codes.side.color.toLch
import codes.side.color.toOkLch
import codes.side.color.toOkhsl
import codes.side.color.toOkhsv
import codes.side.color.toOklab
import codes.side.color.toSrgb
import codes.side.color.toSrgbLinear
import codes.side.color.toXyzD50
import codes.side.color.toXyzD65

// Each view is ColorPickerState.value converted, so a grey's hue reads null here; sliders show the
// remembered one through ColorPickerState.displayValue.

/** The color in sRGB. */
public val ColorPickerState.srgb: SrgbColor get() = value.toSrgb()

/** Replaces the color with [color]. */
public fun ColorPickerState.set(color: SrgbColor) {
    value = color.value
}

/** The color in linear sRGB. */
public val ColorPickerState.srgbLinear: SrgbLinearColor get() = value.toSrgbLinear()

/** Replaces the color with [color]. */
public fun ColorPickerState.set(color: SrgbLinearColor) {
    value = color.value
}

/** The color in Display P3. */
public val ColorPickerState.displayP3: DisplayP3Color get() = value.toDisplayP3()

/** Replaces the color with [color]. */
public fun ColorPickerState.set(color: DisplayP3Color) {
    value = color.value
}

/** The color in CIE XYZ relative to D65. */
public val ColorPickerState.xyzD65: XyzD65Color get() = value.toXyzD65()

/** Replaces the color with [color]. */
public fun ColorPickerState.set(color: XyzD65Color) {
    value = color.value
}

/** The color in CIE XYZ relative to D50. */
public val ColorPickerState.xyzD50: XyzD50Color get() = value.toXyzD50()

/** Replaces the color with [color]. */
public fun ColorPickerState.set(color: XyzD50Color) {
    value = color.value
}

/** The color in CIELAB. */
public val ColorPickerState.lab: LabColor get() = value.toLab()

/** Replaces the color with [color]. */
public fun ColorPickerState.set(color: LabColor) {
    value = color.value
}

/** The color in CIE LCH. A grey's hue is null. */
public val ColorPickerState.lch: LchColor get() = value.toLch()

/** Replaces the color with [color]. */
public fun ColorPickerState.set(color: LchColor) {
    value = color.value
}

/** The color in Oklab. */
public val ColorPickerState.oklab: OklabColor get() = value.toOklab()

/** Replaces the color with [color]. */
public fun ColorPickerState.set(color: OklabColor) {
    value = color.value
}

/** The color in OkLCh. A grey's hue is null. */
public val ColorPickerState.okLch: OkLchColor get() = value.toOkLch()

/** Replaces the color with [color]. */
public fun ColorPickerState.set(color: OkLchColor) {
    value = color.value
}

/** The color in HSL. A grey's hue is null. */
public val ColorPickerState.hsl: HslColor get() = value.toHsl()

/** Replaces the color with [color]. */
public fun ColorPickerState.set(color: HslColor) {
    value = color.value
}

/** The color in HWB. A grey's hue is null. */
public val ColorPickerState.hwb: HwbColor get() = value.toHwb()

/** Replaces the color with [color]. */
public fun ColorPickerState.set(color: HwbColor) {
    value = color.value
}

/** The color in HSV. A grey's hue is null. */
public val ColorPickerState.hsv: HsvColor get() = value.toHsv()

/** Replaces the color with [color]. */
public fun ColorPickerState.set(color: HsvColor) {
    value = color.value
}

/** The color in Okhsl, brought to sRGB's edge when outside it. A grey's hue is null. */
public val ColorPickerState.okhsl: OkhslColor get() = value.toOkhsl()

/** Replaces the color with [color]. */
public fun ColorPickerState.set(color: OkhslColor) {
    value = color.value
}

/** The color in Okhsv, brought to sRGB's edge when outside it. A grey's hue is null. */
public val ColorPickerState.okhsv: OkhsvColor get() = value.toOkhsv()

/** Replaces the color with [color]. */
public fun ColorPickerState.set(color: OkhsvColor) {
    value = color.value
}

/** The color in naive CMYK. */
public val ColorPickerState.cmyk: CmykColor get() = value.toCmyk()

/** Replaces the color with [color]. */
public fun ColorPickerState.set(color: CmykColor) {
    value = color.value
}
