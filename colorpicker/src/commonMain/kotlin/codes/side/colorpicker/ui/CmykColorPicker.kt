package codes.side.colorpicker.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import codes.side.color.Cmyk
import codes.side.color.ColorChannel
import codes.side.color.ColorValue
import codes.side.colorpicker.foundation.ColorSliderScope
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes

/**
 * [ColorPicker] for [Cmyk], over [state]: cyan, magenta, yellow and key sliders and alpha, contextual by
 * default. The naive formula with no color profile: a screen-space parameterization, not ink. The
 * parameters are [ColorPicker]'s.
 */
@Composable
public fun CmykColorPicker(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    showPlane: Boolean = hasPlane(Cmyk),
    showAlpha: Boolean = true,
    enabled: Boolean = true,
    coloringMode: ColoringMode = ColoringMode.defaultFor(Cmyk),
    onValueChangeFinished: () -> Unit = {},
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    thumb: @Composable ColorSliderScope.() -> Unit = { ColorPickerDefaults.SliderThumb(interactionSource, thumbColor) },
    plane: @Composable (ColorPickerState) -> Unit = defaultPlane(Cmyk, enabled, onValueChangeFinished),
    channelSlider: @Composable (ColorPickerState, ColorChannel) -> Unit = defaultChannelSlider(enabled, coloringMode, onValueChangeFinished, thumb),
    alphaSlider: @Composable (ColorPickerState) -> Unit = defaultAlphaSlider(enabled, onValueChangeFinished, thumb),
): Unit = ColorPicker(
    state = state,
    modifier = modifier,
    space = Cmyk,
    showPlane = showPlane,
    showAlpha = showAlpha,
    enabled = enabled,
    coloringMode = coloringMode,
    onValueChangeFinished = onValueChangeFinished,
    colors = colors,
    shapes = shapes,
    thumb = thumb,
    plane = plane,
    channelSlider = channelSlider,
    alphaSlider = alphaSlider,
)

/** [CmykColorPicker] over a value the caller holds, reported in [Cmyk]; controlled as [ColorPicker]'s value form is. */
@Composable
public fun CmykColorPicker(
    value: ColorValue,
    onValueChange: (ColorValue) -> Unit,
    modifier: Modifier = Modifier,
    showPlane: Boolean = hasPlane(Cmyk),
    showAlpha: Boolean = true,
    enabled: Boolean = true,
    coloringMode: ColoringMode = ColoringMode.defaultFor(Cmyk),
    onValueChangeFinished: () -> Unit = {},
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    thumb: @Composable ColorSliderScope.() -> Unit = { ColorPickerDefaults.SliderThumb(interactionSource, thumbColor) },
    plane: @Composable (ColorPickerState) -> Unit = defaultPlane(Cmyk, enabled, onValueChangeFinished),
    channelSlider: @Composable (ColorPickerState, ColorChannel) -> Unit = defaultChannelSlider(enabled, coloringMode, onValueChangeFinished, thumb),
    alphaSlider: @Composable (ColorPickerState) -> Unit = defaultAlphaSlider(enabled, onValueChangeFinished, thumb),
): Unit = ColorPicker(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    space = Cmyk,
    showPlane = showPlane,
    showAlpha = showAlpha,
    enabled = enabled,
    coloringMode = coloringMode,
    onValueChangeFinished = onValueChangeFinished,
    colors = colors,
    shapes = shapes,
    thumb = thumb,
    plane = plane,
    channelSlider = channelSlider,
    alphaSlider = alphaSlider,
)

/** [CmykColorPicker] over a Compose [Color] the caller holds; controlled as [ColorPicker]'s color form is. */
@Composable
public fun CmykColorPicker(
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    showPlane: Boolean = hasPlane(Cmyk),
    showAlpha: Boolean = true,
    enabled: Boolean = true,
    coloringMode: ColoringMode = ColoringMode.defaultFor(Cmyk),
    onValueChangeFinished: () -> Unit = {},
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    thumb: @Composable ColorSliderScope.() -> Unit = { ColorPickerDefaults.SliderThumb(interactionSource, thumbColor) },
    plane: @Composable (ColorPickerState) -> Unit = defaultPlane(Cmyk, enabled, onValueChangeFinished),
    channelSlider: @Composable (ColorPickerState, ColorChannel) -> Unit = defaultChannelSlider(enabled, coloringMode, onValueChangeFinished, thumb),
    alphaSlider: @Composable (ColorPickerState) -> Unit = defaultAlphaSlider(enabled, onValueChangeFinished, thumb),
): Unit = ColorPicker(
    color = color,
    onColorChange = onColorChange,
    modifier = modifier,
    space = Cmyk,
    showPlane = showPlane,
    showAlpha = showAlpha,
    enabled = enabled,
    coloringMode = coloringMode,
    onValueChangeFinished = onValueChangeFinished,
    colors = colors,
    shapes = shapes,
    thumb = thumb,
    plane = plane,
    channelSlider = channelSlider,
    alphaSlider = alphaSlider,
)
