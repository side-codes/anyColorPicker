package codes.side.colorpicker.screenshot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.android.tools.screenshot.PreviewTest
import androidx.compose.ui.unit.dp
import codes.side.colorpicker.conversion.toComposeColor
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.ui.CmykColorPicker
import codes.side.colorpicker.ui.ColorSwatch
import codes.side.colorpicker.ui.HslColorPicker
import codes.side.colorpicker.ui.LabColorPicker
import codes.side.colorpicker.ui.RgbColorPicker

/**
 * Compose previews rendered by the official Compose Preview Screenshot Testing plugin.
 *
 *   ./gradlew :screenshot-tests:updateDebugScreenshotTest    record goldens
 *   ./gradlew :screenshot-tests:validateDebugScreenshotTest  check against them
 *
 * These are the only rendered images in the repo: the recorded references double as the
 * README screenshots, which copyGoldensToDocs copies into docs/images under stable names.
 */

/** A fixed pear green, so every render is deterministic. */
private val Seed = HslColor(hue = 68f, saturation = 0.72f, lightness = 0.62f)

private fun state() = ColorPickerState(Seed)

@Composable
private fun Frame(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) { content() }
        }
    }
}

@PreviewTest
@Preview(name = "HSL independent", widthDp = 440, heightDp = 300)
@Composable
fun HslIndependentPreview() = Frame {
    HslColorPicker(state = state(), coloringMode = ColoringMode.Independent)
}

@PreviewTest
@Preview(name = "HSL contextual", widthDp = 440, heightDp = 300)
@Composable
fun HslContextualPreview() = Frame {
    HslColorPicker(state = state(), coloringMode = ColoringMode.Contextual)
}

@PreviewTest
@Preview(name = "RGB", widthDp = 440, heightDp = 300)
@Composable
fun RgbPreview() = Frame { RgbColorPicker(state = state()) }

@PreviewTest
@Preview(name = "CMYK", widthDp = 440, heightDp = 420)
@Composable
fun CmykPreview() = Frame { CmykColorPicker(state = state()) }

@PreviewTest
@Preview(name = "LAB", widthDp = 440, heightDp = 300)
@Composable
fun LabPreview() = Frame { LabColorPicker(state = state()) }

@PreviewTest
@Preview(name = "Swatch translucent", widthDp = 440, heightDp = 112)
@Composable
fun SwatchPreview() = Frame {
    ColorSwatch(
        color = Seed.copy(alpha = 0.55f).toComposeColor(),
        modifier = Modifier.fillMaxWidth().height(80.dp),
    )
}
