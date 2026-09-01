package codes.side.colorpicker.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.ui.CmykColorPicker
import codes.side.colorpicker.ui.ColorSwatch
import codes.side.colorpicker.ui.HslColorPicker
import codes.side.colorpicker.ui.LabColorPicker
import codes.side.colorpicker.ui.RgbColorPicker
import codes.side.colorpicker.conversion.toComposeColor

/**
 * One rendered example per component. These are the source of the golden images in
 * `docs/images/`, which the README embeds — so the documentation pictures are produced by
 * the same code the tests render, and cannot drift from it.
 */
internal data class ComponentPreview(
    val name: String,
    val size: DpSize,
    val content: @Composable () -> Unit,
)

/** A fixed pear green so every golden is deterministic. */
private val Seed = HslColor(hue = 68f, saturation = 0.72f, lightness = 0.62f)

private fun state() = ColorPickerState(Seed)

@Composable
private fun Frame(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) { content() }
    }
}

internal val componentPreviews: List<ComponentPreview> = listOf(
    ComponentPreview("hsl-picker", DpSize(440.dp, 300.dp)) {
        Frame { HslColorPicker(state = state(), coloringMode = ColoringMode.Independent) }
    },
    ComponentPreview("hsl-picker-contextual", DpSize(440.dp, 300.dp)) {
        Frame { HslColorPicker(state = state(), coloringMode = ColoringMode.Contextual) }
    },
    ComponentPreview("rgb-picker", DpSize(440.dp, 300.dp)) {
        Frame { RgbColorPicker(state = state()) }
    },
    ComponentPreview("cmyk-picker", DpSize(440.dp, 420.dp)) {
        Frame { CmykColorPicker(state = state()) }
    },
    ComponentPreview("lab-picker", DpSize(440.dp, 300.dp)) {
        Frame { LabColorPicker(state = state()) }
    },
    ComponentPreview("color-swatch", DpSize(440.dp, 112.dp)) {
        Frame {
            ColorSwatch(
                color = Seed.copy(alpha = 0.55f).toComposeColor(),
                modifier = Modifier.fillMaxWidth().height(80.dp),
            )
        }
    },
)
