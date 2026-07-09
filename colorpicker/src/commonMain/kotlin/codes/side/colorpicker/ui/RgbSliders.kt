package codes.side.colorpicker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import codes.side.colorpicker.conversion.toComposeColor
import codes.side.colorpicker.model.RgbColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import kotlinx.collections.immutable.persistentListOf

@Composable
fun RedSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Contextual,
    label: (@Composable () -> Unit)? = { SliderLabel("Red") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.rgbColor.intRed}") },
) {
    val rgb = state.rgbColor
    val gradientColors = remember(rgb.green, rgb.blue, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> persistentListOf(Color.Black, Color.Red)
            ColoringMode.Contextual -> persistentListOf(
                RgbColor(red = 0f, green = rgb.green, blue = rgb.blue).toComposeColor(),
                RgbColor(red = 1f, green = rgb.green, blue = rgb.blue).toComposeColor(),
            )
        }
    }
    val thumbColor = remember(rgb, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> RgbColor(red = rgb.red, green = 0f, blue = 0f).toComposeColor()
            ColoringMode.Contextual -> rgb.toComposeColor()
        }
    }

    ColorSlider(
        value = rgb.red,
        onValueChange = { state.updateFromRgb(rgb.copy(red = it.coerceIn(0f, 1f))) },
        gradientColors = gradientColors,
        thumbColor = thumbColor,
        label = label,
        valueLabel = valueLabel,
        modifier = modifier,
        onValueChangeFinished = { state.isInteracting = false },
    )
}

@Composable
fun GreenSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Contextual,
    label: (@Composable () -> Unit)? = { SliderLabel("Green") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.rgbColor.intGreen}") },
) {
    val rgb = state.rgbColor
    val gradientColors = remember(rgb.red, rgb.blue, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> persistentListOf(Color.Black, Color.Green)
            ColoringMode.Contextual -> persistentListOf(
                RgbColor(red = rgb.red, green = 0f, blue = rgb.blue).toComposeColor(),
                RgbColor(red = rgb.red, green = 1f, blue = rgb.blue).toComposeColor(),
            )
        }
    }
    val thumbColor = remember(rgb, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> RgbColor(red = 0f, green = rgb.green, blue = 0f).toComposeColor()
            ColoringMode.Contextual -> rgb.toComposeColor()
        }
    }

    ColorSlider(
        value = rgb.green,
        onValueChange = { state.updateFromRgb(rgb.copy(green = it.coerceIn(0f, 1f))) },
        gradientColors = gradientColors,
        thumbColor = thumbColor,
        label = label,
        valueLabel = valueLabel,
        modifier = modifier,
        onValueChangeFinished = { state.isInteracting = false },
    )
}

@Composable
fun BlueSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Contextual,
    label: (@Composable () -> Unit)? = { SliderLabel("Blue") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.rgbColor.intBlue}") },
) {
    val rgb = state.rgbColor
    val gradientColors = remember(rgb.red, rgb.green, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> persistentListOf(Color.Black, Color.Blue)
            ColoringMode.Contextual -> persistentListOf(
                RgbColor(red = rgb.red, green = rgb.green, blue = 0f).toComposeColor(),
                RgbColor(red = rgb.red, green = rgb.green, blue = 1f).toComposeColor(),
            )
        }
    }
    val thumbColor = remember(rgb, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> RgbColor(red = 0f, green = 0f, blue = rgb.blue).toComposeColor()
            ColoringMode.Contextual -> rgb.toComposeColor()
        }
    }

    ColorSlider(
        value = rgb.blue,
        onValueChange = { state.updateFromRgb(rgb.copy(blue = it.coerceIn(0f, 1f))) },
        gradientColors = gradientColors,
        thumbColor = thumbColor,
        label = label,
        valueLabel = valueLabel,
        modifier = modifier,
        onValueChangeFinished = { state.isInteracting = false },
    )
}
