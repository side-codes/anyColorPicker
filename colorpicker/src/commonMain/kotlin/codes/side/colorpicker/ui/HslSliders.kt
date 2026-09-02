package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import codes.side.colorpicker.conversion.toComposeColor
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList

// The largest hue the slider ever writes. HslColor normalizes hue 360 to the
// equivalent 0, so writing exactly 360 at the right end of the track would snap
// the thumb back to the far left mid-drag; mapping over 0..360 (exclusive)
// keeps the right end a stable thumb position.
// Float.nextDown() is not available in common code; step to the previous
// representable Float via its bit pattern instead.
private val MAX_SLIDER_HUE = Float.fromBits(360f.toRawBits() - 1)

/** Maps a slider fraction in `0..1` to a hue in `0..360` (exclusive). */
internal fun hueFromFraction(fraction: Float): Float =
    (fraction * 360f).coerceAtMost(MAX_SLIDER_HUE)

private val HUE_RAINBOW = persistentListOf(
    Color.Red,
    Color.Yellow,
    Color.Green,
    Color.Cyan,
    Color.Blue,
    Color.Magenta,
    Color.Red,
)

/**
 * Slider for the HSL hue channel of [state], in degrees `0..360`.
 *
 * @param coloringMode with [ColoringMode.Independent] (the default) the track always
 * shows the full rainbow; with [ColoringMode.Contextual] it is rendered at the current
 * saturation and lightness.
 * @param semanticLabel accessibility description of the slider; pass a localized string
 * to replace the English default, or `null` to omit.
 * @param semanticValueText accessibility announcement of the current value in degrees.
 */
@Composable
public fun HueSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Independent,
    label: (@Composable () -> Unit)? = { SliderLabel("Hue") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.hslColor.intHue}°") },
    semanticLabel: String? = "Hue",
    semanticValueText: String? = "${state.hslColor.intHue}°",
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.ThumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.ThumbTrackGap,
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

    val interaction = remember(state) { SliderInteractionGuard(state) }
    ColorSlider(
        value = hsl.hue / 360f,
        onValueChange = {
            interaction.begin()
            state.updateHue(hueFromFraction(it))
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
 * Slider for the HSL saturation channel of [state], in `0..1`.
 *
 * @param coloringMode with [ColoringMode.Independent] (the default) the track runs from
 * gray to the pure hue at mid lightness; with [ColoringMode.Contextual] it is rendered
 * at the current lightness.
 * @param semanticLabel accessibility description of the slider; pass a localized string
 * to replace the English default, or `null` to omit.
 * @param semanticValueText accessibility announcement of the current value in percent.
 */
@Composable
public fun SaturationSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Independent,
    label: (@Composable () -> Unit)? = { SliderLabel("Saturation") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.hslColor.intSaturation}%") },
    semanticLabel: String? = "Saturation",
    semanticValueText: String? = "${state.hslColor.intSaturation}%",
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.ThumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.ThumbTrackGap,
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

    val interaction = remember(state) { SliderInteractionGuard(state) }
    ColorSlider(
        value = hsl.saturation,
        onValueChange = {
            interaction.begin()
            state.updateSaturation(it)
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
 * Slider for the HSL lightness channel of [state], in `0..1`, from black through the
 * hue to white.
 *
 * @param coloringMode with [ColoringMode.Independent] (the default) the track's midpoint
 * is the pure hue; with [ColoringMode.Contextual] it is rendered at the current saturation.
 * @param semanticLabel accessibility description of the slider; pass a localized string
 * to replace the English default, or `null` to omit.
 * @param semanticValueText accessibility announcement of the current value in percent.
 */
@Composable
public fun LightnessSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Independent,
    label: (@Composable () -> Unit)? = { SliderLabel("Lightness") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.hslColor.intLightness}%") },
    semanticLabel: String? = "Lightness",
    semanticValueText: String? = "${state.hslColor.intLightness}%",
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.ThumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.ThumbTrackGap,
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

    val interaction = remember(state) { SliderInteractionGuard(state) }
    ColorSlider(
        value = hsl.lightness,
        onValueChange = {
            interaction.begin()
            state.updateLightness(it)
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

// HSL to RGB is piecewise linear in hue, with breakpoints every 60 degrees. Six segments
// put a stop on every breakpoint, so linear interpolation between them reproduces the
// curve exactly; any other count cuts the corners and the track stops matching the color
// its own thumb previews (7 segments is off by up to 55/255 at hue 180).
private const val HUE_SEGMENTS = 6

internal fun buildHueGradient(saturation: Float, lightness: Float): ImmutableList<Color> =
    (0..HUE_SEGMENTS).map { i ->
        val hue = (i * 360f / HUE_SEGMENTS).coerceAtMost(360f)
        HslColor(hue = hue, saturation = saturation, lightness = lightness).toComposeColor()
    }.toImmutableList()
