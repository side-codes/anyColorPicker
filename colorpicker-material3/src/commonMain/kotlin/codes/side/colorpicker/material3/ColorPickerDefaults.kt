package codes.side.colorpicker.material3

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import codes.side.color.ColorChannel
import codes.side.colorpicker.foundation.ColorSliderScope
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode

// Material's slider handle height (SliderTokens.HandleHeight).
private val SliderThumbHeight = 44.dp

// How far outside the indicator the focus ring sits.
private val FocusRingGap = 4.dp

// A picker's plane, width over height.
private const val PLANE_ASPECT_RATIO = 1.6f

/**
 * Defaults for the Material color picker components: the theme values, whose composable factories read the ambient
 * [MaterialTheme] at the call site and so follow the app's color scheme and shape system; the slider and plane
 * thumbs; and the slots a picker draws unless given others.
 */
public object ColorPickerDefaults {

    /** Default height of a color slider's gradient track. */
    public val TrackHeight: Dp = 16.dp

    /** Size a [ColorSwatch] falls back to when given none. */
    public val SwatchSize: Dp = 48.dp

    /**
     * Width the track reserves for the thumb, matching the Material 3 handle. A custom
     * thumb wider than this must say so, or the track's gap closes underneath it.
     */
    public val ThumbWidth: Dp = 4.dp

    /** Clearance left between the thumb and each end of the track. */
    public val ThumbTrackGap: Dp = 6.dp

    /** Size a [ColorPlane] falls back to. */
    public val PlaneMinSize: Dp = 200.dp

    /** Diameter of a [ColorPlane]'s position indicator. */
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

    /**
     * The sliders' default thumb, drawn as Material draws its handle: a bar in [color] with round ends,
     * half as wide while [interactionSource] reports a press or drag. The narrowing is drawn inside a
     * fixed layout width, so the track beside the thumb does not move as it narrows.
     */
    @Composable
    public fun SliderThumb(interactionSource: InteractionSource, color: Color, modifier: Modifier = Modifier) {
        val pressed by interactionSource.collectIsPressedAsState()
        val dragged by interactionSource.collectIsDraggedAsState()
        Canvas(modifier.size(ThumbWidth, SliderThumbHeight)) {
            val width = if (pressed || dragged) size.width / 2f else size.width
            drawRoundRect(
                color = color,
                topLeft = Offset((size.width - width) / 2f, 0f),
                size = Size(width, size.height),
                cornerRadius = CornerRadius(width / 2f),
            )
        }
    }

    /**
     * The planes' default position indicator, [diameter] across: a white ring over a dark halo, and a
     * second ring outside it while [interactionSource] holds focus from the keyboard.
     */
    @Composable
    public fun PlaneThumb(
        interactionSource: InteractionSource,
        modifier: Modifier = Modifier,
        diameter: Dp = currentDimensions().planeThumbSize,
    ) {
        // Focus is marked on the indicator rather than around the plane: it is where the eye
        // already is, and it moves with the value the arrow keys are changing.
        //
        // Only for whoever needs it. Pressing the surface takes focus too, so a finger would
        // otherwise leave the ring sitting there after the drag, marking a thing the toucher has
        // no way to act on. A platform with no touch reports Keyboard throughout.
        val focused by interactionSource.collectIsFocusedAsState()
        val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
        val showFocus = focused && keyboard

        Canvas(modifier.size(if (showFocus) diameter + FocusRingGap * 2 else diameter)) {
            val outer = size.minDimension / 2f - 2.dp.toPx()
            val radius = if (showFocus) outer - FocusRingGap.toPx() else outer
            // A dark halo under a white ring keeps the indicator readable at both
            // ends of the surface, where a single-colour ring vanishes.
            fun ring(at: Float) {
                drawCircle(
                    color = Color.Black.copy(alpha = 0.35f),
                    radius = at,
                    style = Stroke(width = 4.dp.toPx()),
                )
                drawCircle(
                    color = Color.White,
                    radius = at,
                    style = Stroke(width = 2.dp.toPx()),
                )
            }

            ring(radius)
            if (showFocus) ring(outer)
        }
    }

    /**
     * The plane a picker draws unless given another: a [ChannelPlane] over the axes the picker hands it,
     * filling the width the picker gives it, the whole picker's or its start half's, at 1.6 times its height.
     */
    public fun plane(
        enabled: Boolean,
        onValueChangeFinished: () -> Unit,
    ): @Composable (ColorPickerState, ColorChannel, ColorChannel) -> Unit = { state, x, y ->
        ChannelPlane(
            state,
            x,
            y,
            Modifier.fillMaxWidth().aspectRatio(PLANE_ASPECT_RATIO),
            enabled = enabled,
            onValueChangeFinished = onValueChangeFinished,
        )
    }

    /** The slider a picker draws for each channel unless given another: a [ChannelSlider] coloured by [coloringMode]. */
    public fun channelSlider(
        enabled: Boolean,
        coloringMode: ColoringMode,
        onValueChangeFinished: () -> Unit,
        thumb: @Composable ColorSliderScope.() -> Unit,
    ): @Composable (ColorPickerState, ColorChannel) -> Unit = { state, channel ->
        ChannelSlider(
            state,
            channel,
            enabled = enabled,
            coloringMode = coloringMode,
            onValueChangeFinished = onValueChangeFinished,
            thumb = thumb,
        )
    }

    /**
     * [channelSlider] with each channel coloured as its own space's sliders are by default:
     * [ColoringMode.defaultFor] that space.
     */
    public fun channelSlider(
        enabled: Boolean,
        onValueChangeFinished: () -> Unit,
        thumb: @Composable ColorSliderScope.() -> Unit,
    ): @Composable (ColorPickerState, ColorChannel) -> Unit = { state, channel ->
        ChannelSlider(
            state,
            channel,
            enabled = enabled,
            onValueChangeFinished = onValueChangeFinished,
            thumb = thumb,
        )
    }

    /** The alpha slider a picker draws unless given another: an [AlphaSlider]. */
    public fun alphaSlider(
        enabled: Boolean,
        onValueChangeFinished: () -> Unit,
        thumb: @Composable ColorSliderScope.() -> Unit,
    ): @Composable (ColorPickerState) -> Unit = { state ->
        AlphaSlider(state, enabled = enabled, onValueChangeFinished = onValueChangeFinished, thumb = thumb)
    }
}
