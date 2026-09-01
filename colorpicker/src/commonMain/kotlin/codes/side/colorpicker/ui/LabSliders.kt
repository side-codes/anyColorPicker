package codes.side.colorpicker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import codes.side.colorpicker.conversion.toComposeColor
import codes.side.colorpicker.model.LabColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes
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

/**
 * Slider for the CIELAB lightness (L*) channel of [state], in `0..100`.
 *
 * @param coloringMode with [ColoringMode.Contextual] (the default) the track previews
 * the resulting color at the current a* and b*; with [ColoringMode.Independent] it
 * shows the neutral gray ramp (a = 0, b = 0).
 * @param semanticLabel accessibility description of the slider; pass a localized string
 * to replace the English default, or `null` to omit.
 * @param semanticValueText accessibility announcement of the current value (`0..100`).
 */
@Composable
public fun LightnessLabSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Contextual,
    label: (@Composable () -> Unit)? = { SliderLabel("L") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.labColor.intL}") },
    semanticLabel: String? = "L*",
    semanticValueText: String? = "${state.labColor.intL}",
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
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

    val interaction = remember(state) { SliderInteractionGuard(state) }
    ColorSlider(
        value = lab.l / 100f,
        onValueChange = {
            interaction.begin()
            state.updateLabLightness(it * 100f)
        },
        gradientColors = gradientColors,
        thumbColor = thumbColor,
        label = label,
        valueLabel = valueLabel,
        semanticLabel = semanticLabel,
        semanticValueText = semanticValueText,
        colors = colors,
        shapes = shapes,
        modifier = modifier,
        onValueChangeFinished = { interaction.end() },
    )
}

/**
 * Slider for the CIELAB a* (green-red) axis of [state], in `-128..127`.
 *
 * @param coloringMode with [ColoringMode.Contextual] (the default) the track previews
 * the resulting color at the current L* and b*; with [ColoringMode.Independent] it is
 * rendered at mid lightness with b = 0.
 * @param semanticLabel accessibility description of the slider; pass a localized string
 * to replace the English default, or `null` to omit.
 * @param semanticValueText accessibility announcement of the current value (`-128..127`).
 */
@Composable
public fun LabASlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Contextual,
    label: (@Composable () -> Unit)? = { SliderLabel("a") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.labColor.intA}") },
    semanticLabel: String? = "a*",
    semanticValueText: String? = "${state.labColor.intA}",
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
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

    val interaction = remember(state) { SliderInteractionGuard(state) }
    ColorSlider(
        value = (lab.a + 128f) / 255f,
        onValueChange = {
            interaction.begin()
            state.updateLabA(it * 255f - 128f)
        },
        gradientColors = gradientColors,
        thumbColor = thumbColor,
        label = label,
        valueLabel = valueLabel,
        semanticLabel = semanticLabel,
        semanticValueText = semanticValueText,
        colors = colors,
        shapes = shapes,
        modifier = modifier,
        onValueChangeFinished = { interaction.end() },
    )
}

/**
 * Slider for the CIELAB b* (blue-yellow) axis of [state], in `-128..127`.
 *
 * @param coloringMode with [ColoringMode.Contextual] (the default) the track previews
 * the resulting color at the current L* and a*; with [ColoringMode.Independent] it is
 * rendered at mid lightness with a = 0.
 * @param semanticLabel accessibility description of the slider; pass a localized string
 * to replace the English default, or `null` to omit.
 * @param semanticValueText accessibility announcement of the current value (`-128..127`).
 */
@Composable
public fun LabBSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Contextual,
    label: (@Composable () -> Unit)? = { SliderLabel("b") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.labColor.intB}") },
    semanticLabel: String? = "b*",
    semanticValueText: String? = "${state.labColor.intB}",
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
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

    val interaction = remember(state) { SliderInteractionGuard(state) }
    ColorSlider(
        value = (lab.b + 128f) / 255f,
        onValueChange = {
            interaction.begin()
            state.updateLabB(it * 255f - 128f)
        },
        gradientColors = gradientColors,
        thumbColor = thumbColor,
        label = label,
        valueLabel = valueLabel,
        semanticLabel = semanticLabel,
        semanticValueText = semanticValueText,
        colors = colors,
        shapes = shapes,
        modifier = modifier,
        onValueChangeFinished = { interaction.end() },
    )
}
