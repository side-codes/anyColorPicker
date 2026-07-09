package codes.side.colorpicker.ui

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
 * @param colors checkerboard colors used by both the swatch and the picker's alpha
 * slider; see [ColorPickerDefaults.colors].
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
    colors: ColorPickerColors = ColorPickerDefaults.colors(),
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
                HslColorPicker(
                    state = state,
                    showAlpha = showAlpha,
                    colors = colors,
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
