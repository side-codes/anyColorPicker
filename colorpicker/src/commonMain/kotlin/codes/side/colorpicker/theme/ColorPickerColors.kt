package codes.side.colorpicker.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Colors used by color picker components. Obtain instances via
 * [ColorPickerDefaults.colors] so defaults come from a single source.
 *
 * @property checkerboardLight color of the light cells of the transparency checkerboard.
 * @property checkerboardDark color of the dark cells of the transparency checkerboard.
 * @property disabledAlpha opacity a disabled track and thumb draw at. One value rather than a
 * parallel set of disabled colors: a picker's track is a gradient of the colors being chosen,
 * so there is no fixed color to swap it for.
 */
@Immutable
public data class ColorPickerColors(
    public val checkerboardLight: Color,
    public val checkerboardDark: Color,
    public val disabledAlpha: Float,
)
