package codes.side.colorpicker.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Colors used by color picker components. Obtain instances via
 * [ColorPickerDefaults.colors] so defaults come from a single source.
 *
 * @property checkerboardLight color of the light cells of the transparency checkerboard.
 * @property checkerboardDark color of the dark cells of the transparency checkerboard.
 */
@Immutable
public data class ColorPickerColors(
    public val checkerboardLight: Color,
    public val checkerboardDark: Color,
)
