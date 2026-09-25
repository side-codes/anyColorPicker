package codes.side.colorpicker.state

import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.Color
import codes.side.color.ColorSpace
import codes.side.color.ColorValue
import codes.side.color.DisplayP3
import codes.side.color.Hsl
import codes.side.color.OkLch
import codes.side.color.Okhsl
import codes.side.color.Okhsv
import codes.side.color.Srgb
import codes.side.color.isInGamut
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ColorPickerStateTest {

    private val grey = Srgb(0.5, 0.5, 0.5)

    @Test
    fun holdsItsInitialValue() {
        val red = Srgb(1.0, 0.0, 0.0)
        assertSame(red, ColorPickerState(red).value)
    }

    @Test
    fun aComposeColorStartsInSrgb() {
        assertEquals(Srgb(1.0, 0.0, 0.0), ColorPickerState(Color.Red).value)
    }

    @Test
    fun colorIsTheValueInSrgb() {
        assertEquals(Color.Red, ColorPickerState(Srgb(1.0, 0.0, 0.0)).color)
    }

    @Test
    fun aChannelReadsInItsOwnSpace() {
        val state = ColorPickerState(Srgb(1.0, 0.0, 0.0))
        assertEquals(1.0, state[Srgb.R])
        assertNear(0.0, state[Hsl.H])
        assertNear(100.0, state[Hsl.S])
    }

    @Test
    fun setLeavesTheValueInTheChannelsSpace() {
        val red = Srgb(1.0, 0.0, 0.0)
        val state = ColorPickerState(red)
        state[OkLch.C] = 0.1
        assertEquals(OkLch, state.value.space)
        assertEquals(0.1, state.value[OkLch.C])
        assertNear(red.to(OkLch)[OkLch.L]!!, state.value[OkLch.L])
        assertNear(red.to(OkLch)[OkLch.H]!!, state.value[OkLch.H])
    }

    @Test
    fun setNullWritesNone() {
        val state = ColorPickerState(OkLch(0.5, 0.1, 200.0))
        state[OkLch.L] = null
        assertTrue(state.value.isMissing(OkLch.L))
    }

    @Test
    fun raisingAGreysSaturationBringsBackItsHue() {
        val state = ColorPickerState(Hsl(200.0, 80.0, 50.0))
        state.value = grey
        state[Hsl.S] = 50.0
        assertEquals(Hsl(200.0, 50.0, 50.0), state.value)
    }

    @Test
    fun aGreyColoredInAnAppSpaceNeverSeenTakesTheRememberedHue() {
        val p3Hsl = ColorSpace.hsl("--hsl-p3", DisplayP3)
        val state = ColorPickerState(Hsl(200.0, 80.0, 50.0))
        state.value = grey
        state[p3Hsl.S] = 50.0
        assertNear(Hsl(200.0, 100.0, 50.0).to(p3Hsl)[p3Hsl.H]!!, state.value[p3Hsl.H], message = "not red")
    }

    @Test
    fun aGreyWithNoHueRememberedTakesHueZeroWhenColored() {
        val state = ColorPickerState(grey)
        state[Okhsl.S] = 0.5
        assertEquals(0.0, state.value[Okhsl.H], "written, not left as none")
    }

    @Test
    fun anEditOfTheHueItselfKeepsWhatItWrites() {
        val state = ColorPickerState(Hsl(200.0, 0.0, 50.0))
        state[Hsl.H] = null
        assertTrue(state.value.isMissing(Hsl.H))
    }

    @Test
    fun aMissingComponentOtherThanTheHueStaysMissing() {
        val state = ColorPickerState(OkLch(null, 0.1, 200.0))
        state[OkLch.C] = 0.2
        assertTrue(state.value.isMissing(OkLch.L))
        assertEquals(0.0, state.displayValue(OkLch.L))
    }

    @Test
    fun displayValueShowsTheRememberedHueForAGrey() {
        val state = ColorPickerState(Hsl(200.0, 80.0, 50.0))
        state.value = grey
        assertNull(state[Hsl.H])
        assertEquals(200.0, state.displayValue(Hsl.H))
    }

    @Test
    fun aDeliberateHueAtZeroSaturationIsLeftAlone() {
        val state = ColorPickerState(Hsl(200.0, 80.0, 50.0))
        state[Hsl.S] = 0.0
        assertEquals(200.0, state.value[Hsl.H])
    }

    @Test
    fun aNeutralWrittenInHslReadsBackAsItself() {
        val white = Hsl(0.0, 0.0, 100.0)
        val state = ColorPickerState(Hsl(200.0, 80.0, 50.0))
        state.value = white
        assertEquals(white, state.value)
        assertEquals(0.0, state.displayValue(Hsl.H))
    }

    @Test
    fun hueDraggedToZeroOnAGreyStaysAtZero() {
        val state = ColorPickerState(Hsl(200.0, 80.0, 50.0))
        state[Hsl.S] = 0.0
        state[Hsl.H] = 0.0
        state[Hsl.S] = 100.0
        assertEquals(0.0, state.value[Hsl.H], "raising saturation gives red, not the old hue")
    }

    @Test
    fun eachOkSpaceReadsBackWhatWasWrittenToIt() {
        for (white in listOf(Okhsl(0.0, 0.0, 1.0), Okhsv(0.0, 0.0, 1.0), OkLch(1.0, 0.0, 0.0))) {
            val state = ColorPickerState(Okhsl(140.0, 0.9, 0.5))
            state.value = white
            assertEquals(white, state.value)
        }
    }

    @Test
    fun anInvalidChannelValueThrows() {
        val state = ColorPickerState(grey)
        assertFailsWith<IllegalArgumentException> { state[Srgb.R] = Double.NaN }
        assertFailsWith<IllegalArgumentException> { state[Okhsl.S] = 1.5 }
    }

    @Test
    fun editingInOkhslBringsAColorIntoSrgb() {
        val state = ColorPickerState(DisplayP3(1.0, 0.0, 0.0))
        state[Okhsl.L] = 0.6
        assertTrue(state.value.isInGamut(Srgb.gamut))
    }

    @Test
    fun anEqualWriteChangesNothing() {
        val state = ColorPickerState(Srgb(1.0, 0.0, 0.0))
        state[Srgb.G] = 0.25
        Snapshot.sendApplyNotifications()
        val before = state.value
        var notifications = 0
        val observer = Snapshot.registerApplyObserver { changed, _ -> if (changed.isNotEmpty()) notifications++ }
        try {
            state[Srgb.G] = 0.25
            state.value = Srgb(1.0, 0.25, 0.0)
            Snapshot.sendApplyNotifications()
        } finally {
            observer.dispose()
        }
        assertEquals(0, notifications)
        assertSame(before, state.value)
    }

    @Test
    fun readsInsideAReadOnlySnapshotDoNotWrite() {
        val state = ColorPickerState(Hsl(200.0, 80.0, 50.0))
        state.value = grey
        val snapshot = Snapshot.takeSnapshot()
        try {
            snapshot.enter {
                state.value
                state[Hsl.H]
                assertEquals(200.0, state.displayValue(Hsl.H))
                state.color
                state.hslColor
                state.okhslColor
                state.pickerColor
                state.argbInt
            }
        } finally {
            snapshot.dispose()
        }
    }

    @Test
    fun isInteractingOutlastsTheFirstOfTwoGestures() {
        val state = ColorPickerState(grey)
        state.beginInteraction()
        state.beginInteraction()
        state.endInteraction()
        assertTrue(state.isInteracting)
        state.endInteraction()
        assertFalse(state.isInteracting)
    }

    @Test
    fun isInteractingDoesNotGoNegative() {
        val state = ColorPickerState(grey)
        state.endInteraction()
        state.beginInteraction()
        assertTrue(state.isInteracting)
    }

    @Test
    fun anEditWithoutASinkWritesTheValue() {
        val state = ColorPickerState(Srgb(1.0, 0.0, 0.0))
        state.edit(Srgb.G, 0.5)
        assertEquals(Srgb(1.0, 0.5, 0.0), state.value)
    }

    @Test
    fun anEditGoesToTheSinkInstead() {
        val red = Srgb(1.0, 0.0, 0.0)
        val state = ColorPickerState(red)
        var reported: ColorValue? = null
        state.onEdit = { reported = it }
        state.edit(Srgb.G, 0.5)
        assertEquals(Srgb(1.0, 0.5, 0.0), reported)
        assertSame(red, state.value)
    }

    @Test
    fun anAlphaEditGoesToTheSinkToo() {
        val red = Srgb(1.0, 0.0, 0.0)
        val state = ColorPickerState(red)
        var reported: ColorValue? = null
        state.onEdit = { reported = it }
        state.editAlpha(0.25)
        assertEquals(Srgb(1.0, 0.0, 0.0, 0.25), reported)
        assertSame(red, state.value)
    }

    @Test
    fun anAlphaEditKeepsTheSpace() {
        val state = ColorPickerState(Hsl(200.0, 80.0, 50.0))
        state.editAlpha(0.5)
        assertEquals(Hsl(200.0, 80.0, 50.0, 0.5), state.value)
    }

    @Test
    fun displayComponentsAreWhatEachChannelsSliderShows() {
        val state = ColorPickerState(Hsl(200.0, 80.0, 50.0))
        state.value = grey
        val shown = state.displayComponents(Hsl)
        assertEquals(200.0, shown[Hsl.H.index], "a grey's hue is the one remembered")
        for (channel in Hsl.channels) assertEquals(state.displayValue(channel), shown[channel.index], "$channel")
    }

    @Test
    fun aTwoChannelEditIsOneWrite() {
        val state = ColorPickerState(Srgb(1.0, 0.0, 0.0))
        val reported = mutableListOf<ColorValue>()
        state.onEdit = { reported += it }
        state.edit(Hsl.S, 30.0, Hsl.L, 40.0)
        assertEquals(listOf(Hsl(0.0, 30.0, 40.0)), reported)
    }

    @Test
    fun aTwoChannelEditOnAGreyTakesTheRememberedHue() {
        val state = ColorPickerState(Hsl(200.0, 80.0, 50.0))
        state.value = grey
        state.edit(Hsl.S, 60.0, Hsl.L, 40.0)
        assertEquals(Hsl(200.0, 60.0, 40.0), state.value)
    }

    @Test
    fun editsBuildOnTheLastEmission() {
        val state = ColorPickerState(Hsl(200.0, 50.0, 50.0))
        state.onEdit = { state.emit(it) }
        state.edit(Hsl.S, 60.0)
        state.edit(Hsl.L, 40.0)
        assertEquals(Hsl(200.0, 60.0, 40.0), state.lastEmission)
        assertEquals(Hsl(200.0, 50.0, 50.0), state.value, "an emission is not applied")
    }

    @Test
    fun writingTheValueAnswersTheEmission() {
        val state = ColorPickerState(Hsl(200.0, 50.0, 50.0))
        state.onEdit = { state.emit(it) }
        state.edit(Hsl.S, 60.0)
        state.value = Hsl(200.0, 55.0, 50.0)
        assertNull(state.lastEmission)
        assertEquals(Hsl(200.0, 55.0, 50.0), state.editBase)
    }

    @Test
    fun anEqualWriteStillAnswersTheEmission() {
        val red = Srgb(1.0, 0.0, 0.0)
        val state = ColorPickerState(red)
        state.onEdit = { state.emit(it) }
        state.editAlpha(0.5)
        state.value = red
        assertNull(state.lastEmission, "a caller that keeps its value has answered too")
        assertSame(red, state.editBase)
    }

    @Test
    fun displayComponentsOfAnotherValueUseTheRememberedHue() {
        val state = ColorPickerState(Hsl(200.0, 80.0, 50.0))
        val shown = state.displayComponents(Hsl, of = grey)
        assertEquals(listOf(200.0, 0.0, 50.0), shown.toList())
    }
}
