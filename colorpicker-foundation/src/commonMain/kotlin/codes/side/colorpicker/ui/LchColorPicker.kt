package codes.side.colorpicker.ui

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import codes.side.color.ColorChannel
import codes.side.color.ColorValue
import codes.side.color.Lch
import codes.side.colorpicker.foundation.ColorSliderScope
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerDimensions
import codes.side.colorpicker.theme.ColorPickerShapes

/**
 * [ColorPicker] for [Lch], over [state]: a chroma × lightness plane, lightness, chroma and hue sliders,
 * and alpha. The parameters are [ColorPicker]'s.
 */
@Composable
public fun LchColorPicker(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Vertical,
    coloringMode: ColoringMode = ColoringMode.defaultFor(Lch),
    onValueChangeFinished: () -> Unit = {},
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    dimensions: ColorPickerDimensions = ColorPickerDefaults.currentDimensions(),
    thumb: @Composable ColorSliderScope.() -> Unit = { ColorPickerDefaults.SliderThumb(interactionSource, thumbColor) },
    plane: (@Composable (ColorPickerState, ColorChannel, ColorChannel) -> Unit)? = ColorPickerDefaults.plane(enabled, onValueChangeFinished),
    channelSlider: @Composable (ColorPickerState, ColorChannel) -> Unit = ColorPickerDefaults.channelSlider(enabled, coloringMode, onValueChangeFinished, thumb),
    alphaSlider: (@Composable (ColorPickerState) -> Unit)? = ColorPickerDefaults.alphaSlider(enabled, onValueChangeFinished, thumb),
): Unit = ColorPicker(
    state = state,
    modifier = modifier,
    space = Lch,
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

/** [LchColorPicker] over a value the caller holds, reported in [Lch]; controlled as [ColorPicker]'s value form is. */
@Composable
public fun LchColorPicker(
    value: ColorValue,
    onValueChange: (ColorValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Vertical,
    coloringMode: ColoringMode = ColoringMode.defaultFor(Lch),
    onValueChangeFinished: () -> Unit = {},
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    dimensions: ColorPickerDimensions = ColorPickerDefaults.currentDimensions(),
    thumb: @Composable ColorSliderScope.() -> Unit = { ColorPickerDefaults.SliderThumb(interactionSource, thumbColor) },
    plane: (@Composable (ColorPickerState, ColorChannel, ColorChannel) -> Unit)? = ColorPickerDefaults.plane(enabled, onValueChangeFinished),
    channelSlider: @Composable (ColorPickerState, ColorChannel) -> Unit = ColorPickerDefaults.channelSlider(enabled, coloringMode, onValueChangeFinished, thumb),
    alphaSlider: (@Composable (ColorPickerState) -> Unit)? = ColorPickerDefaults.alphaSlider(enabled, onValueChangeFinished, thumb),
): Unit = ColorPicker(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    space = Lch,
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

/** [LchColorPicker] over a Compose [Color] the caller holds; controlled as [ColorPicker]'s color form is. */
@Composable
public fun LchColorPicker(
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Vertical,
    coloringMode: ColoringMode = ColoringMode.defaultFor(Lch),
    onValueChangeFinished: () -> Unit = {},
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    dimensions: ColorPickerDimensions = ColorPickerDefaults.currentDimensions(),
    thumb: @Composable ColorSliderScope.() -> Unit = { ColorPickerDefaults.SliderThumb(interactionSource, thumbColor) },
    plane: (@Composable (ColorPickerState, ColorChannel, ColorChannel) -> Unit)? = ColorPickerDefaults.plane(enabled, onValueChangeFinished),
    channelSlider: @Composable (ColorPickerState, ColorChannel) -> Unit = ColorPickerDefaults.channelSlider(enabled, coloringMode, onValueChangeFinished, thumb),
    alphaSlider: (@Composable (ColorPickerState) -> Unit)? = ColorPickerDefaults.alphaSlider(enabled, onValueChangeFinished, thumb),
): Unit = ColorPicker(
    color = color,
    onColorChange = onColorChange,
    modifier = modifier,
    space = Lch,
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
