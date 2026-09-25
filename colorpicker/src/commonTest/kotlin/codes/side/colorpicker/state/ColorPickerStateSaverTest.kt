package codes.side.colorpicker.state

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import codes.side.color.Cmyk
import codes.side.color.ColorSpace
import codes.side.color.DisplayP3
import codes.side.color.Hsl
import codes.side.color.Lab
import codes.side.color.OkLch
import codes.side.color.Okhsl
import codes.side.color.Srgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ColorPickerStateSaverTest {

    private val grey = Srgb(0.5, 0.5, 0.5)

    private fun save(state: ColorPickerState, saver: Saver<ColorPickerState, Any>): Any =
        with(saver) { with(SaverScope { true }) { assertNotNull(save(state)) } }

    private fun roundTrip(state: ColorPickerState, saver: Saver<ColorPickerState, Any> = ColorPickerState.Saver()): ColorPickerState =
        assertNotNull(saver.restore(save(state, saver)))

    @Test
    fun theValueSurvivesExactly() {
        val values = listOf(
            Srgb(0.1, 0.2, 0.3, 0.4),
            Hsl(210.0, 40.0, 60.0, 0.5),
            Okhsl(140.0, 0.9, 0.5),
            Cmyk(0.1, 0.2, 0.3, 0.4, 0.5),
            Lab(50.0, 150.0, -150.0),
            OkLch(null, 0.1, 200.0, null),
        )
        for (value in values) assertEquals(value, roundTrip(ColorPickerState(value)).value)
    }

    @Test
    fun theRememberedHuesSurvive() {
        val state = ColorPickerState(Hsl(200.0, 80.0, 50.0))
        state.value = grey
        val restored = roundTrip(state)
        assertEquals(200.0, restored.displayValue(Hsl.H))
        assertEquals(state.displayValue(OkLch.H), restored.displayValue(OkLch.H))
    }

    @Test
    fun anAppSpaceRestoresThroughKnownSpaces() {
        val p3Hsl = ColorSpace.hsl("--hsl-p3", DisplayP3)
        val state = ColorPickerState(p3Hsl.color(doubleArrayOf(30.0, 80.0, 50.0)))
        assertEquals(state.value, roundTrip(state, ColorPickerState.Saver(listOf(p3Hsl))).value)
        val unknown = ColorPickerState.Saver()
        assertNull(unknown.restore(save(state, unknown)))
    }

    @Test
    fun anAppFamilysHueSurvivesWithoutItsSpace() {
        val p3Hsl = ColorSpace.hsl("--hsl-p3", DisplayP3)
        val state = ColorPickerState(p3Hsl.color(doubleArrayOf(30.0, 80.0, 50.0)))
        state.value = grey
        assertEquals(30.0, roundTrip(state).displayValue(p3Hsl.channels[0]))
    }

    @Test
    fun anUnknownSpaceRestoresAsNull() {
        assertNull(ColorPickerState.Saver().restore(listOf(1, "--nowhere", 0.0, 0.0, 0.0, 0, 1.0, false)))
    }

    @Test
    fun anotherFormatVersionRestoresAsNull() {
        assertNull(ColorPickerState.Saver().restore(listOf(2, "srgb", 0.0, 0.0, 0.0, 0, 1.0, false)))
    }

    @Test
    fun aWrongSizeRestoresAsNull() {
        assertNull(ColorPickerState.Saver().restore(listOf(1, "srgb", 0.0, 0.0)))
        assertNull(ColorPickerState.Saver().restore(listOf(1, "srgb", 0.0, 0.0, 0.0, 0, 1.0, false, "oklab")))
    }

    @Test
    fun componentsTheSpaceRefusesRestoreAsNull() {
        assertNull(ColorPickerState.Saver().restore(listOf(1, "okhsl", 0.0, 2.0, 0.5, 0, 1.0, false)))
    }

    @Test
    fun aHueOutsideTheCircleRestoresAsNull() {
        assertNull(ColorPickerState.Saver().restore(listOf(1, "srgb", 0.0, 0.0, 0.0, 0, 1.0, false, "oklab", 400.0)))
    }

    @Test
    fun theEarlierFloatArrayFormatRestoresAsNull() {
        assertNull(ColorPickerState.Saver().restore(floatArrayOf(0f, 200f, 0.5f, 0.5f, 1f, 0f, 200f, 200f)))
    }

    @Test
    fun twoSpacesWithOneIdAreRefused() {
        assertFailsWith<IllegalArgumentException> {
            ColorPickerState.Saver(listOf(ColorSpace.hsl("--x", Srgb), ColorSpace.hsl("--x", DisplayP3)))
        }
    }
}
