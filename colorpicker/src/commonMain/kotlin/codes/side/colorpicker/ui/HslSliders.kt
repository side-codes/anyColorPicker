package codes.side.colorpicker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import codes.side.colorpicker.conversion.toComposeColor
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

private val HUE_RAINBOW = persistentListOf(
    Color.Red,
    Color.Yellow,
    Color.Green,
    Color.Cyan,
    Color.Blue,
    Color.Magenta,
    Color.Red,
)

@Composable
fun HueSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Independent,
    label: (@Composable () -> Unit)? = { SliderLabel("Hue") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.hslColor.intHue}°") },
) {
    val hsl = state.hslColor
    val gradientColors = remember(hsl.saturation, hsl.lightness, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> HUE_RAINBOW
            ColoringMode.Contextual -> buildHueGradient(hsl.saturation, hsl.lightness)
        }
    }
    val thumbColor = remember(hsl, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> HslColor(hue = hsl.hue, saturation = 1f, lightness = 0.5f).toComposeColor()
            ColoringMode.Contextual -> hsl.toComposeColor()
        }
    }

    ColorSlider(
        value = hsl.hue / 360f,
        onValueChange = { state.updateHue(it * 360f) },
        gradientColors = gradientColors,
        thumbColor = thumbColor,
        label = label,
        valueLabel = valueLabel,
        modifier = modifier,
        onValueChangeFinished = { state.isInteracting = false },
    )
}

@Composable
fun SaturationSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Independent,
    label: (@Composable () -> Unit)? = { SliderLabel("Saturation") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.hslColor.intSaturation}%") },
) {
    val hsl = state.hslColor
    val gradientColors = remember(hsl.hue, hsl.lightness, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> persistentListOf(
                Color.Gray,
                HslColor(hue = hsl.hue, saturation = 1f, lightness = 0.5f).toComposeColor(),
            )
            ColoringMode.Contextual -> persistentListOf(
                HslColor(hue = hsl.hue, saturation = 0f, lightness = hsl.lightness).toComposeColor(),
                HslColor(hue = hsl.hue, saturation = 1f, lightness = hsl.lightness).toComposeColor(),
            )
        }
    }
    val thumbColor = remember(hsl, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> HslColor(hue = hsl.hue, saturation = hsl.saturation, lightness = 0.5f).toComposeColor()
            ColoringMode.Contextual -> hsl.toComposeColor()
        }
    }

    ColorSlider(
        value = hsl.saturation,
        onValueChange = { state.updateSaturation(it) },
        gradientColors = gradientColors,
        thumbColor = thumbColor,
        label = label,
        valueLabel = valueLabel,
        modifier = modifier,
        onValueChangeFinished = { state.isInteracting = false },
    )
}

@Composable
fun LightnessSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Independent,
    label: (@Composable () -> Unit)? = { SliderLabel("Lightness") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.hslColor.intLightness}%") },
) {
    val hsl = state.hslColor
    val gradientColors = remember(hsl.hue, hsl.saturation, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> persistentListOf(
                Color.Black,
                HslColor(hue = hsl.hue, saturation = 1f, lightness = 0.5f).toComposeColor(),
                Color.White,
            )
            ColoringMode.Contextual -> persistentListOf(
                Color.Black,
                HslColor(hue = hsl.hue, saturation = hsl.saturation, lightness = 0.5f).toComposeColor(),
                Color.White,
            )
        }
    }
    val thumbColor = remember(hsl, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> HslColor(hue = hsl.hue, saturation = 1f, lightness = hsl.lightness).toComposeColor()
            ColoringMode.Contextual -> hsl.toComposeColor()
        }
    }

    ColorSlider(
        value = hsl.lightness,
        onValueChange = { state.updateLightness(it) },
        gradientColors = gradientColors,
        thumbColor = thumbColor,
        label = label,
        valueLabel = valueLabel,
        modifier = modifier,
        onValueChangeFinished = { state.isInteracting = false },
    )
}

private fun buildHueGradient(saturation: Float, lightness: Float): ImmutableList<Color> {
    val steps = 7
    return (0..steps).map { i ->
        val hue = (i * 360f / steps).coerceAtMost(360f)
        HslColor(hue = hue, saturation = saturation, lightness = lightness).toComposeColor()
    }.toImmutableList()
}
