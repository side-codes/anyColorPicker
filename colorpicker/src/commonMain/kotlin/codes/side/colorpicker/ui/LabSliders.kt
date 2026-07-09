package codes.side.colorpicker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import codes.side.colorpicker.conversion.toComposeColor
import codes.side.colorpicker.model.LabColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import kotlinx.collections.immutable.toImmutableList

// Pre-computed independent (a=0, b=0) gradients
private val LightnessIndependentGradient = (0..10).map { i ->
    LabColor(l = i * 10f, a = 0f, b = 0f).toComposeColor()
}.toImmutableList()

private val AAxisIndependentGradient = (0..10).map { i ->
    val a = -128f + (i * 255f / 10f)
    LabColor(l = 50f, a = a.coerceIn(-128f, 127f), b = 0f).toComposeColor()
}.toImmutableList()

private val BAxisIndependentGradient = (0..10).map { i ->
    val b = -128f + (i * 255f / 10f)
    LabColor(l = 50f, a = 0f, b = b.coerceIn(-128f, 127f)).toComposeColor()
}.toImmutableList()

@Composable
fun LightnessLabSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Contextual,
    label: (@Composable () -> Unit)? = { SliderLabel("L") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.labColor.intL}") },
) {
    val lab = state.labColor
    val gradientColors = remember(lab.a, lab.b, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> LightnessIndependentGradient
            ColoringMode.Contextual -> (0..10).map { i ->
                LabColor(l = i * 10f, a = lab.a, b = lab.b).toComposeColor()
            }.toImmutableList()
        }
    }
    val thumbColor = remember(lab, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> LabColor(l = lab.l, a = 0f, b = 0f).toComposeColor()
            ColoringMode.Contextual -> lab.toComposeColor()
        }
    }

    ColorSlider(
        value = lab.l / 100f,
        onValueChange = { state.updateFromLab(lab.copy(l = (it * 100f).coerceIn(0f, 100f))) },
        gradientColors = gradientColors,
        thumbColor = thumbColor,
        label = label,
        valueLabel = valueLabel,
        modifier = modifier,
        onValueChangeFinished = { state.isInteracting = false },
    )
}

@Composable
fun LabASlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Contextual,
    label: (@Composable () -> Unit)? = { SliderLabel("a") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.labColor.intA}") },
) {
    val lab = state.labColor
    val gradientColors = remember(lab.l, lab.b, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> AAxisIndependentGradient
            ColoringMode.Contextual -> (0..10).map { i ->
                val a = -128f + (i * 255f / 10f)
                LabColor(l = lab.l, a = a.coerceIn(-128f, 127f), b = lab.b).toComposeColor()
            }.toImmutableList()
        }
    }
    val thumbColor = remember(lab, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> LabColor(l = 50f, a = lab.a, b = 0f).toComposeColor()
            ColoringMode.Contextual -> lab.toComposeColor()
        }
    }

    ColorSlider(
        value = (lab.a + 128f) / 255f,
        onValueChange = { state.updateFromLab(lab.copy(a = (it * 255f - 128f).coerceIn(-128f, 127f))) },
        gradientColors = gradientColors,
        thumbColor = thumbColor,
        label = label,
        valueLabel = valueLabel,
        modifier = modifier,
        onValueChangeFinished = { state.isInteracting = false },
    )
}

@Composable
fun LabBSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Contextual,
    label: (@Composable () -> Unit)? = { SliderLabel("b") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.labColor.intB}") },
) {
    val lab = state.labColor
    val gradientColors = remember(lab.l, lab.a, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> BAxisIndependentGradient
            ColoringMode.Contextual -> (0..10).map { i ->
                val b = -128f + (i * 255f / 10f)
                LabColor(l = lab.l, a = lab.a, b = b.coerceIn(-128f, 127f)).toComposeColor()
            }.toImmutableList()
        }
    }
    val thumbColor = remember(lab, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> LabColor(l = 50f, a = 0f, b = lab.b).toComposeColor()
            ColoringMode.Contextual -> lab.toComposeColor()
        }
    }

    ColorSlider(
        value = (lab.b + 128f) / 255f,
        onValueChange = { state.updateFromLab(lab.copy(b = (it * 255f - 128f).coerceIn(-128f, 127f))) },
        gradientColors = gradientColors,
        thumbColor = thumbColor,
        label = label,
        valueLabel = valueLabel,
        modifier = modifier,
        onValueChangeFinished = { state.isInteracting = false },
    )
}
