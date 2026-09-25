package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import codes.side.color.AnalogousCategory
import codes.side.color.ColorChannel
import codes.side.color.ColorSpace
import codes.side.color.Okhsl
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes
import codes.side.colorpicker.theme.ColorPickerTheme

/** Whether a picker over [space] shows a plane: [space] has one hue and two other channels. */
internal fun hasPlane(space: ColorSpace): Boolean = space.channels.size == 3 && space.channels.count { it.isHue } == 1

/**
 * The channels a picker's plane over [space] shows, or null for a space with no plane: across, the
 * channel tagged as colorfulness, or else the first that is not a hue; up, the other that is not a hue.
 */
internal fun planeAxes(space: ColorSpace): Pair<ColorChannel, ColorChannel>? {
    if (!hasPlane(space)) return null
    val others = space.channels.filter { !it.isHue }
    val x = others.firstOrNull { it.analogous == AnalogousCategory.Colorfulness } ?: others.first()
    return x to others.first { it !== x }
}

// A picker's plane, width over height.
private const val PLANE_ASPECT_RATIO = 1.6f

/** The plane a picker over [space] draws unless given another: a [ChannelPlane] over [planeAxes]. */
internal fun defaultPlane(
    space: ColorSpace,
    enabled: Boolean,
    onValueChangeFinished: () -> Unit,
): @Composable (ColorPickerState) -> Unit = { state ->
    val axes = planeAxes(space)
    if (axes != null) {
        ChannelPlane(
            state,
            axes.first,
            axes.second,
            Modifier.fillMaxWidth().aspectRatio(PLANE_ASPECT_RATIO),
            enabled = enabled,
            onValueChangeFinished = onValueChangeFinished,
        )
    }
}

/** The slider a picker draws for each channel unless given another: a [ChannelSlider]. */
internal fun defaultChannelSlider(
    enabled: Boolean,
    coloringMode: ColoringMode,
    onValueChangeFinished: () -> Unit,
    thumb: (@Composable (InteractionSource) -> Unit)?,
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

/** The alpha slider a picker draws unless given another: an [AlphaSlider]. */
internal fun defaultAlphaSlider(
    enabled: Boolean,
    onValueChangeFinished: () -> Unit,
    thumb: (@Composable (InteractionSource) -> Unit)?,
): @Composable (ColorPickerState) -> Unit = { state ->
    AlphaSlider(state, enabled = enabled, onValueChangeFinished = onValueChangeFinished, thumb = thumb)
}

/**
 * A complete picker for [space], editing [state]: a plane over two of its channels when it has one hue
 * and two other channels, a slider per channel in channel order, and an alpha slider. Moving a channel
 * leaves the color in [space].
 *
 * Each part is a slot. A replacement inherits this picker's colors, shapes and dimensions through
 * [ColorPickerTheme], and is refused input while [enabled] is false even if it was never given
 * [enabled] itself.
 *
 * @param space the space whose channels the picker shows. Okhsl by default: its lightness is perceived
 * lightness, and its saturation is measured against the display, so every position is a color the
 * screen can show.
 * @param showPlane whether to draw [plane]; by default, when [space] has one hue and two other channels.
 * @param showAlpha whether to draw [alphaSlider].
 * @param enabled when false the picker is dimmed, refuses input and reports itself disabled.
 * @param coloringMode how the channel tracks are drawn: [ColoringMode.Independent] by default for a
 * space with a hue, so a hue track stays a full spectrum, else [ColoringMode.Contextual].
 * @param onValueChangeFinished called when a drag ends, and after each key press or accessibility step.
 * @param colors checkerboard and disabled colors; see [ColorPickerDefaults.colors].
 * @param shapes track and plane shapes; see [ColorPickerDefaults.shapes].
 * @param thumb optional replacement for every slider's thumb; see [ColorSlider].
 * @param plane slot for the plane; by default a [ChannelPlane] across [space]'s colorfulness channel
 * and up its other one: HSL's S × L, HSV's S × V, HWB's W × B, LCH's and OkLCh's C × L.
 * @param channelSlider slot for each channel's slider, given the state and the channel; [ChannelSlider]
 * by default.
 * @param alphaSlider slot for the alpha slider; [AlphaSlider] by default.
 */
@Composable
public fun ColorPicker(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    space: ColorSpace = Okhsl,
    showPlane: Boolean = hasPlane(space),
    showAlpha: Boolean = true,
    enabled: Boolean = true,
    coloringMode: ColoringMode = defaultColoringMode(space),
    onValueChangeFinished: () -> Unit = {},
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    plane: @Composable (ColorPickerState) -> Unit = defaultPlane(space, enabled, onValueChangeFinished),
    channelSlider: @Composable (ColorPickerState, ColorChannel) -> Unit = defaultChannelSlider(enabled, coloringMode, onValueChangeFinished, thumb),
    alphaSlider: @Composable (ColorPickerState) -> Unit = defaultAlphaSlider(enabled, onValueChangeFinished, thumb),
) {
    // Provided rather than passed down, so a replaced slot inherits the picker's theme without the call
    // site forwarding it.
    ColorPickerTheme(colors = colors, shapes = shapes) {
        Column(
            modifier = modifier.disabledInput(enabled),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (showPlane) plane(state)
            for (channel in space.channels) key(channel) { channelSlider(state, channel) }
            if (showAlpha) alphaSlider(state)
        }
    }
}
