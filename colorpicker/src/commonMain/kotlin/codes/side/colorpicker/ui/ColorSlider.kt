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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import codes.side.color.ColorChannel
import codes.side.colorpicker.foundation.BasicColorSlider
import codes.side.colorpicker.foundation.BasicColorSliderImpl
import codes.side.colorpicker.foundation.ColorPickerStrings
import codes.side.colorpicker.foundation.rememberFractionSteps
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes
import kotlinx.collections.immutable.ImmutableList

// How far a key moves this slider, a hundredth of the track and a tenth for a page, as
// BasicColorSlider's defaults do.
private const val COLOR_SLIDER_STEP = 0.01f
private const val COLOR_SLIDER_PAGE_STEP = 0.1f

/**
 * Building block for a single-channel color slider: a [BasicColorSlider] with a gradient
 * track, optional label row, and optional transparency checkerboard.
 *
 * [ChannelSlider] and [AlphaSlider] are drawn by the same slider. Use this one for a track
 * no [ColorChannel] describes, such as a value of your own. Left and Right move it by a
 * hundredth and Page Up and Page Down by a tenth, and a screen reader steps by a hundredth.
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
    val stops = remember(gradientColors) { TrackStops(gradientColors, null) }
    ColorSliderImpl(
        value = value,
        onValueChange = onValueChange,
        onStep = rememberFractionSteps(value, COLOR_SLIDER_STEP, COLOR_SLIDER_PAGE_STEP, onValueChange),
        accessibilitySteps = accessibilitySteps(0.0..1.0, COLOR_SLIDER_STEP.toDouble()),
        stops = stops,
        thumbColor = thumbColor,
        modifier = modifier,
        label = label,
        valueLabel = valueLabel,
        onValueChangeFinished = onValueChangeFinished,
        enabled = enabled,
        trackHeight = trackHeight,
        showCheckerboard = showCheckerboard,
        semanticLabel = semanticLabel,
        semanticValueText = semanticValueText,
        colors = colors,
        shapes = shapes,
        interactionSource = interactionSource,
        thumb = thumb,
        thumbWidth = thumbWidth,
        thumbTrackGap = thumbTrackGap,
    )
}

/**
 * The Material look of [ColorSlider], [ChannelSlider] and [AlphaSlider] over [BasicColorSliderImpl]: the
 * label row, the gradient track with its gap at the thumb, the handle, the minimum touch size, and the
 * disabled look. [stops] sit at their own positions; [onStep] and [accessibilitySteps] are
 * [BasicColorSliderImpl]'s.
 */
@Composable
internal fun ColorSliderImpl(
    value: Float,
    onValueChange: (Float) -> Unit,
    onStep: (direction: Int, page: Boolean) -> Boolean,
    accessibilitySteps: Int,
    stops: TrackStops,
    thumbColor: Color,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = null,
    valueLabel: (@Composable () -> Unit)? = null,
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
    trackHeight: Dp = ColorPickerDefaults.currentDimensions().trackHeight,
    showCheckerboard: Boolean = false,
    semanticLabel: String? = null,
    semanticValueText: String? = null,
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    interactionSource: MutableInteractionSource? = null,
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.currentDimensions().thumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.currentDimensions().thumbTrackGap,
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

        BasicColorSliderImpl(
            value = value,
            onValueChange = onValueChange,
            onStep = onStep,
            accessibilitySteps = accessibilitySteps,
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize(),
            enabled = enabled,
            thumbColor = thumbColor,
            onValueChangeFinished = onValueChangeFinished,
            semanticLabel = semanticLabel,
            semanticValueText = semanticValueText,
            interactionSource = interactionSource,
            // The slots read the scope's interactionSource and thumbColor, not this function's parameters
            // of the same names: the scope's source is the resolved one, and its color is opaque.
            track = {
                GradientTrack(
                    stops = stops,
                    thumbFraction = fraction,
                    interactionSource = this.interactionSource,
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
            },
            thumb = {
                if (thumb != null) thumb(this.interactionSource) else SliderThumb(this.interactionSource, this.thumbColor)
            },
        )
    }
}
