package codes.side.colorpicker.theme

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Default values used by color picker components.
 *
 * The composable factories read the ambient [MaterialTheme] at the call site, so the
 * defaults automatically follow the app's color scheme and shape system. Pass explicit
 * arguments to override individual values.
 */
public object ColorPickerDefaults {

    /** Default height of a color slider's gradient track. */
    public val TrackHeight: Dp = 16.dp

    /** Size a [codes.side.colorpicker.ui.ColorSwatch] falls back to when given none. */
    public val SwatchSize: Dp = 48.dp

    // surfaceBright/surfaceDim keep visible checkerboard contrast in both
    // light and dark color schemes.
    /**
     * Creates a [ColorPickerColors] with defaults taken from
     * `MaterialTheme.colorScheme` (`surfaceBright`/`surfaceDim` for the
     * transparency checkerboard cells).
     */
    @Composable
    public fun colors(
        checkerboardLight: Color = MaterialTheme.colorScheme.surfaceBright,
        checkerboardDark: Color = MaterialTheme.colorScheme.surfaceDim,
    ): ColorPickerColors = ColorPickerColors(
        checkerboardLight = checkerboardLight,
        checkerboardDark = checkerboardDark,
    )

    /**
     * Creates a [ColorPickerShapes] with a fully rounded slider track and a swatch
     * shape taken from `MaterialTheme.shapes.small`.
     */
    @Composable
    public fun shapes(
        trackShape: Shape = CircleShape,
        swatchShape: Shape = MaterialTheme.shapes.small,
    ): ColorPickerShapes = ColorPickerShapes(
        trackShape = trackShape,
        swatchShape = swatchShape,
    )
}
