package codes.side.color

import codes.side.color.internal.DISPLAY_P3_LINEAR_TO_LMS
import codes.side.color.internal.DISPLAY_P3_LINEAR_TO_XYZ_D65
import codes.side.color.internal.LMS_TO_DISPLAY_P3_LINEAR
import codes.side.color.internal.LMS_TO_SRGB_LINEAR
import codes.side.color.internal.SRGB_LINEAR_TO_LMS
import codes.side.color.internal.SRGB_LINEAR_TO_XYZ_D65
import codes.side.color.internal.XYZ_D65_TO_DISPLAY_P3_LINEAR
import codes.side.color.internal.XYZ_D65_TO_SRGB_LINEAR

/** Linear-light sRGB. CSS `srgb-linear`. */
public object SrgbLinear : RgbColorSpace(
    "srgb-linear",
    RgbPrimaries.Srgb,
    WhitePoint.D65,
    TransferFunction.Linear,
    SRGB_LINEAR_TO_XYZ_D65,
    XYZ_D65_TO_SRGB_LINEAR,
    LMS_TO_SRGB_LINEAR,
    SRGB_LINEAR_TO_LMS,
    null,
)

/** sRGB. CSS `srgb`, and what `rgb()`, hex and named colors mean. */
public object Srgb : RgbColorSpace(
    "srgb",
    RgbPrimaries.Srgb,
    WhitePoint.D65,
    TransferFunction.Srgb,
    SRGB_LINEAR_TO_XYZ_D65,
    XYZ_D65_TO_SRGB_LINEAR,
    LMS_TO_SRGB_LINEAR,
    SRGB_LINEAR_TO_LMS,
    SrgbLinear,
)

/** Display P3: P3 primaries, D65, the sRGB curve. CSS `display-p3`. */
public object DisplayP3 : RgbColorSpace(
    "display-p3",
    RgbPrimaries.DisplayP3,
    WhitePoint.D65,
    TransferFunction.Srgb,
    DISPLAY_P3_LINEAR_TO_XYZ_D65,
    XYZ_D65_TO_DISPLAY_P3_LINEAR,
    LMS_TO_DISPLAY_P3_LINEAR,
    DISPLAY_P3_LINEAR_TO_LMS,
    null,
)
