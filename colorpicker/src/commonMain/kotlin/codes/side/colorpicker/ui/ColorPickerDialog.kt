package codes.side.colorpicker.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import codes.side.colorpicker.conversion.toComposeColor
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.state.rememberColorPickerState

@Composable
fun ColorPickerDialog(
    initialColor: HslColor = HslColor(),
    onColorSelected: (HslColor) -> Unit,
    onDismiss: () -> Unit,
    title: String = "Pick a Color",
    showAlpha: Boolean = true,
    coloringMode: ColoringMode = ColoringMode.Independent,
) {
    val state = rememberColorPickerState(initialColor)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                ColorSwatch(
                    color = state.hslColor.toComposeColor(),
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                )
                Spacer(Modifier.height(16.dp))
                HslColorPicker(
                    state = state,
                    showAlpha = showAlpha,
                    coloringMode = coloringMode,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onColorSelected(state.hslColor) }) {
                Text("Select")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
