package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import codes.side.colorpicker.model.CmykColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes
import codes.side.colorpicker.theme.ColorPickerTheme

/**
 * Complete CMYK picker: cyan, magenta, yellow, and key sliders, plus an optional
 * alpha slider.
 *
 * Each slider is a slot, defaulted to the channel slider it names. Replace one to relabel or
 * restyle that channel: whatever is passed inherits this picker's colors, shapes and
 * dimensions through the theme, and is dimmed only if [enabled] is forwarded to it — though
 * it is refused input either way.
 *
 * @param showAlpha whether to include the [AlphaSlider].
 * @param coloringMode defaults to [ColoringMode.Contextual] so each track previews
 * the resulting color at the current values of the other channels.
 * @param colors checkerboard colors; see [ColorPickerDefaults.colors].
 * @param shapes track shape; see [ColorPickerDefaults.shapes].
 * @param enabled when `false` the picker is dimmed, refuses input, and reports itself
 * disabled to accessibility. Input is refused by the picker as well as by each slider, so a
 * replaced slider slot cannot stay live even if the call site did not forward this to it.
 * @param thumb optional replacement for every slider's thumb; see [ColorSlider].
 * @param cyanSlider slot for the cyan channel; defaults to [CyanSlider].
 * @param magentaSlider slot for the magenta channel; defaults to [MagentaSlider].
 * @param yellowSlider slot for the yellow channel; defaults to [YellowSlider].
 * @param keySlider slot for the key channel; defaults to [KeySlider].
 * @param alphaSlider the [AlphaSlider], shown only when [showAlpha] is `true`.
 */
@Composable
public fun CmykColorPicker(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    showAlpha: Boolean = true,
    enabled: Boolean = true,
    coloringMode: ColoringMode = ColoringMode.Contextual,
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    cyanSlider: @Composable () -> Unit = { CyanSlider(state, enabled = enabled, coloringMode = coloringMode, thumb = thumb) },
    magentaSlider: @Composable () -> Unit = { MagentaSlider(state, enabled = enabled, coloringMode = coloringMode, thumb = thumb) },
    yellowSlider: @Composable () -> Unit = { YellowSlider(state, enabled = enabled, coloringMode = coloringMode, thumb = thumb) },
    keySlider: @Composable () -> Unit = { KeySlider(state, enabled = enabled, coloringMode = coloringMode, thumb = thumb) },
    alphaSlider: @Composable () -> Unit = { AlphaSlider(state, enabled = enabled, thumb = thumb) },
) {
    // Provided rather than passed down, so a replaced slider slot inherits the picker's
    // theme without the call site forwarding it.
    ColorPickerTheme(colors = colors, shapes = shapes) {
        Column(
            modifier = modifier.disabledInput(enabled),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            cyanSlider()
            magentaSlider()
            yellowSlider()
            keySlider()
            if (showAlpha) alphaSlider()
        }
    }
}

/**
 * [CmykColorPicker] over a colour the caller holds, for an app keeping it in a view model rather
 * than in a [ColorPickerState].
 *
 * [onColorChange] fires for changes the user makes, not for a [color] written back in, so the
 * usual loop of the two updating each other does not start.
 *
 * The slots are not here: their defaults name the state, which this overload owns. Reach for
 * the [ColorPickerState] overload to replace a slider.
 */
@Composable
public fun CmykColorPicker(
    color: CmykColor,
    onColorChange: (CmykColor) -> Unit,
    modifier: Modifier = Modifier,
    showAlpha: Boolean = true,
    enabled: Boolean = true,
    coloringMode: ColoringMode = ColoringMode.Contextual,
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
) {
    val state = rememberHoistedColorState(
        color = color,
        read = { cmykColor },
        write = { updateFromCmyk(it) },
        onColorChange = onColorChange,
    )
    CmykColorPicker(
        state = state,
        modifier = modifier,
        showAlpha = showAlpha,
        enabled = enabled,
        coloringMode = coloringMode,
        colors = colors,
        shapes = shapes,
        thumb = thumb,
    )
}
