package codes.side.colorpicker.foundation

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.SaverScope
import codes.side.color.ColorSpace
import codes.side.color.DisplayP3
import codes.side.color.Hsl
import codes.side.color.Hsv
import codes.side.color.OkLch
import codes.side.color.Okhsl
import codes.side.color.Srgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ColorPickerDialogStateTest {

    private val spaces = listOf(Okhsl, OkLch, Hsv, Srgb)
    private val teal = Hsv(190.0, 70.0, 60.0)

    private fun roundTrip(
        state: ColorPickerDialogState,
        saver: Saver<ColorPickerDialogState, Any> = ColorPickerDialogState.Saver(),
    ): ColorPickerDialogState? {
        val saved = with(saver) { with(SaverScope { true }) { assertNotNull(save(state)) } }
        return saver.restore(saved)
    }

    @Test
    fun itOpensInTheValuesOwnSpaceWhenItIsOffered() {
        assertSame(Hsv, ColorPickerDialogState(teal, spaces).space)
    }

    @Test
    fun itOpensInTheFirstSpaceWhenTheValuesIsNotOffered() {
        assertSame(Okhsl, ColorPickerDialogState(DisplayP3(0.2, 0.4, 0.6), spaces).space)
    }

    @Test
    fun anUntouchedStateReturnsTheOriginalItself() {
        val p3 = DisplayP3(1.0, 0.0, 0.0)
        val state = ColorPickerDialogState(p3, spaces)
        assertFalse(state.isModified)
        assertSame(p3, state.result)
    }

    @Test
    fun showingAnotherSpaceDoesNotConvertTheColor() {
        val p3 = DisplayP3(1.0, 0.0, 0.0)
        val state = ColorPickerDialogState(p3, spaces)
        state.space = Srgb
        assertSame(p3, state.pickerState.value)
        assertFalse(state.isModified)
        assertSame(p3, state.result)
    }

    @Test
    fun anEditIsReturnedInTheSpaceOfTheChannelMoved() {
        val state = ColorPickerDialogState(teal, spaces)
        state.space = OkLch
        state.pickerState[OkLch.L] = 0.5
        assertTrue(state.isModified)
        assertSame(OkLch, state.result.space)
        assertEquals(0.5, state.result[OkLch.L])
    }

    @Test
    fun revertPutsTheOriginalBack() {
        val state = ColorPickerDialogState(teal, spaces)
        state.pickerState[Hsv.S] = 10.0
        state.revert()
        assertFalse(state.isModified)
        assertSame(teal, state.result)
        assertEquals(teal, state.pickerState.value)
    }

    @Test
    fun anEditBackToTheOriginalIsNoEdit() {
        val state = ColorPickerDialogState(teal, spaces)
        state.pickerState[Hsv.S] = 10.0
        state.pickerState[Hsv.S] = 70.0
        assertFalse(state.isModified)
        assertSame(teal, state.result)
    }

    @Test
    fun theSpacesAreACopy() {
        val offered = mutableListOf<ColorSpace>(Okhsl, Hsv)
        val state = ColorPickerDialogState(teal, offered)
        offered += Srgb
        assertEquals(listOf(Okhsl, Hsv), state.spaces)
    }

    @Test
    fun aSpaceListedTwiceIsRefused() {
        assertFailsWith<IllegalArgumentException> { ColorPickerDialogState(teal, listOf(Hsv, Okhsl, Hsv)) }
    }

    @Test
    fun anInitialSpaceNotOfferedIsRefused() {
        assertFailsWith<IllegalArgumentException> { ColorPickerDialogState(teal, spaces, Hsl) }
    }

    @Test
    fun noSpacesAreRefused() {
        assertFailsWith<IllegalArgumentException> { ColorPickerDialogState(teal, emptyList(), Hsv) }
        // The default initial space is the first of none.
        assertFailsWith<NoSuchElementException> { ColorPickerDialogState(teal, emptyList()) }
    }

    @Test
    fun showingASpaceNotOfferedIsRefused() {
        val state = ColorPickerDialogState(teal, spaces)
        assertFailsWith<IllegalArgumentException> { state.space = Hsl }
        assertSame(Hsv, state.space)
    }

    @Test
    fun theEditTheSpaceAndTheOriginalSurviveSaving() {
        val state = ColorPickerDialogState(teal, spaces)
        state.space = OkLch
        state.pickerState[OkLch.C] = 0.1
        val restored = assertNotNull(roundTrip(state))
        assertEquals(spaces, restored.spaces)
        assertSame(OkLch, restored.space)
        assertEquals(state.pickerState.value, restored.pickerState.value)
        assertEquals(teal, restored.original)
        assertTrue(restored.isModified)
    }

    @Test
    fun theRememberedHuesSurviveSaving() {
        val state = ColorPickerDialogState(teal, spaces)
        state.pickerState.value = Srgb(0.5, 0.5, 0.5)
        assertEquals(190.0, assertNotNull(roundTrip(state)).pickerState.displayValue(Hsv.H))
    }

    @Test
    fun anAppSpaceRestoresOnlyThroughKnownSpaces() {
        val p3Hsl = ColorSpace.hsl("--hsl-p3", DisplayP3)
        val state = ColorPickerDialogState(p3Hsl.color(doubleArrayOf(30.0, 80.0, 50.0)), listOf(p3Hsl, Okhsl))
        assertSame(p3Hsl, roundTrip(state, ColorPickerDialogState.Saver(listOf(p3Hsl)))?.space)
        assertNull(roundTrip(state))
    }

    @Test
    fun aListOfAnotherFormatRestoresAsNull() {
        assertNull(ColorPickerDialogState.Saver().restore(arrayListOf<Any>(2, "okhsl")))
    }
}
