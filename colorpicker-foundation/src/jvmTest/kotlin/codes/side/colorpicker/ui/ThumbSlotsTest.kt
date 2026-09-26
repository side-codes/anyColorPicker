package codes.side.colorpicker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import codes.side.color.ColorChannel
import codes.side.color.Hsl
import codes.side.color.Srgb
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** The Material components' thumb slots read their component's scope. */
@OptIn(ExperimentalTestApi::class)
class ThumbSlotsTest {

    // Red at full saturation and half lightness, 30% opaque; every thumb paints it opaque.
    private val translucentRed = Hsl(0.0, 100.0, 50.0, 0.3)
    private val opaqueRed = Color(0xFFFF0000)

    @Test
    fun aChannelSlidersThumbReadsItsChannelItsValueAndTheColorUnderIt() = runComposeUiTest {
        var seen: Triple<ColorChannel, Double, Color>? = null
        setContent {
            ChannelSlider(
                ColorPickerState(translucentRed),
                Hsl.H,
                coloringMode = ColoringMode.Contextual,
                thumb = {
                    seen = Triple(channel, value, thumbColor)
                    Box(Modifier.size(4.dp))
                },
            )
        }
        assertEquals(Triple(Hsl.H, 0.0, opaqueRed), seen)
    }

    @Test
    fun anAlphaSlidersThumbReadsTheAlphaAndTheOpaqueColor() = runComposeUiTest {
        var seen: Pair<Double, Color>? = null
        setContent {
            AlphaSlider(
                ColorPickerState(Srgb(1.0, 0.0, 0.0, 0.25)),
                thumb = {
                    seen = alpha to thumbColor
                    Box(Modifier.size(4.dp))
                },
            )
        }
        assertEquals(0.25 to opaqueRed, seen)
    }

    @Test
    fun aColorSlidersThumbReadsItsPositionAndItsColorOpaque() = runComposeUiTest {
        var seen: Pair<Float, Color>? = null
        setContent {
            ColorSlider(
                value = 0.5f,
                onValueChange = {},
                trackColors = listOf(Color.Black, Color.White),
                thumbColor = opaqueRed.copy(alpha = 0.3f),
                thumb = {
                    seen = fraction to thumbColor
                    Box(Modifier.size(4.dp))
                },
            )
        }
        assertEquals(0.5f to opaqueRed, seen)
    }

    @Test
    fun aChannelPlanesThumbReadsItsChannelsAndTheColorUnderIt() = runComposeUiTest {
        var seen: Triple<ColorChannel, ColorChannel, Color>? = null
        setContent {
            ChannelPlane(
                ColorPickerState(translucentRed),
                Hsl.S,
                Hsl.L,
                Modifier.size(200.dp),
                thumb = {
                    seen = Triple(x, y, thumbColor)
                    Box(Modifier.size(4.dp))
                },
            )
        }
        assertEquals(Triple(Hsl.S, Hsl.L, opaqueRed), seen)
    }

    @Test
    fun aColorPlanesThumbReadsItsPosition() = runComposeUiTest {
        var seen: Pair<Float, Float>? = null
        setContent {
            ColorPlane(
                xValue = 0.25f,
                yValue = 0.75f,
                onValueChange = { _, _ -> },
                surface = {},
                modifier = Modifier.size(200.dp),
                thumb = {
                    seen = xFraction to yFraction
                    Box(Modifier.size(4.dp))
                },
            )
        }
        assertEquals(0.25f to 0.75f, seen)
    }

    @Test
    fun aPickersThumbReachesEverySliderWithItsScope() = runComposeUiTest {
        setContent {
            ColorPicker(
                ColorPickerState(Hsl(200.0, 80.0, 50.0)),
                space = Hsl,
                thumb = { Box(Modifier.size(4.dp).testTag(if (enabled) "thumb" else "disabled")) },
            )
        }
        onAllNodesWithTag("thumb", useUnmergedTree = true).assertCountEquals(4)
    }

    @Test
    fun aColorSlidersDefaultThumbIsOpaqueOverATranslucentColor() = runComposeUiTest {
        // The track is white, so red at the start can only be the thumb, and only painted opaque.
        setContent {
            ColorSlider(
                value = 0f,
                onValueChange = {},
                trackColors = listOf(Color.White, Color.White),
                thumbColor = opaqueRed.copy(alpha = 0f),
                modifier = Modifier.size(width = 300.dp, height = 60.dp).testTag("slider"),
            )
        }
        val pixels = onNodeWithTag("slider").captureToImage().toPixelMap()
        var sawThumb = false
        for (x in 0 until pixels.width / 8) {
            for (y in 0 until pixels.height) {
                val p = pixels[x, y]
                if (p.red > 0.9f && p.green < 0.1f && p.blue < 0.1f) sawThumb = true
            }
        }
        assertTrue(sawThumb, "the default thumb took the translucent parameter, not the scope's opaque color")
    }
}
