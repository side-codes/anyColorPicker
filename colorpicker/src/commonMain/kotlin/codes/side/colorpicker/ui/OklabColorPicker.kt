package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import codes.side.color.ColorChannel
import codes.side.color.ColorValue
import codes.side.color.Oklab
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes

/**
 * [ColorPicker] for [Oklab], over [state]: lightness, a and b sliders and alpha, contextual by default.
 * The parameters are [ColorPicker]'s.
 */
@Composable
public fun OklabColorPicker(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    showPlane: Boolean = hasPlane(Oklab),
    showAlpha: Boolean = true,
    enabled: Boolean = true,
    coloringMode: ColoringMode = defaultColoringMode(Oklab),
    onValueChangeFinished: () -> Unit = {},
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    plane: @Composable (ColorPickerState) -> Unit = defaultPlane(Oklab, enabled, onValueChangeFinished),
    channelSlider: @Composable (ColorPickerState, ColorChannel) -> Unit = defaultChannelSlider(enabled, coloringMode, onValueChangeFinished, thumb),
    alphaSlider: @Composable (ColorPickerState) -> Unit = defaultAlphaSlider(enabled, onValueChangeFinished, thumb),
): Unit = ColorPicker(
    state = state,
    modifier = modifier,
    space = Oklab,
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

/** [OklabColorPicker] over a value the caller holds, reported in [Oklab]; controlled as [ColorPicker]'s value form is. */
@Composable
public fun OklabColorPicker(
    value: ColorValue,
    onValueChange: (ColorValue) -> Unit,
    modifier: Modifier = Modifier,
    showPlane: Boolean = hasPlane(Oklab),
    showAlpha: Boolean = true,
    enabled: Boolean = true,
    coloringMode: ColoringMode = defaultColoringMode(Oklab),
    onValueChangeFinished: () -> Unit = {},
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    plane: @Composable (ColorPickerState) -> Unit = defaultPlane(Oklab, enabled, onValueChangeFinished),
    channelSlider: @Composable (ColorPickerState, ColorChannel) -> Unit = defaultChannelSlider(enabled, coloringMode, onValueChangeFinished, thumb),
    alphaSlider: @Composable (ColorPickerState) -> Unit = defaultAlphaSlider(enabled, onValueChangeFinished, thumb),
): Unit = ColorPicker(
    value = value,
    onValueChange = onValueChange,
    modifier = modifier,
    space = Oklab,
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

/** [OklabColorPicker] over a Compose [Color] the caller holds; controlled as [ColorPicker]'s color form is. */
@Composable
public fun OklabColorPicker(
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    showPlane: Boolean = hasPlane(Oklab),
    showAlpha: Boolean = true,
    enabled: Boolean = true,
    coloringMode: ColoringMode = defaultColoringMode(Oklab),
    onValueChangeFinished: () -> Unit = {},
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    plane: @Composable (ColorPickerState) -> Unit = defaultPlane(Oklab, enabled, onValueChangeFinished),
    channelSlider: @Composable (ColorPickerState, ColorChannel) -> Unit = defaultChannelSlider(enabled, coloringMode, onValueChangeFinished, thumb),
    alphaSlider: @Composable (ColorPickerState) -> Unit = defaultAlphaSlider(enabled, onValueChangeFinished, thumb),
): Unit = ColorPicker(
    color = color,
    onColorChange = onColorChange,
    modifier = modifier,
    space = Oklab,
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
