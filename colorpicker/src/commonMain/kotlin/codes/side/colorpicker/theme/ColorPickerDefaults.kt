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

    /**
     * Width the track reserves for the thumb, matching the Material 3 handle. A custom
     * thumb wider than this must say so, or the track's gap closes underneath it.
     */
    public val ThumbWidth: Dp = 4.dp

    /** Clearance left between the thumb and each end of the track. */
    public val ThumbTrackGap: Dp = 6.dp

    /** Size a [codes.side.colorpicker.ui.ColorPlane] falls back to. */
    public val PlaneMinSize: Dp = 200.dp

    /** Diameter of a [codes.side.colorpicker.ui.ColorPlane]'s position indicator. */
    public val PlaneThumbSize: Dp = 24.dp

    /** Opacity a disabled component draws at, the Material 3 disabled content value. */
    public val DisabledAlpha: Float = 0.38f

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
        disabledAlpha: Float = DisabledAlpha,
    ): ColorPickerColors = ColorPickerColors(
        checkerboardLight = checkerboardLight,
        checkerboardDark = checkerboardDark,
        disabledAlpha = disabledAlpha,
    )

    /**
     * The colors in force here: whatever an enclosing [ColorPickerTheme] provided, or [colors]
     * when nothing did.
     *
     * Every component reads this in a parameter default rather than in its body, so an explicit
     * argument still wins and a component composed inside a picker inherits the picker's theme
     * without the call site forwarding it.
     */
    @Composable
    public fun currentColors(): ColorPickerColors = LocalColorPickerColors.current ?: colors()

    /** The shapes in force here; see [currentColors]. */
    @Composable
    public fun currentShapes(): ColorPickerShapes = LocalColorPickerShapes.current ?: shapes()

    /** The dimensions in force here; see [currentColors]. */
    @Composable
    public fun currentDimensions(): ColorPickerDimensions =
        LocalColorPickerDimensions.current ?: dimensions()

    /** Creates a [ColorPickerDimensions] from the constants above. */
    @Composable
    public fun dimensions(
        trackHeight: Dp = TrackHeight,
        thumbWidth: Dp = ThumbWidth,
        thumbTrackGap: Dp = ThumbTrackGap,
        planeMinSize: Dp = PlaneMinSize,
        planeThumbSize: Dp = PlaneThumbSize,
    ): ColorPickerDimensions = ColorPickerDimensions(
        trackHeight = trackHeight,
        thumbWidth = thumbWidth,
        thumbTrackGap = thumbTrackGap,
        planeMinSize = planeMinSize,
        planeThumbSize = planeThumbSize,
    )

    /**
     * Creates a [ColorPickerShapes] with a fully rounded slider track and a swatch
     * shape taken from `MaterialTheme.shapes.small`.
     */
    @Composable
    public fun shapes(
        trackShape: Shape = CircleShape,
        swatchShape: Shape = MaterialTheme.shapes.small,
        planeShape: Shape = MaterialTheme.shapes.medium,
    ): ColorPickerShapes = ColorPickerShapes(
        trackShape = trackShape,
        swatchShape = swatchShape,
        planeShape = planeShape,
    )
}
