package codes.side.colorpicker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import codes.side.colorpicker.conversion.toComposeColor
import codes.side.colorpicker.state.ColorPickerState
import kotlinx.collections.immutable.persistentListOf

@Composable
fun AlphaSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    label: (@Composable () -> Unit)? = { SliderLabel("Alpha") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.hslColor.intAlpha}") },
) {
    val hsl = state.hslColor
    val opaqueColor = remember(hsl.hue, hsl.saturation, hsl.lightness) {
        hsl.copy(alpha = 1f).toComposeColor()
    }
    val thumbColor = remember(hsl) { hsl.toComposeColor() }

    ColorSlider(
        value = hsl.alpha,
        onValueChange = { state.updateAlpha(it) },
        gradientColors = persistentListOf(Color.Transparent, opaqueColor),
        thumbColor = thumbColor,
        label = label,
        valueLabel = valueLabel,
        showCheckerboard = true,
        modifier = modifier,
        onValueChangeFinished = { state.isInteracting = false },
    )
}
