package codes.side.colorpicker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PixelMap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.width
import codes.side.color.Hsl
import codes.side.color.Okhsl
import codes.side.color.Okhsv
import codes.side.colorpicker.state.ColorPickerState
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Layout and painting behavior that only shows up in a real composition.
 */
@OptIn(ExperimentalTestApi::class)
class ComponentRenderingTest {

    @Test
    fun swatchHasSizeWhenTheParentGivesItNone() = runComposeUiTest {
        setContent {
            // A vertically scrolling parent passes unbounded height down, which is where a
            // fillMaxSize child collapses the swatch to nothing.
            Column(Modifier.verticalScroll(rememberScrollState())) {
                ColorSwatch(color = Color.Red, modifier = Modifier.testTag("swatch"))
            }
        }

        onNodeWithTag("swatch").assertHeightIsAtLeast(1.dp)
    }

    @Test
    fun thumbStaysVisibleWhenTheColorIsFullyTransparent() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                AlphaSlider(
                    state = ColorPickerState(Hsl(0.0, 100.0, 50.0, 0.0)),
                    modifier = Modifier.size(width = 300.dp, height = 80.dp).testTag("alpha"),
                )
            }
        }

        // At alpha 0 the thumb must still be painted. Scan the left edge, where the thumb
        // sits at value 0; a transparent thumb leaves only checkerboard there.
        val pixels = onNodeWithTag("alpha").captureToImage().toPixelMap()
        var sawThumb = false
        for (x in 0 until pixels.width / 8) {
            for (y in 0 until pixels.height) {
                val p = pixels[x, y]
                // The thumb is the current hue at full opacity: strongly red.
                if (p.red > 0.7f && p.green < 0.35f && p.blue < 0.35f) sawThumb = true
            }
        }
        assertTrue(sawThumb, "no opaque thumb painted at alpha 0 — it is invisible")
    }

    @Test
    fun aCustomThumbReplacesTheDefaultAndReceivesTheInteractionSource() = runComposeUiTest {
        // Mid grey cannot occur anywhere in a fully saturated hue track, so finding it
        // proves the caller's thumb was painted rather than the default handle.
        val marker = Color(0xFF7F7F7F)
        var received: InteractionSource? = null

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                ChannelSlider(
                    state = ColorPickerState(Hsl(180.0, 100.0, 50.0)),
                    channel = Hsl.H,
                    modifier = Modifier.size(width = 300.dp, height = 60.dp).testTag("hue"),
                    thumb = { source ->
                        received = source
                        Box(Modifier.size(24.dp).background(marker))
                    },
                )
            }
        }

        assertNotNull(received, "the thumb slot was never given an InteractionSource")

        val pixels = onNodeWithTag("hue").captureToImage().toPixelMap()
        var sawMarker = false
        for (x in 0 until pixels.width) {
            for (y in 0 until pixels.height) {
                val p = pixels[x, y]
                if (p.red in 0.45f..0.55f && p.green in 0.45f..0.55f && p.blue in 0.45f..0.55f) {
                    sawMarker = true
                }
            }
        }
        assertTrue(sawMarker, "the custom thumb was not painted — the slot is ignored")
    }

    @Test
    fun aCustomPlaneThumbKeepsTheSizeItMeasuresTo() = runComposeUiTest {
        // Bigger than PlaneThumbSize: a plane that squeezed the slot into its own default
        // diameter would report 24dp here.
        setContent {
            ChannelPlane(
                state = ColorPickerState(Hsl(0.0, 50.0, 50.0)),
                x = Hsl.S,
                y = Hsl.L,
                modifier = Modifier.size(200.dp).testTag("plane"),
                thumb = { Box(Modifier.size(48.dp).testTag("thumb")) },
            )
        }

        val thumb = onNodeWithTag("thumb").getUnclippedBoundsInRoot()
        assertEquals(48f, thumb.width.value, 0.5f, "the custom thumb was resized")
        // Centred on saturation 50, lightness 50 of a 200dp plane.
        assertEquals(100f, (thumb.left + thumb.right).value / 2f, 0.5f)
        assertEquals(100f, (thumb.top + thumb.bottom).value / 2f, 0.5f)
    }

    @Test
    fun aSmallCustomPlaneThumbIsCentredOnItsValue() = runComposeUiTest {
        setContent {
            ChannelPlane(
                state = ColorPickerState(Hsl(0.0, 50.0, 50.0)),
                x = Hsl.S,
                y = Hsl.L,
                modifier = Modifier.size(200.dp).testTag("plane"),
                thumb = { Box(Modifier.size(8.dp).testTag("thumb")) },
            )
        }

        val thumb = onNodeWithTag("thumb").getUnclippedBoundsInRoot()
        assertEquals(8f, thumb.width.value, 0.5f)
        // A thumb positioned by the default diameter instead of its own would sit at 88dp.
        assertEquals(96f, thumb.left.value, 0.5f, "the thumb is offset from the value it marks")
    }

    @Test
    fun thePlaneIndicatorSurvivesTheCorner() = runComposeUiTest {
        // Zero saturation at full lightness parks the indicator on the top left corner,
        // where a shape clip covering the whole plane rounds most of it away. A solid
        // marker in a padded frame counts the half that hangs off the surface too.
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                // The tag goes before the padding, so the captured node is the whole
                // frame and not just the content inside it.
                Box(Modifier.testTag("frame").background(Color.White).padding(24.dp)) {
                    ChannelPlane(
                        state = ColorPickerState(Hsl(0.0, 0.0, 100.0)),
                        x = Hsl.S,
                        y = Hsl.L,
                        modifier = Modifier.size(200.dp),
                        thumb = { Box(Modifier.size(24.dp).background(Color.Magenta)) },
                    )
                }
            }
        }

        val px = onNodeWithTag("frame").captureToImage().toPixelMap()
        var marker = 0
        for (x in 0 until 50) {
            for (y in 0 until 50) {
                val c = px[x, y]
                if (c.red > 0.9f && c.blue > 0.9f && c.green < 0.1f) marker++
            }
        }
        // 24dp square, so 576px whole. Clipped to the surface it would be the quarter
        // inside the corner, and the shape's rounding takes a bite out of even that.
        assertTrue(marker > 500, "the indicator is clipped away in the corner, $marker px")
    }

    // The pixel a fraction across and down, from the top left.
    private fun PixelMap.at(fx: Float, fy: Float): Color = this[(fx * (width - 1)).toInt(), (fy * (height - 1)).toInt()]

    // Both raster planes here are black along the bottom, and draw nothing until their raster arrives.
    private fun ComposeUiTest.rasterDrawn(tag: String): Boolean =
        onNodeWithTag(tag).captureToImage().toPixelMap().at(0.5f, 0.99f).let { it.alpha > 0.99f && it.red < 0.06f }

    @Test
    fun theOkhslPlaneRendersItsCorners() = runComposeUiTest {
        // Lightness 1 parks the indicator on the top edge, clear of the sample points below.
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                ChannelPlane(
                    state = ColorPickerState(Okhsl(29.2, 1.0, 1.0)),
                    x = Okhsl.S,
                    y = Okhsl.L,
                    modifier = Modifier.size(200.dp).testTag("okhslPlane"),
                )
            }
        }
        waitUntil(timeoutMillis = 5_000) { rasterDrawn("okhslPlane") }

        val px = onNodeWithTag("okhslPlane").captureToImage().toPixelMap()

        val bottom = px.at(0.5f, 0.99f)
        assertTrue(
            bottom.red < 0.06f && bottom.green < 0.06f && bottom.blue < 0.06f,
            "lightness 0 is black at every saturation, was $bottom",
        )
        // Not literally the corner: the default plane shape clips a true (0.01, 0.01)
        // sample away. Lightness 1 is white regardless of saturation by construction, so
        // mid saturation clears the clip and exercises the same code path.
        val top = px.at(0.5f, 0.01f)
        assertTrue(
            top.red > 0.94f && top.green > 0.94f && top.blue > 0.94f,
            "lightness 1 is white at every saturation, was $top",
        )
        val midLeft = px.at(0.01f, 0.5f)
        assertTrue(
            abs(midLeft.red - midLeft.green) < 0.02f && abs(midLeft.green - midLeft.blue) < 0.02f,
            "zero saturation is grey at every lightness, was $midLeft",
        )
        // Hue 29.2 at full saturation is where sRGB red lives.
        val midRight = px.at(0.99f, 0.44f)
        assertTrue(
            midRight.red > midRight.green && midRight.green > midRight.blue,
            "the right edge should be the most colourful the hue reaches, was $midRight",
        )
    }

    @Test
    fun theOkhsvPlaneRendersItsCorners() = runComposeUiTest {
        // Value 1, saturation 0 parks the indicator in the top left, clear of the samples.
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                ChannelPlane(
                    state = ColorPickerState(Okhsv(29.2, 0.0, 1.0)),
                    x = Okhsv.S,
                    y = Okhsv.V,
                    modifier = Modifier.size(200.dp).testTag("okhsvPlane"),
                )
            }
        }
        waitUntil(timeoutMillis = 5_000) { rasterDrawn("okhsvPlane") }

        val px = onNodeWithTag("okhsvPlane").captureToImage().toPixelMap()

        val bottom = px.at(0.5f, 0.99f)
        assertTrue(
            bottom.red < 0.06f && bottom.green < 0.06f && bottom.blue < 0.06f,
            "value 0 is black at every saturation, was $bottom",
        )
        // Not literally the corner: the default plane shape clips a true (0.99, 0.01)
        // sample away. This point is still high in both saturation and value, clear of
        // the clip. Unlike Okhsl, the top row runs grey to vivid rather than white to
        // white, so it should read as a clearly dominant, saturated red rather than a
        // pale tint.
        val topRight = px.at(0.92f, 0.08f)
        assertTrue(
            topRight.red > 0.85f && topRight.red - topRight.green > 0.4f && topRight.blue < 0.3f,
            "high saturation and value is the most vivid form of the hue, was $topRight",
        )
        val midLeft = px.at(0.01f, 0.5f)
        assertTrue(
            abs(midLeft.red - midLeft.green) < 0.02f && abs(midLeft.green - midLeft.blue) < 0.02f,
            "zero saturation is grey at every value, was $midLeft",
        )
    }
}
