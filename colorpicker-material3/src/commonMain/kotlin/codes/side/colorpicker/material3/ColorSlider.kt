package codes.side.colorpicker.material3

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import codes.side.color.ColorChannel
import codes.side.colorpicker.foundation.BasicColorSlider
import codes.side.colorpicker.foundation.ColorPickerStrings
import codes.side.colorpicker.foundation.ColorSliderScope
import codes.side.colorpicker.foundation.LocalColorPickerEnabled

/**
 * Building block for a single-channel color slider: a [BasicColorSlider] with a gradient
 * track, optional label row, and optional transparency checkerboard.
 *
 * [ChannelSlider] and [AlphaSlider] are drawn by the same slider. Use this one for a track
 * no [ColorChannel] describes, such as a value of your own. Left and Right move it by a
 * hundredth, Page Up and Page Down by a tenth, and Home and End to the ends; Up and Down are
 * left for moving focus. A screen reader steps by a hundredth.
 *
 * @param value current position in `0..1`; callers map their channel range to this.
 * @param trackColors color stops of the track, evenly spaced from `0` to `1`.
 * @param thumbColor the color the thumb shows; [thumb] reads it opaque, as [ColorSliderScope.thumbColor].
 * @param showCheckerboard draws a transparency checkerboard under the gradient, for
 * gradients with translucent stops (used by [AlphaSlider]).
 * @param semanticLabel accessibility content description of the slider (what channel
 * it controls).
 * @param semanticValueText accessibility state description of the current value: the position as a
 * percentage of the track by default, in the locale's number format. Pass the value in its own units
 * where it has them; `null` omits it.
 * @param colors checkerboard colors; see [ColorPickerDefaults.colors].
 * @param shapes track shape; see [ColorPickerDefaults.shapes].
 * @param dimensions the track's height and the room it leaves for the thumb; see
 * [ColorPickerDefaults.dimensions]. A custom [thumb] wider than [ColorPickerDefaults.ThumbWidth] must
 * say so in [ColorPickerDimensions.thumbWidth], or the track's gap closes and the thumb sits flush
 * against the gradient.
 * @param interactionSource receives the slider's press, drag, focus and hover interactions, which [thumb]
 * reads from [ColorSliderScope.interactionSource]. Note that if `null` is provided, interactions will
 * still happen internally.
 * @param label optional slot shown above the track's start; see [SliderLabel].
 * @param valueLabel optional slot shown above the track's end; see [SliderValueLabel].
 * @param thumb draws the thumb, reading where it is, whether the slider is enabled, its interactions and
 * its opaque color from [ColorSliderScope]; [ColorPickerDefaults.SliderThumb], a rounded bar, by default.
 */
@Composable
public fun ColorSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    trackColors: List<Color>,
    thumbColor: Color,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
    showCheckerboard: Boolean = false,
    semanticLabel: String? = null,
    semanticValueText: String? = ColorPickerStrings.current.sliderPosition(value),
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    dimensions: ColorPickerDimensions = ColorPickerDefaults.currentDimensions(),
    interactionSource: MutableInteractionSource? = null,
    label: (@Composable () -> Unit)? = null,
    valueLabel: (@Composable () -> Unit)? = null,
    // this., or this function's own interactionSource and thumbColor would shadow the scope's: the
    // scope's source is the resolved one, and its color is opaque.
    thumb: @Composable ColorSliderScope.() -> Unit = { ColorPickerDefaults.SliderThumb(this.interactionSource, this.thumbColor) },
) {
    val layoutDirection = LocalLayoutDirection.current
    val gradient = remember(trackColors, layoutDirection) { trackBrush(trackColors, layoutDirection) }
    SliderFrame(modifier, enabled, colors, label, valueLabel) { sliderModifier ->
        BasicColorSlider(
            value = value,
            onValueChange = onValueChange,
            modifier = sliderModifier,
            enabled = enabled,
            thumbColor = thumbColor,
            onValueChangeFinished = onValueChangeFinished,
            semanticLabel = semanticLabel,
            semanticValueText = semanticValueText,
            interactionSource = interactionSource,
            track = { SliderTrack(gradient, colors, shapes, dimensions, showCheckerboard) },
            thumb = thumb,
        )
    }
}

// [colors] evenly spaced from the track's start, mirrored in right-to-left layouts as the slider is.
private fun trackBrush(colors: List<Color>, direction: LayoutDirection): Brush =
    Brush.horizontalGradient(if (direction == LayoutDirection.Rtl) colors.reversed() else colors)

/**
 * The Material frame of [ColorSlider], [ChannelSlider] and [AlphaSlider]: the label row above the
 * slider, the minimum touch size the slider is handed as its modifier, and the disabled look over
 * both.
 */
@Composable
internal fun SliderFrame(
    modifier: Modifier,
    enabled: Boolean,
    colors: ColorPickerColors,
    label: (@Composable () -> Unit)?,
    valueLabel: (@Composable () -> Unit)?,
    slider: @Composable (Modifier) -> Unit,
) {
    val active = enabled && LocalColorPickerEnabled.current

    Column(modifier = modifier.fillMaxWidth().disabledAppearance(active, colors)) {
        if (label != null || valueLabel != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box { label?.invoke() }
                Box { valueLabel?.invoke() }
            }
        }

        slider(
            Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize(),
        )
    }
}

/** The Material track: [gradient] at the dimensions' track height, with its gap at the thumb. */
@Composable
internal fun ColorSliderScope.SliderTrack(
    gradient: Brush,
    colors: ColorPickerColors,
    shapes: ColorPickerShapes,
    dimensions: ColorPickerDimensions,
    showCheckerboard: Boolean = false,
) {
    GradientTrack(
        brush = gradient,
        thumbFraction = fraction,
        interactionSource = interactionSource,
        checkerboardLight = colors.checkerboardLight,
        checkerboardDark = colors.checkerboardDark,
        trackShape = shapes.trackShape,
        showCheckerboard = showCheckerboard,
        thumbWidth = dimensions.thumbWidth,
        thumbTrackGap = dimensions.thumbTrackGap,
        modifier = Modifier
            .fillMaxWidth()
            .height(dimensions.trackHeight),
    )
}
