package codes.side.colorpicker.screenshot

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.foundation.layout.width
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
import codes.side.color.ColorValue
import codes.side.color.Hsl
import codes.side.color.Okhsl
import codes.side.color.Okhsv
import codes.side.color.compose.toComposeColor
import codes.side.color.parseCss
import codes.side.colorpicker.foundation.ColoringMode
import codes.side.colorpicker.material3.AlphaSlider
import codes.side.colorpicker.material3.ChannelPlane
import codes.side.colorpicker.material3.ChannelSlider
import codes.side.colorpicker.material3.CmykColorPicker
import codes.side.colorpicker.material3.ColorPickerDefaults
import codes.side.colorpicker.material3.ColorPickerDialog
import codes.side.colorpicker.material3.ColorSwatch
import codes.side.colorpicker.material3.HslColorPicker
import codes.side.colorpicker.material3.HsvColorPicker
import codes.side.colorpicker.material3.HwbColorPicker
import codes.side.colorpicker.material3.LabColorPicker
import codes.side.colorpicker.material3.LchColorPicker
import codes.side.colorpicker.material3.OkhslColorPicker
import codes.side.colorpicker.material3.OkhsvColorPicker
import codes.side.colorpicker.material3.OklabColorPicker
import codes.side.colorpicker.material3.OklchColorPicker
import codes.side.colorpicker.material3.RgbColorPicker
import codes.side.colorpicker.state.ColorPickerState
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

// Pantone's Colors of the Year, as the sRGB values commonly published for them: one for each case the README shows,
// fixed so every render is deterministic. A picker's independent and contextual renders share one, so the pair differs
// only in coloring mode. They are sRGB, so a dialog is told to open in Okhsl rather than on its RGB tab, which has no
// plane.
private val ClassicBlue = ColorValue.parseCss("#0F4C81")    // 2020
private val LivingCoral = ColorValue.parseCss("#FF6F61")    // 2019
private val Greenery = ColorValue.parseCss("#88B04B")       // 2017
private val UltraViolet = ColorValue.parseCss("#5F4B8B")    // 2018
private val Mimosa = ColorValue.parseCss("#F0C05A")         // 2009
private val Emerald = ColorValue.parseCss("#009473")        // 2013
private val VivaMagenta = ColorValue.parseCss("#BB2649")    // 2023
private val Turquoise = ColorValue.parseCss("#45B5AA")      // 2010
private val TangerineTango = ColorValue.parseCss("#DD4124") // 2012
private val VeryPeri = ColorValue.parseCss("#6667AB")       // 2022
private val RadiantOrchid = ColorValue.parseCss("#B163A3")  // 2014
private val Illuminating = ColorValue.parseCss("#F5DF4D")   // 2021
private val Honeysuckle = ColorValue.parseCss("#D94F70")    // 2011
private val Cerulean = ColorValue.parseCss("#9BB7D4")       // 2000
private val ChiliPepper = ColorValue.parseCss("#9B1B30")    // 2007
private val AquaSky = ColorValue.parseCss("#7BC4C4")        // 2003
private val FuchsiaRose = ColorValue.parseCss("#C74375")    // 2001
private val Tigerlily = ColorValue.parseCss("#E2583E")      // 2004

// One height for every picker, so the README gallery lines up in a grid. A picker with a plane is
// the tallest: a 255dp plane at this width, three sliders and alpha.
private const val PICKER_HEIGHT_DP = 640

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
@Preview(name = "RGB independent", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun RgbIndependentPreview() = Frame { RgbColorPicker(state = ColorPickerState(ClassicBlue), coloringMode = ColoringMode.Independent) }

@PreviewTest
@Preview(name = "RGB contextual", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun RgbContextualPreview() = Frame { RgbColorPicker(state = ColorPickerState(ClassicBlue), coloringMode = ColoringMode.Contextual) }

@PreviewTest
@Preview(name = "HSL independent", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun HslIndependentPreview() = Frame { HslColorPicker(state = ColorPickerState(LivingCoral), coloringMode = ColoringMode.Independent) }

@PreviewTest
@Preview(name = "HSL contextual", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun HslContextualPreview() = Frame { HslColorPicker(state = ColorPickerState(LivingCoral), coloringMode = ColoringMode.Contextual) }

@PreviewTest
@Preview(name = "HSV independent", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun HsvIndependentPreview() = Frame { HsvColorPicker(state = ColorPickerState(Greenery), coloringMode = ColoringMode.Independent) }

@PreviewTest
@Preview(name = "HSV contextual", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun HsvContextualPreview() = Frame { HsvColorPicker(state = ColorPickerState(Greenery), coloringMode = ColoringMode.Contextual) }

@PreviewTest
@Preview(name = "HWB independent", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun HwbIndependentPreview() = Frame { HwbColorPicker(state = ColorPickerState(UltraViolet), coloringMode = ColoringMode.Independent) }

@PreviewTest
@Preview(name = "HWB contextual", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun HwbContextualPreview() = Frame { HwbColorPicker(state = ColorPickerState(UltraViolet), coloringMode = ColoringMode.Contextual) }

@PreviewTest
@Preview(name = "LAB independent", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun LabIndependentPreview() = Frame { LabColorPicker(state = ColorPickerState(Mimosa), coloringMode = ColoringMode.Independent) }

@PreviewTest
@Preview(name = "LAB contextual", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun LabContextualPreview() = Frame { LabColorPicker(state = ColorPickerState(Mimosa), coloringMode = ColoringMode.Contextual) }

@PreviewTest
@Preview(name = "LCH independent", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun LchIndependentPreview() = Frame { LchColorPicker(state = ColorPickerState(Emerald), coloringMode = ColoringMode.Independent) }

@PreviewTest
@Preview(name = "LCH contextual", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun LchContextualPreview() = Frame { LchColorPicker(state = ColorPickerState(Emerald), coloringMode = ColoringMode.Contextual) }

@PreviewTest
@Preview(name = "Oklab independent", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun OklabIndependentPreview() = Frame { OklabColorPicker(state = ColorPickerState(VivaMagenta), coloringMode = ColoringMode.Independent) }

@PreviewTest
@Preview(name = "Oklab contextual", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun OklabContextualPreview() = Frame { OklabColorPicker(state = ColorPickerState(VivaMagenta), coloringMode = ColoringMode.Contextual) }

@PreviewTest
@Preview(name = "OkLCh independent", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun OklchIndependentPreview() = Frame { OklchColorPicker(state = ColorPickerState(Turquoise), coloringMode = ColoringMode.Independent) }

@PreviewTest
@Preview(name = "OkLCh contextual", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun OklchContextualPreview() = Frame { OklchColorPicker(state = ColorPickerState(Turquoise), coloringMode = ColoringMode.Contextual) }

@PreviewTest
@Preview(name = "Okhsl independent", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun OkhslIndependentPreview() = Frame { OkhslColorPicker(state = ColorPickerState(TangerineTango), coloringMode = ColoringMode.Independent) }

@PreviewTest
@Preview(name = "Okhsl contextual", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun OkhslContextualPreview() = Frame { OkhslColorPicker(state = ColorPickerState(TangerineTango), coloringMode = ColoringMode.Contextual) }

@PreviewTest
@Preview(name = "Okhsv independent", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun OkhsvIndependentPreview() = Frame { OkhsvColorPicker(state = ColorPickerState(VeryPeri), coloringMode = ColoringMode.Independent) }

@PreviewTest
@Preview(name = "Okhsv contextual", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun OkhsvContextualPreview() = Frame { OkhsvColorPicker(state = ColorPickerState(VeryPeri), coloringMode = ColoringMode.Contextual) }

@PreviewTest
@Preview(name = "CMYK independent", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun CmykIndependentPreview() = Frame { CmykColorPicker(state = ColorPickerState(RadiantOrchid), coloringMode = ColoringMode.Independent) }

@PreviewTest
@Preview(name = "CMYK contextual", widthDp = 440, heightDp = PICKER_HEIGHT_DP)
@Composable
fun CmykContextualPreview() = Frame { CmykColorPicker(state = ColorPickerState(RadiantOrchid), coloringMode = ColoringMode.Contextual) }

@PreviewTest
@Preview(name = "Horizontal picker", widthDp = 720, heightDp = 360)
@Composable
fun HorizontalPickerPreview() = Frame { OkhslColorPicker(state = ColorPickerState(Illuminating), orientation = Orientation.Horizontal) }

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
    val state = ColorPickerState(Honeysuckle)
    ChannelSlider(
        state = state,
        channel = Okhsl.H,
        thumb = { SquareThumb(state.color, interactionSource) },
        dimensions = ColorPickerDefaults.currentDimensions().copy(thumbWidth = SquareThumbSize),
    )
    AlphaSlider(
        state = state,
        thumb = { SquareThumb(state.color, interactionSource) },
        dimensions = ColorPickerDefaults.currentDimensions().copy(thumbWidth = SquareThumbSize),
    )
}

@PreviewTest
@Preview(name = "Saturation lightness plane", widthDp = 440, heightDp = 420)
@Composable
fun HslPlanePreview() = Frame {
    val state = ColorPickerState(Cerulean)
    ChannelPlane(state, Hsl.S, Hsl.L, Modifier.fillMaxWidth().height(260.dp))
    ChannelSlider(state, Hsl.H)
}

@PreviewTest
@Preview(name = "Okhsl plane", widthDp = 440, heightDp = 420)
@Composable
fun OkhslPlanePreview() = Frame {
    val state = ColorPickerState(ChiliPepper)
    ChannelPlane(state, Okhsl.S, Okhsl.L, Modifier.fillMaxWidth().height(260.dp))
    ChannelSlider(state, Okhsl.H)
}

@PreviewTest
@Preview(name = "Okhsv plane", widthDp = 440, heightDp = 420)
@Composable
fun OkhsvPlanePreview() = Frame {
    val state = ColorPickerState(AquaSky)
    ChannelPlane(state, Okhsv.S, Okhsv.V, Modifier.fillMaxWidth().height(260.dp))
    ChannelSlider(state, Okhsv.H)
}

// The dialog at a phone's width, a 440 dp screen less the margins its dialog window keeps, on a canvas in the landscape
// screenshot's proportions, so the README shows the two at one size. Left to itself on a canvas this wide, the dialog
// would take Material's widest, 560 dp.
@PreviewTest
@Preview(name = "Dialog", widthDp = 2048, heightDp = 960)
@Composable
fun DialogPreview() = Frame {
    ColorPickerDialog(
        initialValue = FuchsiaRose.withAlpha(0.8),
        onValueSelected = {},
        onDismissRequest = {},
        modifier = Modifier.width(392.dp),
        initialSpace = Okhsl,
    )
}

// A landscape tablet: too short to stack the picker, and wide enough to put the plane beside the sliders. A
// phone's dialog window is narrower than that in landscape too, so there the body scrolls instead.
@PreviewTest
@Preview(name = "Dialog horizontal", widthDp = 1280, heightDp = 600)
@Composable
fun DialogHorizontalPreview() = Frame {
    ColorPickerDialog(initialValue = FuchsiaRose.withAlpha(0.8), onValueSelected = {}, onDismissRequest = {}, initialSpace = Okhsl)
}

@PreviewTest
@Preview(name = "Swatch translucent", widthDp = 440, heightDp = 112)
@Composable
fun SwatchPreview() = Frame {
    ColorSwatch(
        color = Tigerlily.withAlpha(0.55).toComposeColor(),
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp),
    )
}
