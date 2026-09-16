package codes.side.colorpicker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.state.ColorPickerState
import kotlin.math.round
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * The overload that takes a colour and a callback, for a caller holding the value themselves.
 * The risk in any two-way binding is the two ends updating each other forever, so that is what
 * these watch.
 */
@OptIn(ExperimentalTestApi::class)
class HoistedColorTest {

    @Test
    fun aChangeMadeInsideIsReportedOut() = runComposeUiTest {
        // Driven through the state rather than through a gesture: the gesture reaching a slider
        // is PickerConfigurationTest's job, and what needs proving here is that a change inside
        // reaches the caller.
        var held by mutableStateOf(HslColor(hue = 200f, saturation = 0.8f, lightness = 0.5f))
        var reported: HslColor? = null
        lateinit var state: ColorPickerState
        setContent {
            state = rememberHoistedColorState(
                color = held,
                read = { hslColor },
                write = { updateFromHsl(it) },
                onColorChange = { held = it; reported = it },
            )
        }
        waitForIdle()
        runOnUiThread { state.updateHue(120f) }
        waitForIdle()
        assertEquals(120f, reported?.hue, "the caller should have been told")
        assertEquals(120f, held.hue, "and their value should now hold it")
    }

    @Test
    fun aValueWrittenInIsShownWithoutBeingReportedBack() = runComposeUiTest {
        var held by mutableStateOf(HslColor(hue = 200f, saturation = 0.8f, lightness = 0.5f))
        var reports = 0
        setContent {
            HslColorPicker(
                color = held,
                onColorChange = { held = it; reports++ },
                modifier = Modifier.testTag("picker").width(300.dp),
            )
        }
        waitForIdle()
        assertEquals(0, reports, "showing a colour is not a change")

        held = HslColor(hue = 40f, saturation = 0.5f, lightness = 0.6f)
        waitForIdle()
        assertEquals(0, reports, "a colour written in is not a change either")
        assertEquals(40f, held.hue, "and it is not overwritten on the way back")
    }

    @Test
    fun theBindingSettles() = runComposeUiTest {
        // If the two ends disagreed about what counts as a change they would update each other
        // indefinitely; reaching idle at all is the assertion.
        var held by mutableStateOf(HslColor(hue = 10f, saturation = 0.3f, lightness = 0.4f))
        var reports = 0
        setContent {
            Box {
                HslColorPicker(
                    color = held,
                    onColorChange = { held = it; reports++ },
                    modifier = Modifier.testTag("picker").width(300.dp),
                )
            }
        }
        repeat(3) {
            held = HslColor(hue = 10f + it * 30f, saturation = 0.3f, lightness = 0.4f)
            waitForIdle()
        }
        assertEquals(0, reports, "nothing the caller wrote should have come back as a change")
    }

    @Test
    fun aRejectedChangeIsPutBack() = runComposeUiTest {
        // A caller that validates and says no. The picker cannot be left showing a colour its
        // owner refused, or the swatch and the caller's value disagree with nothing on screen
        // to say which one is real.
        val held = HslColor(hue = 200f, saturation = 0.8f, lightness = 0.5f)
        var reports = 0
        lateinit var state: ColorPickerState
        setContent {
            state = rememberHoistedColorState(
                color = held,
                read = { hslColor },
                write = { updateFromHsl(it) },
                onColorChange = { reports++ },
            )
        }
        waitForIdle()
        runOnUiThread { state.updateHue(120f) }
        waitForIdle()
        assertEquals(200f, state.hslColor.hue, "the caller's value is the one that survives")
        assertEquals(1, reports, "and they were asked once, not once per frame")
    }

    @Test
    fun aChangeRoundedByTheCallerIsShownRounded() = runComposeUiTest {
        // Snapping to steps is the common shape of this: the callback answers with a value the
        // picker never sent, and it happens to equal the one the caller already held, so there
        // is no change for an effect keyed on the value to notice.
        var held by mutableStateOf(HslColor(hue = 180f, saturation = 0.8f, lightness = 0.5f))
        lateinit var state: ColorPickerState
        setContent {
            state = rememberHoistedColorState(
                color = held,
                read = { hslColor },
                write = { updateFromHsl(it) },
                onColorChange = { held = it.copy(hue = round(it.hue / 30f) * 30f) },
            )
        }
        waitForIdle()
        runOnUiThread { state.updateHue(185f) }
        waitForIdle()
        assertEquals(180f, state.hslColor.hue, "the picker shows the step, not the finger")
        assertEquals(180f, held.hue)
    }

    @Test
    fun aCallerThatTransformsEveryChangeStillSettles() = runComposeUiTest {
        var held by mutableStateOf(HslColor(hue = 180f, saturation = 0.8f, lightness = 0.5f))
        var reports = 0
        lateinit var state: ColorPickerState
        setContent {
            state = rememberHoistedColorState(
                color = held,
                read = { hslColor },
                write = { updateFromHsl(it) },
                onColorChange = { held = it.copy(hue = it.hue + 1f); reports++ },
            )
        }
        waitForIdle()
        runOnUiThread { state.updateHue(185f) }
        waitForIdle()
        assertEquals(186f, state.hslColor.hue)
        assertEquals(186f, held.hue)
        assertEquals(1, reports, "the value the caller chose is not reported back at them")
    }
}
