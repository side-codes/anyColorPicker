package codes.side.colorpicker.ui

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import codes.side.color.AnalogousCategory
import codes.side.color.ColorChannel
import codes.side.color.ColorSpace
import codes.side.color.ColorValue
import codes.side.color.Okhsl
import codes.side.color.compose.toColorValue
import codes.side.color.compose.toComposeColor
import codes.side.colorpicker.foundation.BasicColorPicker
import codes.side.colorpicker.foundation.ColorSliderScope
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerDimensions
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

/**
 * A complete picker for [space], editing [state]: a plane over two of its channels when it has one hue
 * and two other channels, a slider per channel in channel order, and an alpha slider. Moving a channel
 * leaves the color in [space].
 *
 * Each part is a slot. A replacement inherits this picker's colors, shapes and dimensions through
 * [ColorPickerTheme]. While [enabled] is false it is refused pointer input even if it was never given
 * [enabled] itself, and the library's sliders and planes inside it are disabled outright: dimmed, and
 * deaf to the keyboard and a screen reader too.
 *
 * @param space the space whose channels the picker shows. Okhsl by default: its lightness is perceived
 * lightness, and its saturation is measured against the display, so every position is a color the
 * screen can show.
 * @param enabled when false the picker is dimmed, refuses input and reports itself disabled.
 * @param orientation [Orientation.Vertical] stacks the plane, the channel sliders and alpha;
 * [Orientation.Horizontal] puts the plane in the start half and the sliders and alpha in the end half,
 * and gives the sliders the whole width when there is no plane.
 * @param coloringMode how the channel tracks are drawn: [ColoringMode.Independent] by default for a
 * space with a hue, so a hue track stays a full spectrum, else [ColoringMode.Contextual].
 * @param onValueChangeFinished called when a tap or drag ends, and after each key press or accessibility step.
 * @param colors checkerboard and disabled colors; see [ColorPickerDefaults.colors].
 * @param shapes track and plane shapes; see [ColorPickerDefaults.shapes].
 * @param dimensions track and plane sizes; see [ColorPickerDefaults.dimensions].
 * @param thumb draws every slider's thumb from its [ColorSliderScope]; [ColorPickerDefaults.SliderThumb]
 * by default.
 * @param plane slot for the plane, handed the state and its axes: across, the channel that measures
 * colorfulness, and up the other, as HSL's S × L, HSV's S × V, HWB's W × B, LCH's and OkLCh's C × L. A
 * [ChannelPlane] by default; `null` leaves it out, and a space without such a plane never calls it.
 * @param channelSlider slot for each channel's slider, given the state and the channel; [ChannelSlider]
 * by default.
 * @param alphaSlider slot for the alpha slider; [AlphaSlider] by default, and `null` leaves it out.
 */
@Composable
public fun ColorPicker(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    space: ColorSpace = Okhsl,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Vertical,
    coloringMode: ColoringMode = ColoringMode.defaultFor(space),
    onValueChangeFinished: () -> Unit = {},
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    dimensions: ColorPickerDimensions = ColorPickerDefaults.currentDimensions(),
    thumb: @Composable ColorSliderScope.() -> Unit = { ColorPickerDefaults.SliderThumb(interactionSource, thumbColor) },
    plane: (@Composable (ColorPickerState, ColorChannel, ColorChannel) -> Unit)? = ColorPickerDefaults.plane(enabled, onValueChangeFinished),
    channelSlider: @Composable (ColorPickerState, ColorChannel) -> Unit = ColorPickerDefaults.channelSlider(enabled, coloringMode, onValueChangeFinished, thumb),
    alphaSlider: (@Composable (ColorPickerState) -> Unit)? = ColorPickerDefaults.alphaSlider(enabled, onValueChangeFinished, thumb),
) {
    // Provided rather than passed down, so a replaced slot inherits the picker's theme without the call
    // site forwarding it. BasicColorPicker does the same for the picker's disabled state.
    ColorPickerTheme(colors = colors, shapes = shapes, dimensions = dimensions) {
        BasicColorPicker(
            state = state,
            space = space,
            plane = plane,
            channelSlider = channelSlider,
            alphaSlider = alphaSlider,
            modifier = modifier,
            enabled = enabled,
            orientation = orientation,
            spacing = 12.dp,
        )
    }
}

/**
 * [ColorPicker] over a value the caller holds, fully controlled, as Compose's `Slider(value,
 * onValueChange)` is. Every change the user makes reaches [onValueChange] in the same event, as the
 * whole new value: in [space], or in the value's own space when only alpha changed, so opacity never
 * pulls a color [space] cannot hold to its edge. The picker draws only [value], and a value passed in
 * is never reported back.
 *
 * Update your value in the callback. A caller that ignores it holds the picker still, and one that
 * clamps or rounds shows the clamp or the rounding at once. A value that arrives late (debounced, from
 * a store, or through a coroutine) is drawn when it arrives, and a drag carries on from the finger. A
 * caller like that is better served by holding a [ColorPickerState] and observing it.
 *
 * When [value] is the one the picker last reported, the picker keeps that exact value. Any other value
 * becomes the picker's; a grey arriving without a hue keeps the hue last chosen. The remembered hues are
 * saved across configuration changes.
 *
 * The remaining parameters are [ColorPicker]'s.
 */
@Composable
public fun ColorPicker(
    value: ColorValue,
    onValueChange: (ColorValue) -> Unit,
    modifier: Modifier = Modifier,
    space: ColorSpace = Okhsl,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Vertical,
    coloringMode: ColoringMode = ColoringMode.defaultFor(space),
    onValueChangeFinished: () -> Unit = {},
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    dimensions: ColorPickerDimensions = ColorPickerDefaults.currentDimensions(),
    thumb: @Composable ColorSliderScope.() -> Unit = { ColorPickerDefaults.SliderThumb(interactionSource, thumbColor) },
    plane: (@Composable (ColorPickerState, ColorChannel, ColorChannel) -> Unit)? = ColorPickerDefaults.plane(enabled, onValueChangeFinished),
    channelSlider: @Composable (ColorPickerState, ColorChannel) -> Unit = ColorPickerDefaults.channelSlider(enabled, coloringMode, onValueChangeFinished, thumb),
    alphaSlider: (@Composable (ColorPickerState) -> Unit)? = ColorPickerDefaults.alphaSlider(enabled, onValueChangeFinished, thumb),
) {
    val state = rememberControlledPickerState(value, space, onValueChange, toValue = { it }, fromValue = { it })
    ColorPicker(
        state = state,
        modifier = modifier,
        space = space,
        enabled = enabled,
        orientation = orientation,
        coloringMode = coloringMode,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        shapes = shapes,
        dimensions = dimensions,
        thumb = thumb,
        plane = plane,
        channelSlider = channelSlider,
        alphaSlider = alphaSlider,
    )
}

/**
 * [ColorPicker] over a Compose [Color] the caller holds, fully controlled as the [ColorValue] form is.
 * [onColorChange] receives each change as `toComposeColor()`: brought into sRGB by CSS gamut mapping,
 * eight bits a channel.
 *
 * The picker keeps the exact value behind the last color it reported, so it never steps through 8-bit
 * sRGB itself, and an edit outside sRGB stays where the user put it. Hold a [ColorValue] instead to keep
 * wide gamut and `none` on your side too.
 *
 * The remaining parameters are [ColorPicker]'s.
 *
 * @throws IllegalArgumentException for [Color.Unspecified] and Compose's HDR spaces, which
 * [toColorValue] refuses.
 */
@Composable
public fun ColorPicker(
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    space: ColorSpace = Okhsl,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Vertical,
    coloringMode: ColoringMode = ColoringMode.defaultFor(space),
    onValueChangeFinished: () -> Unit = {},
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    dimensions: ColorPickerDimensions = ColorPickerDefaults.currentDimensions(),
    thumb: @Composable ColorSliderScope.() -> Unit = { ColorPickerDefaults.SliderThumb(interactionSource, thumbColor) },
    plane: (@Composable (ColorPickerState, ColorChannel, ColorChannel) -> Unit)? = ColorPickerDefaults.plane(enabled, onValueChangeFinished),
    channelSlider: @Composable (ColorPickerState, ColorChannel) -> Unit = ColorPickerDefaults.channelSlider(enabled, coloringMode, onValueChangeFinished, thumb),
    alphaSlider: (@Composable (ColorPickerState) -> Unit)? = ColorPickerDefaults.alphaSlider(enabled, onValueChangeFinished, thumb),
) {
    val state = rememberControlledPickerState(
        color,
        space,
        onColorChange,
        toValue = { it.toColorValue() },
        fromValue = { it.toComposeColor() },
    )
    ColorPicker(
        state = state,
        modifier = modifier,
        space = space,
        enabled = enabled,
        orientation = orientation,
        coloringMode = coloringMode,
        onValueChangeFinished = onValueChangeFinished,
        colors = colors,
        shapes = shapes,
        dimensions = dimensions,
        thumb = thumb,
        plane = plane,
        channelSlider = channelSlider,
        alphaSlider = alphaSlider,
    )
}
