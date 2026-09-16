package codes.side.colorpicker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.height
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * What the picker parameters are for: replacing one slider, theming every slider at once, and
 * turning the whole thing off. None of it means anything outside a real composition.
 */
@OptIn(ExperimentalTestApi::class)
class PickerConfigurationTest {

    private fun state() = ColorPickerState(HslColor(hue = 200f, saturation = 0.8f, lightness = 0.5f))

    @Test
    fun aReplacedSlotIsDrawnInsteadOfTheDefault() = runComposeUiTest {
        setContent {
            HslColorPicker(state = state(), hueSlider = { Text("Farbton") })
        }
        onNodeWithText("Farbton").assertExists()
        // The slider it stood in for is gone, and the ones beside it are not.
        onNodeWithText("Hue").assertDoesNotExist()
        onNodeWithText("Saturation").assertExists()
    }

    @Test
    fun aReplacedSlotInheritsThePickersColorsWithoutForwardingThem() = runComposeUiTest {
        val custom = ColorPickerColors(
            checkerboardLight = Color.Red,
            checkerboardDark = Color.Green,
            disabledAlpha = 0.5f,
            disabledSaturation = 1f,
        )
        var seen: ColorPickerColors? = null
        setContent {
            HslColorPicker(
                state = state(),
                colors = custom,
                // Nothing is passed to this slot, so anything it reads came through the theme
                // the picker provides.
                hueSlider = { seen = ColorPickerDefaults.currentColors() },
            )
        }
        assertEquals(custom, seen)
    }

    /**
     * The track is one part of a slider's height, and a Material slider will not measure below
     * its own minimum, so the size here has to clear that before the change shows at all.
     */
    @Test
    fun aThemedTrackHeightReachesAChannelSlider() = runComposeUiTest {
        setContent {
            Box(Modifier.testTag("default")) { HueSlider(state = state()) }
        }
        val default = onNodeWithTag("default").getUnclippedBoundsInRoot().height

        setContent {
            ColorPickerTheme(dimensions = ColorPickerDefaults.dimensions(trackHeight = 80.dp)) {
                Box(Modifier.testTag("raised")) { HueSlider(state = state()) }
            }
        }
        val raised = onNodeWithTag("raised").getUnclippedBoundsInRoot().height

        assertTrue(
            raised > default,
            "an 80dp track should make the slider taller than the 16dp default, $raised vs $default",
        )
    }

    @Test
    fun aDisabledPickerRefusesADragOnASlotThatNeverSawEnabled() = runComposeUiTest {
        val state = state()
        val before = state.hslColor.hue
        setContent {
            HslColorPicker(
                state = state,
                enabled = false,
                // The trap this guards: replacing a slider to relabel it, and not forwarding
                // enabled. The picker has to refuse the drag on the slot's behalf.
                hueSlider = {
                    Box(Modifier.testTag("replaced").width(200.dp)) {
                        HueSlider(state = state, label = null, valueLabel = null)
                    }
                },
            )
        }
        onNodeWithTag("replaced").performTouchInput { swipeRight() }
        assertEquals(before, state.hslColor.hue)
    }

    @Test
    fun aDisabledPickerReportsItselfDisabled() = runComposeUiTest {
        setContent {
            HslColorPicker(
                state = state(),
                modifier = Modifier.testTag("picker"),
                enabled = false,
            )
        }
        onNodeWithTag("picker").assertIsNotEnabled()
    }

    @Test
    fun aDisabledSliderRefusesADrag() = runComposeUiTest {
        val state = state()
        val before = state.hslColor.hue
        setContent {
            HueSlider(
                state = state,
                modifier = Modifier.testTag("slider").width(200.dp),
                enabled = false,
                label = null,
                valueLabel = null,
            )
        }
        onNodeWithTag("slider").performTouchInput { swipeRight() }
        assertEquals(before, state.hslColor.hue)
    }

    @Test
    fun anEnabledSliderAcceptsADrag() = runComposeUiTest {
        val state = state()
        val before = state.hslColor.hue
        setContent {
            HueSlider(
                state = state,
                modifier = Modifier.testTag("slider").width(200.dp),
                label = null,
                valueLabel = null,
            )
        }
        onNodeWithTag("slider").performTouchInput { swipeRight() }
        assertTrue(state.hslColor.hue != before, "an enabled slider should have moved")
    }

    @Test
    fun everyPickerTakesTheSameConfiguration() = runComposeUiTest {
        // That this compiles is most of the assertion: the six have to agree on the set.
        val pickers: List<@Composable (ColorPickerState) -> Unit> = listOf(
            { HslColorPicker(it, enabled = false, thumb = {}) },
            { RgbColorPicker(it, enabled = false, thumb = {}) },
            { CmykColorPicker(it, enabled = false, thumb = {}) },
            { LabColorPicker(it, enabled = false, thumb = {}) },
            { OkhslColorPicker(it, enabled = false, thumb = {}) },
            { OkhsvColorPicker(it, enabled = false, thumb = {}) },
        )
        setContent {
            val shared = state()
            pickers.forEach { picker -> Box { picker(shared) } }
        }
    }

    /**
     * Refusing the gesture is half of disabled; the other half is looking it. Measured as how
     * colourful the track still is, since a dimmed gradient over the background loses spread
     * between its channels rather than moving in any one direction.
     */
    @Test
    fun aDisabledSliderIsDrawnDimmer() = runComposeUiTest {
        fun colourfulness(): Float {
            val pixels = onNodeWithTag("slider").captureToImage().toPixelMap()
            var total = 0f
            val y = pixels.height / 2
            for (x in 0 until pixels.width) {
                val c = pixels[x, y]
                total += maxOf(c.red, c.green, c.blue) - minOf(c.red, c.green, c.blue)
            }
            return total / pixels.width
        }

        setContent {
            Box(Modifier.testTag("slider").width(200.dp).background(Color.White)) {
                HueSlider(state = state(), label = null, valueLabel = null)
            }
        }
        val lit = colourfulness()

        setContent {
            Box(Modifier.testTag("slider").width(200.dp).background(Color.White)) {
                HueSlider(state = state(), enabled = false, label = null, valueLabel = null)
            }
        }
        val dimmed = colourfulness()

        assertTrue(
            dimmed < lit * 0.75f,
            "a disabled track should be visibly washed out: $dimmed against $lit",
        )
    }

    /** The planes dim through their own path, not the slider's, so they get their own check. */
    @Test
    fun aDisabledPlaneIsDrawnDimmer() = runComposeUiTest {
        fun colourfulness(): Float {
            val pixels = onNodeWithTag("plane").captureToImage().toPixelMap()
            var total = 0f
            val y = pixels.height / 2
            for (x in 0 until pixels.width) {
                val c = pixels[x, y]
                total += maxOf(c.red, c.green, c.blue) - minOf(c.red, c.green, c.blue)
            }
            return total / pixels.width
        }

        setContent {
            Box(Modifier.background(Color.White)) {
                HslPlane(state = state(), modifier = Modifier.testTag("plane").size(160.dp))
            }
        }
        val lit = colourfulness()

        setContent {
            Box(Modifier.background(Color.White)) {
                HslPlane(
                    state = state(),
                    enabled = false,
                    modifier = Modifier.testTag("plane").size(160.dp),
                )
            }
        }
        val dimmed = colourfulness()

        assertTrue(
            dimmed < lit * 0.75f,
            "a disabled plane should be visibly washed out: $dimmed against $lit",
        )
    }

    /**
     * The two knobs are independent, so all four settings have to behave: dim only, drain only,
     * both, and neither. Colourfulness catches draining; lightness toward the white background
     * catches dimming.
     */
    @Test
    fun theDisabledLookIsWhateverTheColorsAskFor() = runComposeUiTest {
        fun measure(disabledAlpha: Float, disabledSaturation: Float): Pair<Float, Float> {
            setContent {
                ColorPickerTheme(
                    colors = ColorPickerDefaults.colors(
                        disabledAlpha = disabledAlpha,
                        disabledSaturation = disabledSaturation,
                    ),
                ) {
                    Box(Modifier.background(Color.White)) {
                        HslPlane(
                            state = state(),
                            enabled = false,
                            modifier = Modifier.testTag("plane").size(160.dp),
                        )
                    }
                }
            }
            val pixels = onNodeWithTag("plane").captureToImage().toPixelMap()
            var colour = 0f
            var light = 0f
            val y = pixels.height / 2
            for (x in 0 until pixels.width) {
                val c = pixels[x, y]
                colour += maxOf(c.red, c.green, c.blue) - minOf(c.red, c.green, c.blue)
                light += (c.red + c.green + c.blue) / 3f
            }
            return colour / pixels.width to light / pixels.width
        }

        val (untouchedColour, untouchedLight) = measure(1f, 1f)
        val (dimmedColour, dimmedLight) = measure(0.38f, 1f)
        val (drainedColour, drainedLight) = measure(1f, 0f)
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
