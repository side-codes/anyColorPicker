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
    public const val DisabledAlpha: Float = 0.38f

    /**
     * Colour a disabled component keeps. Full, by default: dimming alone is the Material
     * convention, and draining the colour as well is a choice a caller makes.
     */
    public const val DisabledSaturation: Float = 1f

    // surfaceBright/surfaceDim keep visible checkerboard contrast in both
    // light and dark color schemes.
    /**
     * Creates a [ColorPickerColors] with defaults taken from
     * `MaterialTheme.colorScheme` (`surfaceBright`/`surfaceDim` for the
     * transparency checkerboard cells). A color left [Color.Unspecified] takes its default, so a
     * call naming one value keeps the rest.
     */
    @Composable
    public fun colors(
        checkerboardLight: Color = Color.Unspecified,
        checkerboardDark: Color = Color.Unspecified,
        disabledAlpha: Float = DisabledAlpha,
        disabledSaturation: Float = DisabledSaturation,
    ): ColorPickerColors = ColorPickerColors(
        checkerboardLight = MaterialTheme.colorScheme.surfaceBright,
        checkerboardDark = MaterialTheme.colorScheme.surfaceDim,
        disabledAlpha = DisabledAlpha,
        disabledSaturation = DisabledSaturation,
    ).copy(
        checkerboardLight = checkerboardLight,
        checkerboardDark = checkerboardDark,
        disabledAlpha = disabledAlpha,
        disabledSaturation = disabledSaturation,
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

    /**
     * Creates a [ColorPickerDimensions] from the constants above. A size left [Dp.Unspecified] takes
     * its constant, so a call naming one value keeps the rest.
     */
    @Composable
    public fun dimensions(
        trackHeight: Dp = Dp.Unspecified,
        thumbWidth: Dp = Dp.Unspecified,
        thumbTrackGap: Dp = Dp.Unspecified,
        planeMinSize: Dp = Dp.Unspecified,
        planeThumbSize: Dp = Dp.Unspecified,
    ): ColorPickerDimensions = ColorPickerDimensions(
        trackHeight = TrackHeight,
        thumbWidth = ThumbWidth,
        thumbTrackGap = ThumbTrackGap,
        planeMinSize = PlaneMinSize,
        planeThumbSize = PlaneThumbSize,
    ).copy(
        trackHeight = trackHeight,
        thumbWidth = thumbWidth,
        thumbTrackGap = thumbTrackGap,
        planeMinSize = planeMinSize,
        planeThumbSize = planeThumbSize,
    )

    /**
     * Creates a [ColorPickerShapes] with a fully rounded slider track, a swatch shape taken from
     * `MaterialTheme.shapes.small` and a plane shape from `MaterialTheme.shapes.medium`. A shape left
     * `null` takes its default, so a call naming one shape keeps the rest.
     */
    @Composable
    public fun shapes(
        trackShape: Shape? = null,
        swatchShape: Shape? = null,
        planeShape: Shape? = null,
    ): ColorPickerShapes = ColorPickerShapes(
        trackShape = CircleShape,
        swatchShape = MaterialTheme.shapes.small,
        planeShape = MaterialTheme.shapes.medium,
    ).copy(
        trackShape = trackShape,
        swatchShape = swatchShape,
        planeShape = planeShape,
    )
}
