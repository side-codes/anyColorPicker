package codes.side.colorpicker.model

import androidx.compose.runtime.Immutable

/**
 * An immutable color in one of the supported color spaces: [RgbColor], [HslColor],
 * [CmykColor], [LabColor], [OklabColor], [OklchColor], [OkhslColor], or [OkhsvColor].
 *
 * All implementations validate their channels on construction (the constructor throws
 * [IllegalArgumentException] for out-of-range or NaN values) and share an [alpha] channel.
 */
@Immutable
public sealed interface PickerColor {
    /** Opacity in `0..1`, where `0` is fully transparent and `1` is fully opaque. */
    public val alpha: Float
}
