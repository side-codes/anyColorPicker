package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import codes.side.colorpicker.conversion.toComposeColor
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColorPickerStateSaver
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes

/**
 * [AlertDialog] hosting an [HslColorPicker] with a preview swatch and
 * confirm/dismiss buttons.
 *
 * The dialog owns its picker state, saved with [initialColor] as the key: in-progress
 * edits survive configuration changes and process death, while passing a different
 * [initialColor] recreates the state at that color.
 *
 * @param onColorSelected called with the chosen color when the confirm button is
 * pressed; the caller is responsible for dismissing the dialog.
 * @param onDismiss called when the user cancels or dismisses the dialog.
 * @param title dialog title text; [confirmText] and [dismissText] label the buttons —
 * pass localized strings to replace the English defaults.
 * @param showAlpha whether to include the alpha slider.
 * @param enabled when `false` the picker inside the dialog is dimmed and refuses input. The
 * buttons stay live, so the dialog can still be dismissed.
 * @param colors checkerboard colors used by both the swatch and the picker's alpha
 * slider; see [ColorPickerDefaults.colors].
 * @param shapes track and swatch shapes; see [ColorPickerDefaults.shapes].
 * @param thumb optional replacement for every slider's thumb; see [ColorSlider].
 * @param hueSlider slot for the hue channel; `null` keeps [HueSlider]. The dialog owns its
 * state, so every slot is handed the [ColorPickerState] to read and write — without it a
 * replacement slider would have nothing to bind to.
 * @param saturationSlider slot for the saturation channel; `null` keeps [SaturationSlider].
 * @param lightnessSlider slot for the lightness channel; `null` keeps [LightnessSlider].
 * @param alphaSlider slot for the alpha channel; `null` keeps [AlphaSlider].
 */
@Composable
public fun ColorPickerDialog(
    onColorSelected: (HslColor) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initialColor: HslColor = HslColor(),
    title: String = "Pick a Color",
    confirmText: String = "Select",
    dismissText: String = "Cancel",
    showAlpha: Boolean = true,
    enabled: Boolean = true,
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
    hueSlider: (@Composable (ColorPickerState) -> Unit)? = null,
    saturationSlider: (@Composable (ColorPickerState) -> Unit)? = null,
    lightnessSlider: (@Composable (ColorPickerState) -> Unit)? = null,
    alphaSlider: (@Composable (ColorPickerState) -> Unit)? = null,
) {
    // initialColor is a reset key: a new initial color re-creates the state,
    // while configuration changes restore in-progress edits via the saver.
    val state = rememberSaveable(initialColor, saver = ColorPickerStateSaver) {
        ColorPickerState(initialColor)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
            ) {
                ColorSwatch(
                    color = state.hslColor.toComposeColor(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = colors,
                )
                Spacer(Modifier.height(16.dp))
                // The slots are nullable rather than defaulted to the sliders: a default
                // would have to name the state the dialog creates, which does not exist yet
                // where the parameter list is written. Null means whatever the picker draws.
                HslColorPicker(
                    state = state,
                    showAlpha = showAlpha,
                    enabled = enabled,
                    colors = colors,
                    shapes = shapes,
                    thumb = thumb,
                    hueSlider = hueSlider.boundTo(state) { HueSlider(state, enabled = enabled, thumb = thumb) },
                    saturationSlider = saturationSlider.boundTo(state) { SaturationSlider(state, enabled = enabled, thumb = thumb) },
                    lightnessSlider = lightnessSlider.boundTo(state) { LightnessSlider(state, enabled = enabled, thumb = thumb) },
                    alphaSlider = alphaSlider.boundTo(state) { AlphaSlider(state, enabled = enabled, thumb = thumb) },
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(state.hslColor) }) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(dismissText)
            }
        },
    )
}

/**
 * Adapts a dialog slot, which is given the state the dialog owns, to a picker slot, which is
 * given nothing. Null keeps [default].
 */
private fun (@Composable (ColorPickerState) -> Unit)?.boundTo(
    state: ColorPickerState,
    default: @Composable () -> Unit,
): @Composable () -> Unit {
    val slot = this ?: return default
    return { slot(state) }
}
