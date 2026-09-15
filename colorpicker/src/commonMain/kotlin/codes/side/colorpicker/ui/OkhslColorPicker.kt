package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes
import codes.side.colorpicker.theme.ColorPickerTheme

/**
 * Complete Okhsl picker: hue, saturation, and lightness sliders, plus an optional alpha
 * slider.
 *
 * The perceptual counterpart to [HslColorPicker]. Lightness here means perceived
 * lightness, so the middle of the track looks equally light at every hue, and saturation
 * is measured against the display gamut, so `100%` is reachable at every hue and
 * lightness rather than running off the end of what the screen can show.
 *
 * Each slider is a slot, defaulted to the channel slider it names. Replace one to relabel or
 * restyle that channel: whatever is passed inherits this picker's colors, shapes and
 * dimensions through the theme, and is dimmed only if [enabled] is forwarded to it — though
 * it is refused input either way.
 *
 * @param showAlpha whether to include the [AlphaSlider].
 * @param coloringMode defaults to [ColoringMode.Independent], for the same reason as
 * [HslColorPicker]: a contextual hue track collapses into a near-uniform strip at low
 * saturation or extreme lightness, and stops being navigable.
 * @param colors checkerboard colors; see [ColorPickerDefaults.colors].
 * @param shapes track shape; see [ColorPickerDefaults.shapes].
 * @param enabled when `false` the picker is dimmed, refuses input, and reports itself
 * disabled to accessibility. Input is refused by the picker as well as by each slider, so a
 * replaced slider slot cannot stay live even if the call site did not forward this to it.
 * @param thumb optional replacement for every slider's thumb; see [ColorSlider].
 * @param hueSlider slot for the hue channel; defaults to [OkhslHueSlider].
 * @param saturationSlider slot for the saturation channel; defaults to [OkhslSaturationSlider].
 * @param lightnessSlider slot for the lightness channel; defaults to [OkhslLightnessSlider].
 * @param alphaSlider the [AlphaSlider], shown only when [showAlpha] is `true`.
 */
@Composable
public fun OkhslColorPicker(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    showAlpha: Boolean = true,
    enabled: Boolean = true,
    coloringMode: ColoringMode = ColoringMode.Independent,
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    hueSlider: @Composable () -> Unit = { OkhslHueSlider(state, enabled = enabled, coloringMode = coloringMode, thumb = thumb) },
    saturationSlider: @Composable () -> Unit = { OkhslSaturationSlider(state, enabled = enabled, coloringMode = coloringMode, thumb = thumb) },
    lightnessSlider: @Composable () -> Unit = { OkhslLightnessSlider(state, enabled = enabled, coloringMode = coloringMode, thumb = thumb) },
    alphaSlider: @Composable () -> Unit = { AlphaSlider(state, enabled = enabled, thumb = thumb) },
) {
    // Provided rather than passed down, so a replaced slider slot inherits the picker's
    // theme without the call site forwarding it.
    ColorPickerTheme(colors = colors, shapes = shapes) {
        Column(
            modifier = modifier.disabledInput(enabled),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            hueSlider()
            saturationSlider()
            lightnessSlider()
            if (showAlpha) alphaSlider()
        }
    }
}
