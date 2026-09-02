package codes.side.colorpicker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import kotlin.test.Test
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
}
