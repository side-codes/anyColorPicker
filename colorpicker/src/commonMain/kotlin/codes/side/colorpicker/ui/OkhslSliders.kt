package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import codes.side.colorpicker.conversion.toComposeColor
import codes.side.colorpicker.model.OkhslColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

/**
 * Saturation the Independent hue track is drawn at, rather than the 1.0 the HSL picker
 * uses.
 *
 * At saturation 1 an Okhsl hue sweep runs along the sRGB gamut surface, which turns a
 * corner wherever the sweep crosses an edge of the RGB cube — around hue 142, red falls
 * to zero as blue starts to rise. A gradient interpolates straight through such a corner,
 * and no practical number of stops fixes it: the error stalls near 9 of 255 even at 256
 * stops. Backing off the boundary makes the sweep smooth, and 0.85 is the most colorful
 * setting that stays under one perceptible step at [OK_HUE_STOPS].
 */
private const val HUE_TRACK_SATURATION = 0.85f

// Stops for the hue track. Okhsl hue is not piecewise linear in sRGB the way HSL's is,
// so there are no breakpoints to land on; 32 measures 1.9 of 255 against the true sweep,
// and doubling it buys nothing.
private const val OK_HUE_STOPS = 32

// The saturation, lightness and value tracks are smooth, and 16 holds them under half a
// step of 255.
private const val OK_CHANNEL_STOPS = 16

internal inline fun buildOkGradient(stops: Int, color: (Float) -> Color): ImmutableList<Color> =
    (0..stops).map { color(it.toFloat() / stops) }.toImmutableList()

/**
 * Slider for the Okhsl hue channel of [state], in degrees `0..360`.
 *
 * @param coloringMode with [ColoringMode.Independent] (the default) the track shows the
 * full spectrum at a fixed saturation and lightness; with [ColoringMode.Contextual] it is
 * rendered at the current saturation and lightness.
 * @param semanticLabel accessibility description of the slider; pass a localized string
 * to replace the English default, or `null` to omit.
 * @param semanticValueText accessibility announcement of the current value in degrees.
 */
@Composable
public fun OkhslHueSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Independent,
    label: (@Composable () -> Unit)? = { SliderLabel("Hue") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.okhslColor.intHue}°") },
    semanticLabel: String? = "Hue",
    semanticValueText: String? = "${state.okhslColor.intHue}°",
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.ThumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.ThumbTrackGap,
) {
    val okhsl = state.okhslColor
    // The two coloring modes differ only in which saturation and lightness the strip is
    // drawn at, so they pick the pair rather than each building a gradient of its own.
    val trackSaturation = when (coloringMode) {
        ColoringMode.Independent -> HUE_TRACK_SATURATION
        ColoringMode.Contextual -> okhsl.saturation
    }
    val trackLightness = when (coloringMode) {
        ColoringMode.Independent -> 0.5f
        ColoringMode.Contextual -> okhsl.lightness
    }
    val gradientColors = remember(trackSaturation, trackLightness) {
        buildOkGradient(OK_HUE_STOPS) { fraction ->
            OkhslColor(
                hue = hueFromFraction(fraction),
                saturation = trackSaturation,
                lightness = trackLightness,
            ).toComposeColor()
        }
    }
    // Matches the track so the thumb never disagrees with the strip under it.
    val thumbColor = remember(okhsl.hue, trackSaturation, trackLightness) {
        OkhslColor(
            hue = okhsl.hue,
            saturation = trackSaturation,
            lightness = trackLightness,
        ).toComposeColor()
    }

    val interaction = remember(state) { SliderInteractionGuard(state) }
    ColorSlider(
        value = okhsl.hue / 360f,
        onValueChange = {
            interaction.begin()
            state.updateOkhslHue(hueFromFraction(it))
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
 * Slider for the Okhsl saturation channel of [state], in `0..1`, from gray to the most
 * colorful the hue and lightness allow on the display.
 *
 * @param coloringMode with [ColoringMode.Independent] (the default) the track is drawn at
 * mid lightness; with [ColoringMode.Contextual] it is drawn at the current lightness.
 * @param semanticLabel accessibility description of the slider; pass a localized string
 * to replace the English default, or `null` to omit.
 * @param semanticValueText accessibility announcement of the current value in percent.
 */
@Composable
public fun OkhslSaturationSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Independent,
    label: (@Composable () -> Unit)? = { SliderLabel("Saturation") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.okhslColor.intSaturation}%") },
    semanticLabel: String? = "Saturation",
    semanticValueText: String? = "${state.okhslColor.intSaturation}%",
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.ThumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.ThumbTrackGap,
) {
    val okhsl = state.okhslColor
    val trackLightness = when (coloringMode) {
        ColoringMode.Independent -> 0.5f
        ColoringMode.Contextual -> okhsl.lightness
    }
    val gradientColors = remember(okhsl.hue, trackLightness) {
        buildOkGradient(OK_CHANNEL_STOPS) { fraction ->
            OkhslColor(
                hue = okhsl.hue,
                saturation = fraction,
                lightness = trackLightness,
            ).toComposeColor()
        }
    }
    val thumbColor = remember(okhsl, trackLightness) {
        OkhslColor(
            hue = okhsl.hue,
            saturation = okhsl.saturation,
            lightness = trackLightness,
        ).toComposeColor()
    }

    val interaction = remember(state) { SliderInteractionGuard(state) }
    ColorSlider(
        value = okhsl.saturation,
        onValueChange = {
            interaction.begin()
            state.updateOkhslSaturation(it)
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
 * Slider for the Okhsl lightness channel of [state], in `0..1`, from black through the
 * hue to white.
 *
 * Unlike [LightnessSlider], the value here is perceived lightness: the midpoint of this
 * track looks equally light at every hue, which is the whole reason to prefer Okhsl over
 * HSL.
 *
 * @param coloringMode with [ColoringMode.Independent] (the default) the track is drawn at
 * full saturation; with [ColoringMode.Contextual] it is drawn at the current saturation.
 * @param semanticLabel accessibility description of the slider; pass a localized string
 * to replace the English default, or `null` to omit.
 * @param semanticValueText accessibility announcement of the current value in percent.
 */
@Composable
public fun OkhslLightnessSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    coloringMode: ColoringMode = ColoringMode.Independent,
    label: (@Composable () -> Unit)? = { SliderLabel("Lightness") },
    valueLabel: (@Composable () -> Unit)? = { SliderValueLabel("${state.okhslColor.intLightness}%") },
    semanticLabel: String? = "Lightness",
    semanticValueText: String? = "${state.okhslColor.intLightness}%",
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    thumbWidth: Dp = ColorPickerDefaults.ThumbWidth,
    thumbTrackGap: Dp = ColorPickerDefaults.ThumbTrackGap,
) {
    val okhsl = state.okhslColor
    val trackSaturation = when (coloringMode) {
        ColoringMode.Independent -> HUE_TRACK_SATURATION
        ColoringMode.Contextual -> okhsl.saturation
    }
    val gradientColors = remember(okhsl.hue, trackSaturation) {
        buildOkGradient(OK_CHANNEL_STOPS) { fraction ->
            OkhslColor(
                hue = okhsl.hue,
                saturation = trackSaturation,
                lightness = fraction,
            ).toComposeColor()
        }
    }
    val thumbColor = remember(okhsl, trackSaturation) {
        OkhslColor(
            hue = okhsl.hue,
            saturation = trackSaturation,
            lightness = okhsl.lightness,
        ).toComposeColor()
    }

    val interaction = remember(state) { SliderInteractionGuard(state) }
    ColorSlider(
        value = okhsl.lightness,
        onValueChange = {
            interaction.begin()
            state.updateOkhslLightness(it)
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
