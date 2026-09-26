package codes.side.colorpicker.foundation

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import codes.side.color.ColorChannel
import codes.side.color.ColorSpace
import codes.side.color.ColorValue
import codes.side.color.compose.toColorValue
import codes.side.color.compose.toComposeColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.ui.LocalPickerEnabled
import codes.side.colorpicker.ui.disabledInput
import codes.side.colorpicker.ui.planeAxes
import codes.side.colorpicker.ui.rememberControlledPickerState

/**
 * A complete picker for [space] over [state], laying out the parts it is given: [plane] over two of the
 * space's channels when it has one hue and two other channels, [channelSlider] for each channel in
 * channel order, and [alphaSlider]. It draws nothing itself.
 *
 * [plane] is handed its axes: across, the channel tagged as colorfulness, or else the first that is not
 * a hue; up, the other that is not a hue. For HSL that is S × L, for HSV S × V, for HWB W × B, for LCH
 * and OkLCh C × L.
 *
 * While [enabled] is false a part is refused pointer input even if it was never given [enabled]
 * itself, and the library's sliders and planes inside it are disabled outright: deaf to the keyboard
 * and a screen reader too, and reporting themselves disabled to their slots.
 *
 * @param plane draws the plane; `null` leaves it out, and a space without a plane never calls it.
 * @param channelSlider draws each channel's slider, given the state and the channel.
 * @param alphaSlider draws the alpha slider; `null` leaves it out.
 * @param orientation [Orientation.Vertical] stacks the plane, the channel sliders and alpha;
 * [Orientation.Horizontal] puts the plane in the start half and the sliders and alpha in the end
 * half, and gives the sliders the whole width when there is no plane.
 * @param spacing the space between the parts.
 */
@Composable
public fun BasicColorPicker(
    state: ColorPickerState,
    space: ColorSpace,
    plane: (@Composable (ColorPickerState, x: ColorChannel, y: ColorChannel) -> Unit)?,
    channelSlider: @Composable (ColorPickerState, ColorChannel) -> Unit,
    alphaSlider: (@Composable (ColorPickerState) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Vertical,
    spacing: Dp = 0.dp,
) {
    val axes = if (plane != null) planeAxes(space) else null
    // Provided rather than passed down, so a replaced part is disabled with the picker without the call
    // site forwarding it.
    CompositionLocalProvider(LocalPickerEnabled provides (enabled && LocalPickerEnabled.current)) {
        when (orientation) {
            Orientation.Vertical -> Column(
                modifier = modifier.disabledInput(enabled),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                if (plane != null && axes != null) plane(state, axes.first, axes.second)
                PickerSliders(state, space, channelSlider, alphaSlider)
            }
            Orientation.Horizontal -> Row(
                modifier = modifier.disabledInput(enabled),
                horizontalArrangement = Arrangement.spacedBy(spacing),
            ) {
                if (plane != null && axes != null) {
                    Box(Modifier.weight(1f)) { plane(state, axes.first, axes.second) }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing)) {
                    PickerSliders(state, space, channelSlider, alphaSlider)
                }
            }
        }
    }
}

/**
 * [BasicColorPicker] over a value the caller holds, fully controlled. Every change the user makes
 * reaches [onValueChange] in the same event, as the whole new value: in [space], or in the value's own
 * space when only alpha changed, so opacity never pulls a color [space] cannot hold to its edge. The
 * picker draws only [value], and a value passed in is never reported back.
 *
 * Update your value in the callback. A caller that ignores it holds the picker still, and one that
 * clamps or rounds shows the clamp or the rounding at once. A value that arrives late is drawn when it
 * arrives, and a drag carries on from the finger.
 *
 * When [value] is the one the picker last reported, the picker keeps that exact value. Any other value
 * becomes the picker's; a grey arriving without a hue keeps the hue last chosen. The remembered hues are
 * saved across configuration changes.
 *
 * The remaining parameters are the state form's.
 */
@Composable
public fun BasicColorPicker(
    value: ColorValue,
    onValueChange: (ColorValue) -> Unit,
    space: ColorSpace,
    plane: (@Composable (ColorPickerState, x: ColorChannel, y: ColorChannel) -> Unit)?,
    channelSlider: @Composable (ColorPickerState, ColorChannel) -> Unit,
    alphaSlider: (@Composable (ColorPickerState) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Vertical,
    spacing: Dp = 0.dp,
) {
    val state = rememberControlledPickerState(value, space, onValueChange, toValue = { it }, fromValue = { it })
    BasicColorPicker(state, space, plane, channelSlider, alphaSlider, modifier, enabled, orientation, spacing)
}

/**
 * [BasicColorPicker] over a Compose [Color] the caller holds, fully controlled as the [ColorValue] form
 * is. [onColorChange] receives each change as `toComposeColor()`: brought into sRGB by CSS gamut
 * mapping, eight bits a channel. The picker keeps the exact value behind the last color it reported,
 * so it never steps through 8-bit sRGB itself.
 *
 * The remaining parameters are the state form's.
 *
 * @throws IllegalArgumentException for [Color.Unspecified] and Compose's HDR spaces, which
 * [toColorValue] refuses.
 */
@Composable
public fun BasicColorPicker(
    color: Color,
    onColorChange: (Color) -> Unit,
    space: ColorSpace,
    plane: (@Composable (ColorPickerState, x: ColorChannel, y: ColorChannel) -> Unit)?,
    channelSlider: @Composable (ColorPickerState, ColorChannel) -> Unit,
    alphaSlider: (@Composable (ColorPickerState) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Vertical,
    spacing: Dp = 0.dp,
) {
    val state = rememberControlledPickerState(
        color,
        space,
        onColorChange,
        toValue = { it.toColorValue() },
        fromValue = { it.toComposeColor() },
    )
    BasicColorPicker(state, space, plane, channelSlider, alphaSlider, modifier, enabled, orientation, spacing)
}

// A slider for each of [space]'s channels in channel order, then alpha.
@Composable
private fun PickerSliders(
    state: ColorPickerState,
    space: ColorSpace,
    channelSlider: @Composable (ColorPickerState, ColorChannel) -> Unit,
    alphaSlider: (@Composable (ColorPickerState) -> Unit)?,
) {
    for (channel in space.channels) key(channel) { channelSlider(state, channel) }
    alphaSlider?.invoke(state)
}
