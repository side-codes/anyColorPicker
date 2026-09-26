package codes.side.colorpicker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import codes.side.color.Cmyk
import codes.side.color.ColorSpace
import codes.side.color.ColorValue
import codes.side.color.Hsl
import codes.side.color.Hsv
import codes.side.color.Hwb
import codes.side.color.Lab
import codes.side.color.Lch
import codes.side.color.OkLch
import codes.side.color.Okhsl
import codes.side.color.Okhsv
import codes.side.color.Oklab
import codes.side.color.Srgb
import codes.side.colorpicker.foundation.EnglishText
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What the picker parameters are for: every picker taking the same configuration, theming every
 * component at once, and turning one off. None of it means anything outside a real composition.
 */
@OptIn(ExperimentalTestApi::class)
class PickerConfigurationTest {

    private val seed = Okhsl(30.0, 0.5, 0.5)

    private class NamedPicker(val space: ColorSpace, val content: @Composable (ColorPickerState) -> Unit)

    private val named = listOf(
        NamedPicker(Srgb) { RgbColorPicker(it) },
        NamedPicker(Hsl) { HslColorPicker(it) },
        NamedPicker(Hsv) { HsvColorPicker(it) },
        NamedPicker(Hwb) { HwbColorPicker(it) },
        NamedPicker(Lab) { LabColorPicker(it) },
        NamedPicker(Lch) { LchColorPicker(it) },
        NamedPicker(Oklab) { OklabColorPicker(it) },
        NamedPicker(OkLch) { OkLchColorPicker(it) },
        NamedPicker(Okhsl) { OkhslColorPicker(it) },
        NamedPicker(Okhsv) { OkhsvColorPicker(it) },
        NamedPicker(Cmyk) { CmykColorPicker(it) },
    )

    @Test
    fun eachNamedPickerShowsItsOwnSpace() = runComposeUiTest {
        val state = ColorPickerState(seed)
        var index by mutableIntStateOf(0)
        setContent { named[index].content(state) }
        for (i in named.indices) {
            index = i
            waitForIdle()
            val space = named[i].space
            val labels = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo)).fetchSemanticsNodes()
                .map { it.config[SemanticsProperties.ContentDescription].single() }
            assertEquals(space.channels.map { EnglishText.channelSpokenName(it) } + EnglishText.alphaName(), labels, "${space.id}'s sliders")
            val planes = onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.CustomActions)).fetchSemanticsNodes().size
            assertEquals(if (hasPlane(space)) 1 else 0, planes, "${space.id}'s plane")
        }
    }

    @Test
    fun everyPickerTakesTheSameConfigurationInAllThreeForms() = runComposeUiTest {
        // That this compiles is most of the assertion: the twelve pickers agree on the set.
        val onState: List<@Composable (ColorPickerState) -> Unit> = listOf(
            { ColorPicker(it, enabled = false, thumb = {}, onValueChangeFinished = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { RgbColorPicker(it, enabled = false, thumb = {}, onValueChangeFinished = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { HslColorPicker(it, enabled = false, thumb = {}, onValueChangeFinished = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { HsvColorPicker(it, enabled = false, thumb = {}, onValueChangeFinished = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { HwbColorPicker(it, enabled = false, thumb = {}, onValueChangeFinished = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { LabColorPicker(it, enabled = false, thumb = {}, onValueChangeFinished = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { LchColorPicker(it, enabled = false, thumb = {}, onValueChangeFinished = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { OklabColorPicker(it, enabled = false, thumb = {}, onValueChangeFinished = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { OkLchColorPicker(it, enabled = false, thumb = {}, onValueChangeFinished = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { OkhslColorPicker(it, enabled = false, thumb = {}, onValueChangeFinished = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { OkhsvColorPicker(it, enabled = false, thumb = {}, onValueChangeFinished = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { CmykColorPicker(it, enabled = false, thumb = {}, onValueChangeFinished = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
        )
        val onValue: List<@Composable (ColorValue) -> Unit> = listOf(
            { ColorPicker(it, {}, enabled = false, thumb = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { RgbColorPicker(it, {}, enabled = false, thumb = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { HslColorPicker(it, {}, enabled = false, thumb = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { HsvColorPicker(it, {}, enabled = false, thumb = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { HwbColorPicker(it, {}, enabled = false, thumb = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { LabColorPicker(it, {}, enabled = false, thumb = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { LchColorPicker(it, {}, enabled = false, thumb = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { OklabColorPicker(it, {}, enabled = false, thumb = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { OkLchColorPicker(it, {}, enabled = false, thumb = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { OkhslColorPicker(it, {}, enabled = false, thumb = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { OkhsvColorPicker(it, {}, enabled = false, thumb = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { CmykColorPicker(it, {}, enabled = false, thumb = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
        )
        val onColor: List<@Composable (Color) -> Unit> = listOf(
            { ColorPicker(it, {}, enabled = false, thumb = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { RgbColorPicker(it, {}, enabled = false, thumb = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { HslColorPicker(it, {}, enabled = false, thumb = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { HsvColorPicker(it, {}, enabled = false, thumb = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { HwbColorPicker(it, {}, enabled = false, thumb = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { LabColorPicker(it, {}, enabled = false, thumb = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { LchColorPicker(it, {}, enabled = false, thumb = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { OklabColorPicker(it, {}, enabled = false, thumb = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { OkLchColorPicker(it, {}, enabled = false, thumb = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { OkhslColorPicker(it, {}, enabled = false, thumb = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { OkhsvColorPicker(it, {}, enabled = false, thumb = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
            { CmykColorPicker(it, {}, enabled = false, thumb = {}, orientation = Orientation.Horizontal, alphaSlider = null) },
        )
        setContent {
            val shared = remember { ColorPickerState(seed) }
            Column(Modifier.verticalScroll(rememberScrollState())) {
                onState.forEach { picker -> picker(shared) }
                onValue.forEach { picker -> picker(seed) }
                onColor.forEach { picker -> picker(Color.Red) }
            }
        }
    }

    /**
     * The track is one part of a slider's height, and a slider never measures below the 48 dp minimum
     * touch size, so the size here has to clear that before the change shows at all.
     */
    @Test
    fun aThemedTrackHeightReachesAChannelSlider() = runComposeUiTest {
        setContent {
            Box(Modifier.testTag("default")) { ChannelSlider(ColorPickerState(seed), Okhsl.H) }
        }
        val default = onNodeWithTag("default").getUnclippedBoundsInRoot().height
        setContent {
            ColorPickerTheme(dimensions = ColorPickerDefaults.dimensions(trackHeight = 80.dp)) {
                Box(Modifier.testTag("raised")) { ChannelSlider(ColorPickerState(seed), Okhsl.H) }
            }
        }
        val raised = onNodeWithTag("raised").getUnclippedBoundsInRoot().height
        assertTrue(raised > default, "an 80dp track should make the slider taller than the 16dp default, $raised vs $default")
    }

    @Test
    fun aDisabledSliderRefusesADrag() = runComposeUiTest {
        val state = ColorPickerState(seed)
        setContent {
            ChannelSlider(state, Okhsl.H, Modifier.testTag("slider").width(200.dp), enabled = false, label = null, valueLabel = null)
        }
        onNodeWithTag("slider").performTouchInput { swipeRight() }
        assertEquals(seed, state.value)
    }

    @Test
    fun anEnabledSliderAcceptsADrag() = runComposeUiTest {
        val state = ColorPickerState(seed)
        setContent { ChannelSlider(state, Okhsl.H, Modifier.testTag("slider").width(200.dp), label = null, valueLabel = null) }
        onNodeWithTag("slider").performTouchInput { swipeRight() }
        assertTrue(state.value != seed, "an enabled slider should have moved")
    }

    private fun colourfulness(pixels: androidx.compose.ui.graphics.PixelMap): Float {
        var total = 0f
        val y = pixels.height / 2
        for (x in 0 until pixels.width) {
            val c = pixels[x, y]
            total += maxOf(c.red, c.green, c.blue) - minOf(c.red, c.green, c.blue)
        }
        return total / pixels.width
    }

    /**
     * Refusing the gesture is half of disabled; the other half is looking it. Measured as how colourful
     * the track still is, since a dimmed gradient over the background loses spread between its channels
     * rather than moving in any one direction.
     */
    @Test
    fun aDisabledSliderIsDrawnDimmer() = runComposeUiTest {
        setContent {
            Box(Modifier.testTag("slider").width(200.dp).background(Color.White)) {
                ChannelSlider(ColorPickerState(seed), Okhsl.H, label = null, valueLabel = null)
            }
        }
        val lit = colourfulness(onNodeWithTag("slider").captureToImage().toPixelMap())
        setContent {
            Box(Modifier.testTag("slider").width(200.dp).background(Color.White)) {
                ChannelSlider(ColorPickerState(seed), Okhsl.H, enabled = false, label = null, valueLabel = null)
            }
        }
        val dimmed = colourfulness(onNodeWithTag("slider").captureToImage().toPixelMap())
        assertTrue(dimmed < lit * 0.75f, "a disabled track should be visibly washed out: $dimmed against $lit")
    }

    /** The planes dim through their own path, not the slider's, so they get their own check. */
    @Test
    fun aDisabledPlaneIsDrawnDimmer() = runComposeUiTest {
        setContent {
            Box(Modifier.background(Color.White)) {
                ChannelPlane(ColorPickerState(Hsl(200.0, 80.0, 50.0)), Hsl.S, Hsl.L, Modifier.testTag("plane").size(160.dp))
            }
        }
        val lit = colourfulness(onNodeWithTag("plane").captureToImage().toPixelMap())
        setContent {
            Box(Modifier.background(Color.White)) {
                ChannelPlane(ColorPickerState(Hsl(200.0, 80.0, 50.0)), Hsl.S, Hsl.L, Modifier.testTag("plane").size(160.dp), enabled = false)
            }
        }
        val dimmed = colourfulness(onNodeWithTag("plane").captureToImage().toPixelMap())
        assertTrue(dimmed < lit * 0.75f, "a disabled plane should be visibly washed out: $dimmed against $lit")
    }

    /**
     * The two knobs are independent, so all four settings have to behave: dim only, drain only, both,
     * and neither. Colourfulness catches draining; lightness toward the white background catches dimming.
     */
    @Test
    fun theDisabledLookIsWhateverTheColorsAskFor() = runComposeUiTest {
        fun measure(disabledAlpha: Float, disabledSaturation: Float): Pair<Float, Float> {
            setContent {
                ColorPickerTheme(
                    colors = ColorPickerDefaults.colors(disabledAlpha = disabledAlpha, disabledSaturation = disabledSaturation),
                ) {
                    Box(Modifier.background(Color.White)) {
                        ChannelPlane(
                            ColorPickerState(Hsl(200.0, 80.0, 50.0)),
                            Hsl.S,
                            Hsl.L,
                            Modifier.testTag("plane").size(160.dp),
                            enabled = false,
                        )
                    }
                }
            }
            val pixels = onNodeWithTag("plane").captureToImage().toPixelMap()
            var light = 0f
            val y = pixels.height / 2
            for (x in 0 until pixels.width) {
                val c = pixels[x, y]
                light += (c.red + c.green + c.blue) / 3f
            }
            return colourfulness(pixels) to light / pixels.width
        }

        val (untouchedColour, untouchedLight) = measure(1f, 1f)
        val (_, dimmedLight) = measure(0.38f, 1f)
        val (drainedColour, _) = measure(1f, 0f)
        val (bothColour, bothLight) = measure(0.38f, 0f)

        // Neither knob set leaves it looking enabled.
        assertTrue(untouchedColour > 0.05f, "untouched should still be colourful: $untouchedColour")
        // Dimming lightens it toward the background without draining much colour outright.
        assertTrue(dimmedLight > untouchedLight, "dimming should lighten: $dimmedLight vs $untouchedLight")
        // Draining removes the colour while leaving the weight alone.
        assertTrue(drainedColour < untouchedColour * 0.1f, "draining should remove colour: $drainedColour")
        // Both does both.
        assertTrue(bothColour < untouchedColour * 0.1f, "both should remove colour: $bothColour")
        assertTrue(bothLight > untouchedLight, "both should lighten: $bothLight vs $untouchedLight")
    }
}
