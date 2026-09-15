package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import codes.side.colorpicker.conversion.toComposeColor
import codes.side.colorpicker.model.LabColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Stops for the L*, a* and b* tracks.
 *
 * HSL's hue track has breakpoints to land on and the Ok* tracks stay inside the gamut,
 * but a LAB track has neither going for it: over half of an a* or b* sweep is outside
 * sRGB, and out there the mapped color runs along the gamut surface, turning a corner at
 * every edge of the RGB cube it crosses. Stops cannot follow a corner, so the error
 * flattens out instead of falling — the Independent a* track measures 36 of 255 at 10
 * stops, 7.4 at 64 and still 5.5 at 128. 64 is where the return goes flat, and it is what
 * the Ok* tracks already use.
 */
internal const val LAB_CHANNEL_STOPS = 64

/** The a* or b* value a track reaches at [fraction] of its travel; both run `-128..127`. */
internal fun labAxisFromFraction(fraction: Float): Float =
    (-128f + fraction * 255f).coerceIn(-128f, 127f)

internal fun buildLabLightnessGradient(a: Float, b: Float): ImmutableList<Color> =
    (0..LAB_CHANNEL_STOPS).map { i ->
        LabColor(l = i * 100f / LAB_CHANNEL_STOPS, a = a, b = b).toComposeColor()
    }.toImmutableList()

internal fun buildLabAGradient(l: Float, b: Float): ImmutableList<Color> =
    (0..LAB_CHANNEL_STOPS).map { i ->
        LabColor(l = l, a = labAxisFromFraction(i.toFloat() / LAB_CHANNEL_STOPS), b = b).toComposeColor()
    }.toImmutableList()

internal fun buildLabBGradient(l: Float, a: Float): ImmutableList<Color> =
    (0..LAB_CHANNEL_STOPS).map { i ->
        LabColor(l = l, a = a, b = labAxisFromFraction(i.toFloat() / LAB_CHANNEL_STOPS)).toComposeColor()
    }.toImmutableList()

// Pre-computed independent (a=0, b=0) gradients
private val LightnessIndependentGradient = buildLabLightnessGradient(a = 0f, b = 0f)

private val AAxisIndependentGradient = buildLabAGradient(l = 50f, b = 0f)

private val BAxisIndependentGradient = buildLabBGradient(l = 50f, a = 0f)

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
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.ThumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.ThumbTrackGap,
) {
    val lab = state.labColor
    val gradientColors = remember(lab.a, lab.b, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> LightnessIndependentGradient
            ColoringMode.Contextual -> buildLabLightnessGradient(a = lab.a, b = lab.b)
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
        thumb = thumb,
        thumbWidth = thumbWidth,
        thumbTrackGap = thumbTrackGap,
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
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.ThumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.ThumbTrackGap,
) {
    val lab = state.labColor
    val gradientColors = remember(lab.l, lab.b, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> AAxisIndependentGradient
            ColoringMode.Contextual -> buildLabAGradient(l = lab.l, b = lab.b)
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
        thumb = thumb,
        thumbWidth = thumbWidth,
        thumbTrackGap = thumbTrackGap,
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
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.ThumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.ThumbTrackGap,
) {
    val lab = state.labColor
    val gradientColors = remember(lab.l, lab.a, coloringMode) {
        when (coloringMode) {
            ColoringMode.Independent -> BAxisIndependentGradient
            ColoringMode.Contextual -> buildLabBGradient(l = lab.l, a = lab.a)
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
        thumb = thumb,
        thumbWidth = thumbWidth,
        thumbTrackGap = thumbTrackGap,
    )
}
