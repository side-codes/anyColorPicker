package codes.side.colorpicker.ui

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import codes.side.color.ColorChannel
import codes.side.color.ColorValue
import codes.side.color.Lab
import codes.side.colorpicker.foundation.ColorSliderScope
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerDimensions
import codes.side.colorpicker.theme.ColorPickerShapes

/**
 * [ColorPicker] for [Lab], CSS lab() with its D50 white, over [state]: lightness, a and b sliders and
 * alpha, contextual by default. Most of the a–b square lies outside sRGB and draws as the nearest color
 * sRGB holds. The parameters are [ColorPicker]'s.
 */
@Composable
public fun LabColorPicker(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Vertical,
    coloringMode: ColoringMode = ColoringMode.defaultFor(Lab),
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
    space = Lab,
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

/** [LabColorPicker] over a value the caller holds, reported in [Lab]; controlled as [ColorPicker]'s value form is. */
@Composable
public fun LabColorPicker(
    value: ColorValue,
    onValueChange: (ColorValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Vertical,
    coloringMode: ColoringMode = ColoringMode.defaultFor(Lab),
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
    space = Lab,
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

/** [LabColorPicker] over a Compose [Color] the caller holds; controlled as [ColorPicker]'s color form is. */
@Composable
public fun LabColorPicker(
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Vertical,
    coloringMode: ColoringMode = ColoringMode.defaultFor(Lab),
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
    space = Lab,
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
