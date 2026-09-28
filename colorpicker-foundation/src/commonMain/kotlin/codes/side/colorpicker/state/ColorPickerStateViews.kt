package codes.side.colorpicker.state

import codes.side.color.CmykColor
import codes.side.color.DisplayP3Color
import codes.side.color.HslColor
import codes.side.color.HsvColor
import codes.side.color.HwbColor
import codes.side.color.LabColor
import codes.side.color.LchColor
import codes.side.color.OkhslColor
import codes.side.color.OkhsvColor
import codes.side.color.OklabColor
import codes.side.color.OklchColor
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
import codes.side.color.toOkhsl
import codes.side.color.toOkhsv
import codes.side.color.toOklab
import codes.side.color.toOklch
import codes.side.color.toSrgb
import codes.side.color.toSrgbLinear
import codes.side.color.toXyzD50
import codes.side.color.toXyzD65

// Each view is ColorPickerState.value converted, so a grey's hue reads null here; sliders show the
// remembered one through ColorPickerState.displayValue.

/** The color in sRGB. */
public val ColorPickerState.srgb: SrgbColor get() = value.toSrgb()

/** The color in linear sRGB. */
public val ColorPickerState.srgbLinear: SrgbLinearColor get() = value.toSrgbLinear()

/** The color in Display P3. */
public val ColorPickerState.displayP3: DisplayP3Color get() = value.toDisplayP3()

/** The color in CIE XYZ relative to D65. */
public val ColorPickerState.xyzD65: XyzD65Color get() = value.toXyzD65()

/** The color in CIE XYZ relative to D50. */
public val ColorPickerState.xyzD50: XyzD50Color get() = value.toXyzD50()

/** The color in CIELAB. */
public val ColorPickerState.lab: LabColor get() = value.toLab()

/** The color in CIE LCH. A grey's hue is null. */
public val ColorPickerState.lch: LchColor get() = value.toLch()

/** The color in Oklab. */
public val ColorPickerState.oklab: OklabColor get() = value.toOklab()

/** The color in OkLCh. A grey's hue is null. */
public val ColorPickerState.oklch: OklchColor get() = value.toOklch()

/** The color in HSL. A grey's hue is null. */
public val ColorPickerState.hsl: HslColor get() = value.toHsl()

/** The color in HWB. A grey's hue is null. */
public val ColorPickerState.hwb: HwbColor get() = value.toHwb()

/** The color in HSV. A grey's hue is null. */
public val ColorPickerState.hsv: HsvColor get() = value.toHsv()

/** The color in Okhsl, brought to sRGB's edge when outside it. A grey's hue is null. */
public val ColorPickerState.okhsl: OkhslColor get() = value.toOkhsl()

/** The color in Okhsv, brought to sRGB's edge when outside it. A grey's hue is null. */
public val ColorPickerState.okhsv: OkhsvColor get() = value.toOkhsv()

/** The color in naive CMYK. */
public val ColorPickerState.cmyk: CmykColor get() = value.toCmyk()
