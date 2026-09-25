package codes.side.colorpicker.ui

import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import codes.side.color.ColorValue
import codes.side.color.DisplayP3
import codes.side.color.Hsl
import codes.side.color.OkLch
import codes.side.color.Okhsl
import codes.side.color.Srgb
import codes.side.color.compose.toColorValue
import codes.side.color.compose.toComposeColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.assertNear
import kotlin.math.abs
import kotlin.math.round
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import androidx.compose.ui.graphics.colorspace.ColorSpaces as ComposeSpaces

/**
 * A picker over a value its caller holds draws only that value and reports every change at once. The
 * risk in any two-way binding is the two ends updating each other forever, or one overwriting the
 * other; these watch both.
 */
@OptIn(ExperimentalTestApi::class)
class ControlledPickerTest {

    private val teal = Hsl(200.0, 80.0, 50.0)

    private fun ComposeUiTest.hueProgress(): Float =
        sliderNamed("Hue").fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].current

    private fun ComposeUiTest.setProgress(slider: String, fraction: Float) {
        sliderNamed(slider).performSemanticsAction(SemanticsActions.SetProgress) { it(fraction) }
    }

    @Test
    fun aChangeIsReportedInTheSameEventInThePickersSpace() = runComposeUiTest {
        var held by mutableStateOf<ColorValue>(Srgb(0.2, 0.4, 0.8))
        val reported = mutableListOf<ColorValue>()
        setContent {
            ColorPicker(
                held,
                {
                    reported += it
                    held = it
                },
                space = Hsl,
                showPlane = false,
            )
        }
        setProgress("Hue", 0.5f)
        assertEquals(1, reported.size)
        assertEquals(Hsl, reported.single().space)
        assertNear(180.0, reported.single()[Hsl.H])
    }

    @Test
    fun aRejectedChangeNeverMovesTheThumb() = runComposeUiTest {
        var reports = 0
        setContent { ColorPicker(teal, { reports++ }, space = Hsl, showPlane = false) }
        setProgress("Hue", 0.5f)
        waitForIdle()
        assertEquals(200f / 360f, hueProgress(), 1e-6f)
        onNodeWithText("200°").assertExists()
        assertEquals(1, reports)
    }

    @Test
    fun aRoundedChangeShowsRounded() = runComposeUiTest {
        var held by mutableStateOf<ColorValue>(Hsl(180.0, 80.0, 50.0))
        setContent {
            ColorPicker(held, { held = it.with(Hsl.H, round(it[Hsl.H]!! / 30.0) * 30.0) }, space = Hsl, showPlane = false)
        }
        setProgress("Hue", 185f / 360f)
        waitForIdle()
        onNodeWithText("180°").assertExists()
        assertEquals(0.5f, hueProgress(), 1e-6f)
    }

    @Test
    fun aCallerThatTransformsEveryChangeSettles() = runComposeUiTest {
        var held by mutableStateOf<ColorValue>(Hsl(100.0, 80.0, 50.0))
        var reports = 0
        setContent {
            ColorPicker(
                held,
                {
                    reports++
                    held = it.with(Hsl.H, it[Hsl.H]!! + 1.0)
                },
                space = Hsl,
                showPlane = false,
            )
        }
        setProgress("Hue", 0.5f)
        waitForIdle()
        onNodeWithText("181°").assertExists()
        assertEquals(1, reports, "the value the caller chose is not reported back to it")
    }

    @Test
    fun aValueWrittenInIsShownWithoutBeingReportedBack() = runComposeUiTest {
        var held by mutableStateOf<ColorValue>(teal)
        var reports = 0
        setContent {
            ColorPicker(
                held,
                {
                    reports++
                    held = it
                },
                space = Hsl,
                showPlane = false,
            )
        }
        waitForIdle()
        held = Hsl(40.0, 50.0, 60.0)
        waitForIdle()
        onNodeWithText("40°").assertExists()
        assertEquals(0, reports)
        assertEquals(Hsl(40.0, 50.0, 60.0), held, "and it is not overwritten on the way back")
    }

    @Test
    fun aSlowCallersValueIsDrawnWhileTheDragGoesOnFromTheFinger() = runComposeUiTest {
        var held by mutableStateOf<ColorValue>(Hsl(0.0, 80.0, 50.0))
        val reported = mutableListOf<ColorValue>()
        setContent { ColorPicker(held, { reported += it }, Modifier.width(400.dp), space = Hsl, showPlane = false) }
        sliderNamed("Hue").performTouchInput {
            down(Offset(width * 0.25f, centerY))
            moveTo(Offset(width * 0.5f, centerY))
        }
        waitForIdle()
        // A value from the caller's store lands in the middle of the drag.
        held = Hsl(30.0, 80.0, 50.0)
        waitForIdle()
        assertEquals(30f / 360f, hueProgress(), 1e-4f, "the late value is drawn")
        // Lifted in a gesture of its own: a move sent with the up in one batch never reaches the slider.
        sliderNamed("Hue").performTouchInput { moveTo(Offset(width * 0.75f, centerY)) }
        sliderNamed("Hue").performTouchInput { up() }
        val last = reported.last()[Hsl.H]!!
        assertTrue(last in 250.0..290.0, "the drag goes on from the finger at three quarters, not from the late value: $last")
    }

    @Test
    fun aColorCallerKeepsTheExactEmittedValue() = runComposeUiTest {
        var color by mutableStateOf(Okhsl(30.0, 0.5, 0.5).toComposeColor())
        lateinit var state: ColorPickerState
        setContent {
            state = rememberControlledPickerState(
                color,
                Okhsl,
                onChange = { color = it },
                toValue = { it.toColorValue() },
                fromValue = { it.toComposeColor() },
            )
        }
        runOnUiThread { state.edit(Okhsl.S, 0.537) }
        waitForIdle()
        assertEquals(Okhsl, state.value.space, "not rebuilt from the 8-bit color")
        assertEquals(0.537, state[Okhsl.S])
    }

    @Test
    fun anEditOutsideSrgbSurvivesAColorRoundTrip() = runComposeUiTest {
        var color by mutableStateOf(OkLch(0.7, 0.1, 150.0).toComposeColor())
        setContent { ColorPicker(color, { color = it }, space = OkLch, showPlane = false) }
        setProgress("Chroma", 0.875f)
        waitForIdle()
        onNodeWithText("0.350").assertExists()
    }

    @Test
    fun aGreyEchoedLateByAColorCallerKeepsTheHue() = runComposeUiTest {
        var color by mutableStateOf(teal.toComposeColor())
        val reported = mutableListOf<Color>()
        // A caller writing through a store: its echo arrives after the report, not in it.
        setContent { ColorPicker(color, { reported += it }, space = Hsl, showPlane = false) }
        setProgress("Saturation", 0f)
        waitForIdle()
        color = reported.last()
        waitForIdle()
        setProgress("Saturation", 0.5f)
        val hue = reported.last().toColorValue().to(Hsl)[Hsl.H]!!
        assertTrue(abs(hue - 200.0) < 2.0, "raising saturation should bring back hue 200, not red: $hue")
    }

    @Test
    fun aValueInAnotherSpaceIsShownAndEditsComeBackInThePickersSpace() = runComposeUiTest {
        val p3Red = DisplayP3(1.0, 0.0, 0.0)
        var held by mutableStateOf<ColorValue>(p3Red)
        val reported = mutableListOf<ColorValue>()
        setContent {
            ColorPicker(
                held,
                {
                    reported += it
                    held = it
                },
                space = Okhsl,
                showPlane = false,
            )
        }
        waitForIdle()
        assertEquals(p3Red, held, "shown without being rewritten")
        assertTrue(reported.isEmpty(), "or reported")
        setProgress("Lightness", 0.5f)
        waitForIdle()
        assertEquals(Okhsl, reported.single().space)
    }

    @Test
    fun aComposeColorInDisplayP3SettlesWithoutReports() = runComposeUiTest {
        val p3 = Color(1f, 0f, 0f, 1f, ComposeSpaces.DisplayP3)
        var reports = 0
        setContent { ColorPicker(p3, { reports++ }, space = Hsl, showPlane = false) }
        // Reaching idle is the assertion that the two ends do not keep rewriting each other.
        waitForIdle()
        assertEquals(0, reports)
    }

    @Test
    fun aSlotThatWritesTheStateIsReportedLikeAnEdit() = runComposeUiTest {
        var held by mutableStateOf<ColorValue>(teal)
        val reported = mutableListOf<ColorValue>()
        setContent {
            ColorPicker(
                held,
                {
                    reported += it
                    held = it
                },
                space = Hsl,
                showPlane = false,
                // A replaced slot is handed the picker's state, and writing it is how a custom control edits.
                channelSlider = { state, channel ->
                    if (channel === Hsl.H) {
                        TextButton(onClick = { state[Hsl.H] = 120.0 }) { Text("Grün") }
                        TextButton(onClick = { state.value = Hsl(40.0, 50.0, 60.0) }) { Text("Ocker") }
                    } else {
                        ChannelSlider(state, channel)
                    }
                },
            )
        }
        onNodeWithText("Grün").performClick()
        waitForIdle()
        onNodeWithText("Ocker").performClick()
        waitForIdle()
        assertEquals(listOf<ColorValue>(Hsl(120.0, 80.0, 50.0), Hsl(40.0, 50.0, 60.0)), reported)
        assertEquals(Hsl(40.0, 50.0, 60.0), held)
    }

    @Test
    fun aDisabledPickerReportsNothing() = runComposeUiTest {
        var reports = 0
        setContent { ColorPicker(teal, { reports++ }, Modifier.width(300.dp), space = Hsl, showPlane = false, enabled = false) }
        sliderNamed("Hue").performTouchInput { swipeRight() }
        waitForIdle()
        assertEquals(0, reports)
    }
}
