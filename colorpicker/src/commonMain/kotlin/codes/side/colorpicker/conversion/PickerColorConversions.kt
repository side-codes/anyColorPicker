package codes.side.colorpicker.conversion

import codes.side.colorpicker.model.CmykColor
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.LabColor
import codes.side.colorpicker.model.OkhslColor
import codes.side.colorpicker.model.OkhsvColor
import codes.side.colorpicker.model.OklabColor
import codes.side.colorpicker.model.OklchColor
import codes.side.colorpicker.model.PickerColor
import codes.side.colorpicker.model.RgbColor

/**
 * Converts any [PickerColor] to sRGB, the hub every other space converts through.
 *
 * With eight spaces, writing each pair out would mean sixty-four branches to keep in
 * step. Routing through RGB costs one extra conversion when neither end is RGB, which is
 * what the pairwise code did anyway.
 */
internal fun PickerColor.toRgbColor(): RgbColor = when (this) {
    is RgbColor -> this
    is HslColor -> toRgb()
    is CmykColor -> toRgb()
    is LabColor -> toRgb()
    is OklabColor -> toRgb()
    is OklchColor -> toRgb()
    is OkhslColor -> toRgb()
    is OkhsvColor -> toRgb()
}

/** Returns a copy of this color with [alpha] replaced, keeping its space. */
internal fun PickerColor.withAlpha(alpha: Float): PickerColor = when (this) {
    is RgbColor -> copy(alpha = alpha)
    is HslColor -> copy(alpha = alpha)
    is CmykColor -> copy(alpha = alpha)
    is LabColor -> copy(alpha = alpha)
    is OklabColor -> copy(alpha = alpha)
    is OklchColor -> copy(alpha = alpha)
    is OkhslColor -> copy(alpha = alpha)
    is OkhsvColor -> copy(alpha = alpha)
}
