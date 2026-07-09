package codes.side.colorpicker.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.font.FontFamily

/**
 * Default label slot rendering used by built-in sliders. Renders [text] with
 * `MaterialTheme.typography.labelMedium`.
 *
 * Exposed so that consumers overriding [ColorSlider]'s `label`/`valueLabel`
 * slots can keep visual consistency with the default style.
 */
@Composable
fun SliderLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
    )
}

/**
 * Default value-label slot rendering used by built-in sliders. Renders [text]
 * with `MaterialTheme.typography.labelMedium` and a monospaced font for
 * stable digit alignment as the value changes.
 */
@Composable
fun SliderValueLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontFamily = FontFamily.Monospace,
    )
}
