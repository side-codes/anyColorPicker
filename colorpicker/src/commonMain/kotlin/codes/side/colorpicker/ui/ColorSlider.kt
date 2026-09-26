package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.InteractionSource
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import codes.side.color.ColorChannel
import codes.side.colorpicker.foundation.BasicColorSlider
import codes.side.colorpicker.foundation.ColorPickerStrings
import codes.side.colorpicker.foundation.ColorSliderScope
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes
import kotlinx.collections.immutable.ImmutableList

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
 * @param gradientColors color stops of the track gradient, from `0` to `1`.
 * @param label optional slot shown above the track's start; see [SliderLabel].
 * @param valueLabel optional slot shown above the track's end; see [SliderValueLabel].
 * @param showCheckerboard draws a transparency checkerboard under the gradient, for
 * gradients with translucent stops (used by [AlphaSlider]).
 * @param semanticLabel accessibility content description of the slider (what channel
 * it controls).
 * @param semanticValueText accessibility state description of the current value: the position as a
 * percentage of the track by default, in the locale's number format. Pass the value in its own units
 * where it has them; `null` omits it.
 * @param colors checkerboard colors; see [ColorPickerDefaults.colors].
 * @param shapes track shape; see [ColorPickerDefaults.shapes].
 * @param interactionSource receives the slider's press, drag, focus and hover interactions, and is what
 * [thumb] is handed. Note that if `null` is provided, interactions will still happen internally.
 * @param thumb optional replacement for the slider thumb. `null` keeps the default handle, a
 * rounded bar in [thumbColor]; pass a composable to control its size, shape
 * and stroke entirely. It receives the slider's [InteractionSource], so a thumb can react
 * to press and drag; the current value and color need no parameter, since the caller
 * already passed them as [value] and [thumbColor].
 * @param thumbWidth how much room the track leaves for the thumb. A custom [thumb] wider
 * than [ColorPickerDefaults.ThumbWidth] must declare its width here, or the track's gap
 * closes and the thumb sits flush against the gradient.
 * @param thumbTrackGap clearance between the thumb and each track end.
 */
@Composable
public fun ColorSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    gradientColors: ImmutableList<Color>,
    thumbColor: Color,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    valueLabel: (@Composable () -> Unit)? = null,
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
    trackHeight: Dp = ColorPickerDefaults.currentDimensions().trackHeight,
    showCheckerboard: Boolean = false,
    semanticLabel: String? = null,
    semanticValueText: String? = ColorPickerStrings.current.sliderPosition(value),
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    interactionSource: MutableInteractionSource? = null,
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.currentDimensions().thumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.currentDimensions().thumbTrackGap,
) {
    val layoutDirection = LocalLayoutDirection.current
    val gradient = remember(gradientColors, layoutDirection) { TrackStops(gradientColors, null).brush(layoutDirection) }
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
            track = { SliderTrack(gradient, colors, shapes, thumbWidth, thumbTrackGap, trackHeight, showCheckerboard) },
            thumb = { SliderHandle(thumb) },
        )
    }
}

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
    val active = enabled && LocalPickerEnabled.current

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

/** The Material track: [gradient] at [trackHeight], with its gap at the thumb. */
@Composable
internal fun ColorSliderScope.SliderTrack(
    gradient: Brush,
    colors: ColorPickerColors,
    shapes: ColorPickerShapes,
    thumbWidth: Dp,
    thumbTrackGap: Dp,
    trackHeight: Dp = ColorPickerDefaults.currentDimensions().trackHeight,
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
        thumbWidth = thumbWidth,
        thumbTrackGap = thumbTrackGap,
        modifier = Modifier
            .fillMaxWidth()
            .height(trackHeight),
    )
}

/**
 * A caller's [thumb], or the default handle. Both read the scope's source and color rather than a wrapper's
 * parameters of the same names: the scope's source is the resolved one, and its color is opaque.
 */
@Composable
internal fun ColorSliderScope.SliderHandle(thumb: (@Composable (InteractionSource) -> Unit)?) {
    if (thumb != null) thumb(interactionSource) else SliderThumb(interactionSource, thumbColor)
}
