package codes.side.colorpicker.material3

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import codes.side.color.ColorValue
import codes.side.color.Okhsl
import codes.side.colorpicker.foundation.AlphaSliderPart
import codes.side.colorpicker.foundation.ChannelSliderPart
import codes.side.colorpicker.foundation.ColorSliderScope
import codes.side.colorpicker.foundation.ColoringMode
import codes.side.colorpicker.foundation.PlanePart
import codes.side.colorpicker.state.ColorPickerState

/**
 * [ColorPicker] for [Okhsl], over [state]: a saturation × lightness plane, hue, saturation and lightness
 * sliders, and alpha. Lightness is perceived lightness, and saturation is measured against sRGB, so
 * every position is a color the screen shows. The parameters are [ColorPicker]'s.
 */
@Composable
public fun OkhslColorPicker(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Vertical,
    coloringMode: ColoringMode = ColoringMode.defaultFor(Okhsl),
    onValueChangeFinished: () -> Unit = {},
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    dimensions: ColorPickerDimensions = ColorPickerDefaults.currentDimensions(),
    thumb: @Composable ColorSliderScope.() -> Unit = { ColorPickerDefaults.SliderThumb(interactionSource, thumbColor) },
    plane: (@Composable (PlanePart) -> Unit)? = { ColorPickerDefaults.Plane(it) },
    channelSlider: @Composable (ChannelSliderPart) -> Unit = { ChannelSlider(it.state, it.channel) },
    alphaSlider: (@Composable (AlphaSliderPart) -> Unit)? = { AlphaSlider(it.state) },
): Unit = ColorPicker(
    state = state,
    modifier = modifier,
    space = Okhsl,
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

/** [OkhslColorPicker] over a value the caller holds, reported in [Okhsl]; controlled as [ColorPicker]'s value form is. */
@Composable
public fun OkhslColorPicker(
    value: ColorValue,
    onValueChange: (ColorValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Vertical,
    coloringMode: ColoringMode = ColoringMode.defaultFor(Okhsl),
    onValueChangeFinished: () -> Unit = {},
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    dimensions: ColorPickerDimensions = ColorPickerDefaults.currentDimensions(),
    thumb: @Composable ColorSliderScope.() -> Unit = { ColorPickerDefaults.SliderThumb(interactionSource, thumbColor) },
    plane: (@Composable (PlanePart) -> Unit)? = { ColorPickerDefaults.Plane(it) },
    channelSlider: @Composable (ChannelSliderPart) -> Unit = { ChannelSlider(it.state, it.channel) },
    alphaSlider: (@Composable (AlphaSliderPart) -> Unit)? = { AlphaSlider(it.state) },
): Unit = ColorPicker(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    space = Okhsl,
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

/** [OkhslColorPicker] over a Compose [Color] the caller holds; controlled as [ColorPicker]'s color form is. */
@Composable
public fun OkhslColorPicker(
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Vertical,
    coloringMode: ColoringMode = ColoringMode.defaultFor(Okhsl),
    onValueChangeFinished: () -> Unit = {},
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    dimensions: ColorPickerDimensions = ColorPickerDefaults.currentDimensions(),
    thumb: @Composable ColorSliderScope.() -> Unit = { ColorPickerDefaults.SliderThumb(interactionSource, thumbColor) },
    plane: (@Composable (PlanePart) -> Unit)? = { ColorPickerDefaults.Plane(it) },
    channelSlider: @Composable (ChannelSliderPart) -> Unit = { ChannelSlider(it.state, it.channel) },
    alphaSlider: (@Composable (AlphaSliderPart) -> Unit)? = { AlphaSlider(it.state) },
): Unit = ColorPicker(
    color = color,
    onColorChange = onColorChange,
    modifier = modifier,
    space = Okhsl,
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
