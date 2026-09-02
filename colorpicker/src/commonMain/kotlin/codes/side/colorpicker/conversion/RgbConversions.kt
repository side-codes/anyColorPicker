package codes.side.colorpicker.conversion

import codes.side.colorpicker.model.RgbColor

/**
 * Returns [RgbColor.Black] or [RgbColor.White], whichever contrasts better with this
 * color. Uses WCAG relative luminance with a threshold of 0.179 — the luminance at
 * which black and white text yield equal WCAG contrast ratios — rather than the
 * simpler 0.5 midpoint, which would pick black on backgrounds too dark for it.
 * Alpha is ignored.
 */
internal fun RgbColor.contrastColor(): RgbColor {
    // WCAG relative luminance on linearized sRGB channels.
    val luminance = 0.2126 * linearize(red.toDouble()) +
            0.7152 * linearize(green.toDouble()) +
            0.0722 * linearize(blue.toDouble())
    return if (luminance > 0.179) RgbColor.Black else RgbColor.White
}
