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
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes
import kotlinx.collections.immutable.ImmutableList

/**
 * Building block for a single-channel color slider: an M3 [Slider] with a gradient
 * track, optional label row, and optional transparency checkerboard.
 *
 * All built-in channel sliders (hue, red, alpha, ...) are thin wrappers around this;
 * use it directly to build a custom channel.
 *
 * @param value current position in `0..1`; callers map their channel range to this.
 * @param gradientColors color stops of the track gradient, from `0` to `1`.
 * @param label optional slot shown above the track's start; see [SliderLabel].
 * @param valueLabel optional slot shown above the track's end; see [SliderValueLabel].
 * @param showCheckerboard draws a transparency checkerboard under the gradient, for
 * gradients with translucent stops (used by [AlphaSlider]).
 * @param semanticLabel accessibility content description of the slider (what channel
 * it controls); merged with the M3 slider's own progress semantics.
 * @param semanticValueText accessibility state description of the current value, for
 * announcing the channel's native units instead of a raw fraction.
 * @param colors checkerboard colors; see [ColorPickerDefaults.colors].
 * @param shapes track shape; see [ColorPickerDefaults.shapes].
 * @param thumb optional replacement for the slider thumb. `null` keeps the Material 3
 * default thumb tinted with [thumbColor]; pass a composable to control its size, shape
 * and stroke entirely. It receives the slider's [InteractionSource], so a thumb can react
 * to press and drag; the current value and color need no parameter, since the caller
 * already passed them as [value] and [thumbColor].
 * @param thumbWidth how much room the track leaves for the thumb. A custom [thumb] wider
 * than [ColorPickerDefaults.ThumbWidth] must declare its width here, or the track's gap
 * closes and the thumb sits flush against the gradient.
 * @param thumbTrackGap clearance between the thumb and each track end.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
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
    trackHeight: Dp = ColorPickerDefaults.TrackHeight,
    showCheckerboard: Boolean = false,
    semanticLabel: String? = null,
    semanticValueText: String? = null,
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.ThumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.ThumbTrackGap,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val sliderColors = SliderDefaults.colors(
        thumbColor = thumbColor.asOpaqueThumb(),
        activeTrackColor = Color.Transparent,
        inactiveTrackColor = Color.Transparent,
    )

    Column(modifier = modifier.fillMaxWidth()) {
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

        Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier
                .fillMaxWidth()
                // Merged semantics only — M3 Slider's own progress semantics
                // must survive.
                .semantics {
                    semanticLabel?.let { contentDescription = it }
                    semanticValueText?.let { stateDescription = it }
                },
            interactionSource = interactionSource,
            track = {
                GradientTrack(
                    colors = gradientColors,
                    thumbFraction = value,
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
            },
            // The slot takes the InteractionSource rather than M3's SliderState. Two
            // reasons: SliderState is experimental, so naming it here would force an
            // opt-in on everyone calling ColorSlider, and it is not what a thumb is
            // missing. Value and color are already in scope at the call site, captured
            // by the lambda; press and drag state is the only thing a caller cannot
            // reach, because this source is created here.
            thumb = {
                if (thumb != null) thumb(interactionSource)
                else SliderDefaults.Thumb(interactionSource, colors = sliderColors)
            },
            colors = sliderColors,
        )
    }
}

/**
 * A thumb is always painted opaque. One that inherited the color's alpha would vanish
 * exactly when the color became transparent, leaving nothing to grab — and on the alpha
 * slider that is the thumb you need in order to drag back.
 */
private fun Color.asOpaqueThumb(): Color = if (alpha == 1f) this else copy(alpha = 1f)
