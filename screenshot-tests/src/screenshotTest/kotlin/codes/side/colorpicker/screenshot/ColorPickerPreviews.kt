package codes.side.colorpicker.screenshot

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import codes.side.colorpicker.conversion.toComposeColor
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.ui.AlphaSlider
import codes.side.colorpicker.ui.CmykColorPicker
import codes.side.colorpicker.ui.ColorSwatch
import codes.side.colorpicker.ui.HslColorPicker
import codes.side.colorpicker.ui.HueSlider
import codes.side.colorpicker.ui.LabColorPicker
import codes.side.colorpicker.ui.RgbColorPicker
import com.android.tools.screenshot.PreviewTest

/**
 * Compose previews rendered by the official Compose Preview Screenshot Testing plugin.
 *
 *   ./gradlew :screenshot-tests:updateDebugScreenshotTest    record goldens
 *   ./gradlew :screenshot-tests:validateDebugScreenshotTest  check against them
 *
 * These are the only rendered images in the repo: the recorded references double as the
 * README screenshots, which copyGoldensToDocs copies into docs/images under stable names.
 * Every picker appears in both coloring modes, because the difference between them is
 * visual and a gallery is the only place it can honestly be shown.
 */

/** A fixed pear green, so every render is deterministic. */
private val Seed = HslColor(hue = 68f, saturation = 0.72f, lightness = 0.62f)

private fun state() = ColorPickerState(Seed)

// One height for every picker, so the README gallery lines up in a grid.
private const val PICKER_HEIGHT_DP = 420

@Composable
private fun Frame(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = darkColorScheme()) {
        Surface(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
            ) { content() }
        }
    }
}

@PreviewTest
@Preview(name = "HSL independent", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun HslIndependentPreview() = Frame {
    HslColorPicker(state = state(), coloringMode = ColoringMode.Independent)
}

@PreviewTest
@Preview(name = "HSL contextual", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun HslContextualPreview() = Frame {
    HslColorPicker(state = state(), coloringMode = ColoringMode.Contextual)
}

@PreviewTest
@Preview(name = "RGB independent", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun RgbIndependentPreview() = Frame {
    RgbColorPicker(state = state(), coloringMode = ColoringMode.Independent)
}

@PreviewTest
@Preview(name = "RGB contextual", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun RgbContextualPreview() = Frame {
    RgbColorPicker(state = state(), coloringMode = ColoringMode.Contextual)
}

@PreviewTest
@Preview(name = "CMYK independent", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun CmykIndependentPreview() = Frame {
    CmykColorPicker(state = state(), coloringMode = ColoringMode.Independent)
}

@PreviewTest
@Preview(name = "CMYK contextual", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun CmykContextualPreview() = Frame {
    CmykColorPicker(state = state(), coloringMode = ColoringMode.Contextual)
}

@PreviewTest
@Preview(name = "LAB independent", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun LabIndependentPreview() = Frame {
    LabColorPicker(state = state(), coloringMode = ColoringMode.Independent)
}

@PreviewTest
@Preview(name = "LAB contextual", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun LabContextualPreview() = Frame {
    LabColorPicker(state = state(), coloringMode = ColoringMode.Contextual)
}

// Kept character-for-character identical to SquareThumb in the sample app, so the image
// in the README is the thing the sample actually runs.
@Composable
private fun SquareThumb(color: Color, interaction: InteractionSource) {
    val fill = color.copy(alpha = 1f)
    val shape = RoundedCornerShape(16.dp)
    // The ring is the fill lifted most of the way to white, so it tracks the color
    // continuously instead of flipping between two neutrals at a luminance threshold —
    // that flip is visible as a snap the moment you drag across it.
    val ring = lerp(fill, Color.White, 0.6f)

    // The slot is handed the slider's InteractionSource precisely so a thumb can do this.
    val dragged by interaction.collectIsDraggedAsState()
    val pressed by interaction.collectIsPressedAsState()
    val elevation by animateDpAsState(
        targetValue = if (dragged || pressed) 4.dp else 0.dp,
        label = "thumbElevation",
    )

    Box(
        Modifier
            .size(SquareThumbSize)
            .shadow(elevation, shape)
            .background(ring, shape)
            .padding(5.dp)
            .background(fill, RoundedCornerShape(11.dp)),
    )
}

// Material 3's minimum interactive size (LocalMinimumInteractiveComponentSize).
private val SquareThumbSize = 48.dp

@PreviewTest
@Preview(name = "Custom thumb", widthDp = 440, heightDp = 200)
@Composable
fun CustomThumbPreview() = Frame {
    val state = state()
    HueSlider(
        state = state,
        thumb = { source -> SquareThumb(state.hslColor.toComposeColor(), source) },
        thumbWidth = SquareThumbSize,
    )
    AlphaSlider(
        state = state,
        thumb = { source -> SquareThumb(state.hslColor.toComposeColor(), source) },
        thumbWidth = SquareThumbSize,
    )
}

@PreviewTest
@Preview(name = "Swatch translucent", widthDp = 440, heightDp = 112)
@Composable
fun SwatchPreview() = Frame {
    ColorSwatch(
        color = Seed.copy(alpha = 0.55f).toComposeColor(),
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
    )
}
