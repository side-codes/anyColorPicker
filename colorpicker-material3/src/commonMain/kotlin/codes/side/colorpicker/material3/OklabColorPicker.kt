package codes.side.colorpicker.material3

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import codes.side.color.ColorValue
import codes.side.color.Oklab
import codes.side.colorpicker.foundation.AlphaSliderPart
import codes.side.colorpicker.foundation.ChannelSliderPart
import codes.side.colorpicker.foundation.ColorSliderScope
import codes.side.colorpicker.foundation.ColoringMode
import codes.side.colorpicker.foundation.PlanePart
import codes.side.colorpicker.state.ColorPickerState

/**
 * [ColorPicker] for [Oklab], over [state]: lightness, a and b sliders and alpha, contextual by default.
 * The parameters are [ColorPicker]'s.
 */
@Composable
public fun OklabColorPicker(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Vertical,
    coloringMode: ColoringMode = ColoringMode.defaultFor(Oklab),
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
    space = Oklab,
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

/** [OklabColorPicker] over a value the caller holds, reported in [Oklab]; controlled as [ColorPicker]'s value form is. */
@Composable
public fun OklabColorPicker(
    value: ColorValue,
    onValueChange: (ColorValue) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Vertical,
    coloringMode: ColoringMode = ColoringMode.defaultFor(Oklab),
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
    space = Oklab,
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

/** [OklabColorPicker] over a Compose [Color] the caller holds; controlled as [ColorPicker]'s color form is. */
@Composable
public fun OklabColorPicker(
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Vertical,
    coloringMode: ColoringMode = ColoringMode.defaultFor(Oklab),
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
    space = Oklab,
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
