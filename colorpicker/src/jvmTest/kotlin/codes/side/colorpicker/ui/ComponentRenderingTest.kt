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
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
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
                    state = ColorPickerState(
                        HslColor(hue = 0f, saturation = 1f, lightness = 0.5f, alpha = 0f),
                    ),
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
    fun hueTrackIsMirroredInRightToLeft() = runComposeUiTest {
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                HueSlider(
                    state = ColorPickerState(
                        HslColor(
                            hue = 180f,
                            saturation = 1f,
                            lightness = 0.5f,
                        ),
                    ),
                    coloringMode = ColoringMode.Contextual,
                    modifier = Modifier.size(width = 300.dp, height = 60.dp).testTag("hue"),
                )
            }
        }

        val pixels = onNodeWithTag("hue").captureToImage().toPixelMap()
        val y = pixels.height / 2
        // Mirrored: yellow (hue 60) sits 1/6 from the RIGHT edge.
        val yellow = pixels[pixels.width - pixels.width / 6 - 1, y]
        assertTrue(
            yellow.red > 0.7f && yellow.green > 0.7f && yellow.blue < 0.3f,
            "expected yellow 1/6 from the right in RTL, got $yellow",
        )
    }

    @Test
    fun aCustomThumbReplacesTheDefaultAndReceivesTheInteractionSource() = runComposeUiTest {
        // Mid grey cannot occur anywhere in a fully saturated hue track, so finding it
        // proves the caller's thumb was painted rather than the Material 3 default.
        val marker = Color(0xFF7F7F7F)
        var received: InteractionSource? = null

        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                HueSlider(
                    state = ColorPickerState(
                        HslColor(
                            hue = 180f,
                            saturation = 1f,
                            lightness = 0.5f,
                        ),
                    ),
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
    fun thePlaneRendersHslExactlyAtItsCorners() = runComposeUiTest {
        // Hue 0 with the thumb parked at the top right, so no sample point sits under it.
        setContent {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                SaturationLightnessPlane(
                    state = ColorPickerState(
                        HslColor(hue = 0f, saturation = 1f, lightness = 1f)
                    ),
                    modifier = Modifier.size(200.dp).testTag("plane"),
                )
            }
        }

        val px = onNodeWithTag("plane").captureToImage().toPixelMap()
        fun at(fx: Float, fy: Float) = px[
            (fx * (px.width - 1)).toInt(),
            (fy * (px.height - 1)).toInt(),
        ]

        // A horizontal grey-to-hue ramp under a white-to-transparent-to-black overlay
        // reproduces HSL only if every one of these lands.
        val topRight = at(0.75f, 0.02f)
        assertTrue(
            topRight.red > 0.9f && topRight.green > 0.9f && topRight.blue > 0.9f,
            "top edge should be white, was $topRight",
        )
        val bottomRight = at(0.75f, 0.98f)
        assertTrue(
            bottomRight.red < 0.1f && bottomRight.green < 0.1f && bottomRight.blue < 0.1f,
            "bottom edge should be black, was $bottomRight",
        )
        val midRight = at(0.98f, 0.5f)
        assertTrue(
            midRight.red > 0.9f && midRight.green < 0.1f && midRight.blue < 0.1f,
            "the pure hue belongs at full saturation, mid lightness, was $midRight",
        )
        val midLeft = at(0.02f, 0.5f)
        assertTrue(
            midLeft.red in 0.4f..0.6f && midLeft.green in 0.4f..0.6f && midLeft.blue in 0.4f..0.6f,
            "zero saturation at mid lightness is grey, was $midLeft",
        )
    }

    /** Centre x of the indicator's white ring on the plane's middle row, in pixels. */
    private fun ComposeUiTest.indicatorCentreOnMiddleRow(): Float {
        val px = onNodeWithTag("plane").captureToImage().toPixelMap()
        val row = px.height / 2
        val ring = (0 until px.width).filter {
            val c = px[it, row]
            c.red > 0.95f && c.green > 0.95f && c.blue > 0.95f
        }
        assertTrue(ring.isNotEmpty(), "no indicator ring on the middle row")
        return (ring.first() + ring.last()) / 2f
    }

    @Test
    fun thePlaneIndicatorStaysOnItsColorInRightToLeft() {
        // The surface, the gradient and the pointer mapping are all unmirrored. An
        // indicator placed relative to the layout direction lands on the opposite side,
        // pointing at grey while the state reads full saturation.
        fun centre(direction: LayoutDirection): Float {
            var centre = 0f
            runComposeUiTest {
                setContent {
                    CompositionLocalProvider(LocalLayoutDirection provides direction) {
                        SaturationLightnessPlane(
                            state = ColorPickerState(
                                HslColor(hue = 240f, saturation = 0.9f, lightness = 0.5f)
                            ),
                            modifier = Modifier.size(200.dp).testTag("plane"),
                        )
                    }
                }
                centre = indicatorCentreOnMiddleRow()
            }
            return centre
        }

        val ltr = centre(LayoutDirection.Ltr)
        val rtl = centre(LayoutDirection.Rtl)
        assertTrue(ltr > 150f, "saturation 0.9 belongs near the right edge, was $ltr")
        assertEquals(ltr, rtl, 1f, "the indicator moved between layout directions")
    }

    @Test
    fun aCustomPlaneThumbKeepsTheSizeItMeasuresTo() = runComposeUiTest {
        // Bigger than PlaneThumbSize: a plane that squeezed the slot into its own default
        // diameter would report 24dp here.
        setContent {
            SaturationLightnessPlane(
                state = ColorPickerState(HslColor(hue = 0f, saturation = 0.5f, lightness = 0.5f)),
                modifier = Modifier.size(200.dp).testTag("plane"),
                thumb = { Box(Modifier.size(48.dp).testTag("thumb")) },
            )
        }

        val thumb = onNodeWithTag("thumb").getUnclippedBoundsInRoot()
        assertEquals(48f, thumb.width.value, 0.5f, "the custom thumb was resized")
        // Centred on saturation 0.5, lightness 0.5 of a 200dp plane.
        assertEquals(100f, (thumb.left + thumb.right).value / 2f, 0.5f)
        assertEquals(100f, (thumb.top + thumb.bottom).value / 2f, 0.5f)
    }

    @Test
    fun aSmallCustomPlaneThumbIsCentredOnItsValue() = runComposeUiTest {
        setContent {
            SaturationLightnessPlane(
                state = ColorPickerState(HslColor(hue = 0f, saturation = 0.5f, lightness = 0.5f)),
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
                    SaturationLightnessPlane(
                        state = ColorPickerState(HslColor(hue = 0f, saturation = 0f, lightness = 1f)),
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
}
