package codes.side.colorpicker.foundation

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.FocusInteraction
import androidx.compose.foundation.interaction.HoverInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performMouseInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeWithVelocity
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BasicColorSliderPointerTest {

    // A 20 dp thumb that records into [seen] every interaction the slider hands its slots.
    private fun recordingThumb(seen: MutableList<Interaction>): @Composable ColorSliderScope.() -> Unit = {
        LaunchedEffect(interactionSource) { interactionSource.interactions.collect { seen += it } }
        Box(Modifier.size(20.dp))
    }

    private fun List<Interaction>.names(): List<String?> = map { it::class.simpleName }

    @Test
    fun aTapJumpsToThePointTapped() = runComposeUiTest {
        val held = HeldSlider()
        showSlider(held)
        onNodeWithTag("slider").performTouchInput { click(Offset(alongTrack(0.25f), centerY)) }
        assertEquals(0.25f, held.value, 0.01f)
        assertEquals(1, held.finished)
    }

    @Test
    fun aDragWaitsForTheSlopThenFollowsTheFinger() = runComposeUiTest {
        val held = HeldSlider()
        showSlider(held)
        onNodeWithTag("slider").performTouchInput {
            down(Offset(alongTrack(0.25f), centerY))
            moveBy(Offset(viewConfiguration.touchSlop / 2, 0f))
        }
        assertEquals(emptyList(), held.reported, "nothing moves inside the slop")
        onNodeWithTag("slider").performTouchInput { moveTo(Offset(alongTrack(0.75f), centerY)) }
        assertEquals(0.75f, held.value, 0.01f)
        onNodeWithTag("slider").performTouchInput { up() }
        assertEquals(1, held.finished)
    }

    @Test
    fun aDragGoesOnFromTheFingerWhenTheValueMovesUnderIt() = runComposeUiTest {
        val held = HeldSlider()
        showSlider(held)
        onNodeWithTag("slider").performTouchInput {
            down(Offset(alongTrack(0.25f), centerY))
            moveTo(Offset(alongTrack(0.5f), centerY))
        }
        // The caller's own value lands mid-drag.
        held.value = 0.1f
        waitForIdle()
        onNodeWithTag("slider").performTouchInput { moveTo(Offset(alongTrack(0.9f), centerY)) }
        onNodeWithTag("slider").performTouchInput { up() }
        assertEquals(0.9f, held.reported.last(), 0.01f)
    }

    @Test
    fun rightToLeftTakesTheTrackFromTheRight() = runComposeUiTest {
        val held = HeldSlider()
        showSlider(held, direction = LayoutDirection.Rtl)
        onNodeWithTag("slider").performTouchInput { click(Offset(alongTrack(0.25f), centerY)) }
        assertEquals(0.75f, held.value, 0.01f)
    }

    @Test
    fun aTapThatStopsAFlingMovesNothing() = runComposeUiTest {
        val held = HeldSlider()
        val scroll = ScrollState(0)
        setContent {
            Column(Modifier.size(300.dp, 800.dp).verticalScroll(scroll).testTag("column")) {
                Spacer(Modifier.height(1100.dp))
                BasicColorSlider(
                    value = held.value,
                    onValueChange = { held.reported += it },
                    modifier = Modifier.width(220.dp).testTag("slider"),
                    onValueChangeFinished = { held.finished++ },
                    track = { Box(Modifier.fillMaxWidth().height(8.dp)) },
                    thumb = { Box(Modifier.size(20.dp)) },
                )
                Spacer(Modifier.height(3000.dp))
            }
        }
        runOnIdle { scroll.dispatchRawDelta(with(density) { 1000.dp.toPx() }) }
        waitForIdle()
        mainClock.autoAdvance = false
        // A quick flick beside the slider, which the column goes on scrolling after the finger lifts.
        onNodeWithTag("column").performTouchInput {
            val x = with(density) { 260.dp.toPx() }
            swipeWithVelocity(Offset(x, with(density) { 20.dp.toPx() }), Offset(x, with(density) { 220.dp.toPx() }), endVelocity = 4000f, durationMillis = 100)
        }
        mainClock.advanceTimeBy(32)
        assertTrue(scroll.isScrollInProgress, "the column is flinging")
        onNodeWithTag("slider").performTouchInput { click(Offset(alongTrack(0.9f), centerY)) }
        mainClock.advanceTimeBy(100)
        assertEquals(emptyList(), held.reported, "the tap stopped the fling, and set nothing")
        assertEquals(0, held.finished)
    }

    @Test
    fun aVerticalSwipeInAScrollingColumnIsLeftToTheColumn() = runComposeUiTest {
        val held = HeldSlider()
        val scroll = ScrollState(0)
        setContent {
            Column(Modifier.height(300.dp).verticalScroll(scroll)) {
                BasicColorSlider(
                    value = held.value,
                    onValueChange = { held.reported += it },
                    modifier = Modifier.width(220.dp).testTag("slider"),
                    onValueChangeFinished = { held.finished++ },
                    track = {},
                    thumb = { Box(Modifier.size(20.dp)) },
                )
                Spacer(Modifier.height(2000.dp))
            }
        }
        onNodeWithTag("slider").performTouchInput {
            down(center)
            moveBy(Offset(0f, -viewConfiguration.touchSlop * 4))
        }
        onNodeWithTag("slider").performTouchInput { up() }
        waitForIdle()
        assertTrue(scroll.value > 0, "the column scrolled")
        assertEquals(emptyList(), held.reported)
        assertEquals(0, held.finished)
    }

    @Test
    fun aFingerLiftedOffTheSliderIsNoTap() = runComposeUiTest {
        val held = HeldSlider()
        showSlider(held)
        onNodeWithTag("slider").performTouchInput {
            down(Offset(alongTrack(0.25f), centerY))
            moveBy(Offset(0f, 200.dp.toPx()))
        }
        onNodeWithTag("slider").performTouchInput { up() }
        assertEquals(emptyList(), held.reported)
        assertEquals(0, held.finished)
    }

    @Test
    fun aDisabledSliderIgnoresTouch() = runComposeUiTest {
        val held = HeldSlider().apply { enabled = false }
        showSlider(held)
        onNodeWithTag("slider").performTouchInput { click(Offset(alongTrack(0.25f), centerY)) }
        onNodeWithTag("slider").performTouchInput { swipeRight() }
        assertEquals(emptyList(), held.reported)
    }

    @Test
    fun aTapPressesAndReleases() = runComposeUiTest {
        val seen = mutableListOf<Interaction>()
        showSlider(HeldSlider(), thumb = recordingThumb(seen))
        onNodeWithTag("slider").performTouchInput { click(center) }
        waitForIdle()
        assertEquals(listOf("Press", "Release"), seen.names())
    }

    @Test
    fun aDragPressesDragsAndReleases() = runComposeUiTest {
        val seen = mutableListOf<Interaction>()
        showSlider(HeldSlider(), thumb = recordingThumb(seen))
        onNodeWithTag("slider").performTouchInput {
            down(center)
            moveBy(Offset(viewConfiguration.touchSlop * 2, 0f))
        }
        onNodeWithTag("slider").performTouchInput { up() }
        waitForIdle()
        assertTrue(seen[0] is PressInteraction.Press, "$seen")
        assertTrue(seen[1] is DragInteraction.Start, "$seen")
        assertTrue(seen[2] is DragInteraction.Stop, "$seen")
        assertTrue(seen[3] is PressInteraction.Release, "$seen")
        assertEquals(4, seen.size)
    }

    @Test
    fun aFingerHeldStillShowsAPress() = runComposeUiTest {
        val seen = mutableListOf<Interaction>()
        showSlider(HeldSlider(), thumb = recordingThumb(seen))
        onNodeWithTag("slider").performTouchInput { down(center) }
        mainClock.advanceTimeBy(500)
        waitForIdle()
        assertEquals(listOf("Press"), seen.names())
        onNodeWithTag("slider").performTouchInput { up() }
        waitForIdle()
        assertEquals(listOf("Press", "Release"), seen.names())
    }

    @Test
    fun aMousePressShowsAtOnce() = runComposeUiTest {
        val seen = mutableListOf<Interaction>()
        showSlider(HeldSlider(), thumb = recordingThumb(seen))
        // No time passes, so only a press shown on the button's way down is seen.
        mainClock.autoAdvance = false
        onNodeWithTag("slider").performMouseInput {
            moveTo(center)
            press()
        }
        waitForIdle()
        assertTrue(seen.any { it is PressInteraction.Press }, "$seen")
    }

    @Test
    fun aScrollThatStartsOnASliderShowsNoPress() = runComposeUiTest {
        val seen = mutableListOf<Interaction>()
        setContent {
            Column(Modifier.height(300.dp).verticalScroll(ScrollState(0))) {
                BasicColorSlider(
                    value = 0.5f,
                    onValueChange = {},
                    modifier = Modifier.width(220.dp).testTag("slider"),
                    track = {},
                    thumb = recordingThumb(seen),
                )
                Spacer(Modifier.height(2000.dp))
            }
        }
        onNodeWithTag("slider").performTouchInput {
            down(center)
            moveBy(Offset(0f, -viewConfiguration.touchSlop * 4))
        }
        onNodeWithTag("slider").performTouchInput { up() }
        waitForIdle()
        assertEquals(emptyList(), seen, "the thumb shows nothing for a scroll")
    }

    @Test
    fun focusAndHoverReachTheSource() = runComposeUiTest {
        val seen = mutableListOf<Interaction>()
        showSlider(HeldSlider(), thumb = recordingThumb(seen))
        onNodeWithTag("slider").requestFocus()
        onNodeWithTag("slider").performMouseInput { enter(center) }
        waitForIdle()
        assertTrue(seen.any { it is FocusInteraction.Focus }, "focus: $seen")
        assertTrue(seen.any { it is HoverInteraction.Enter }, "hover: $seen")
    }

    @Test
    fun disablingMidDragCancelsThePressAndTheDrag() = runComposeUiTest {
        val held = HeldSlider()
        val seen = mutableListOf<Interaction>()
        showSlider(held, thumb = recordingThumb(seen))
        onNodeWithTag("slider").performTouchInput {
            down(center)
            moveBy(Offset(viewConfiguration.touchSlop * 2, 0f))
        }
        waitForIdle()
        held.enabled = false
        waitForIdle()
        assertTrue(seen.getOrNull(2) is DragInteraction.Cancel, "$seen")
        assertTrue(seen.getOrNull(3) is PressInteraction.Cancel, "$seen")
        assertEquals(1, held.finished, "the drag it cut off reports its end")
    }

    @Test
    fun aSliderInAClickableRowKeepsItsTouches() = runComposeUiTest {
        val held = HeldSlider()
        val row = MutableInteractionSource()
        val rowSaw = mutableListOf<Interaction>()
        var clicks = 0
        setContent {
            LaunchedEffect(row) { row.interactions.collect { rowSaw += it } }
            Box(Modifier.clickable(interactionSource = row, indication = null) { clicks++ }) {
                BasicColorSlider(
                    value = held.value,
                    onValueChange = { held.reported += it },
                    modifier = Modifier.width(220.dp).testTag("slider"),
                    track = {},
                    thumb = { Box(Modifier.size(20.dp)) },
                )
            }
        }
        onNodeWithTag("slider").performTouchInput { click(Offset(alongTrack(0.25f), centerY)) }
        waitForIdle()
        assertEquals(1, held.reported.size, "the slider took the tap")
        assertEquals(0, clicks, "and the row did not")
        assertTrue(rowSaw.none { it is PressInteraction.Press }, "nor did it show a press: $rowSaw")
    }

    @Test
    fun aSliderRemovedMidDragReleasesItsSource() = runComposeUiTest {
        val source = MutableInteractionSource()
        val seen = mutableListOf<Interaction>()
        var shown by mutableStateOf(true)
        setContent {
            LaunchedEffect(source) { source.interactions.collect { seen += it } }
            if (shown) {
                BasicColorSlider(
                    value = 0.5f,
                    onValueChange = {},
                    modifier = Modifier.width(220.dp).testTag("slider"),
                    interactionSource = source,
                    track = {},
                    thumb = { Box(Modifier.size(20.dp)) },
                )
            }
        }
        onNodeWithTag("slider").performTouchInput {
            down(center)
            moveBy(Offset(viewConfiguration.touchSlop * 2, 0f))
        }
        waitForIdle()
        shown = false
        waitForIdle()
        assertTrue(seen.getOrNull(2) is DragInteraction.Cancel, "$seen")
        assertTrue(seen.getOrNull(3) is PressInteraction.Cancel, "$seen")
    }

    @Test
    fun aSourceSwappedMidDragEndsTheDragOnTheOldOne() = runComposeUiTest {
        val first = MutableInteractionSource()
        val second = MutableInteractionSource()
        var current by mutableStateOf(first)
        val toFirst = mutableListOf<Interaction>()
        var finished = 0
        setContent {
            LaunchedEffect(first) { first.interactions.collect { toFirst += it } }
            BasicColorSlider(
                value = 0.5f,
                onValueChange = {},
                modifier = Modifier.width(220.dp).testTag("slider"),
                onValueChangeFinished = { finished++ },
                interactionSource = current,
                track = {},
                thumb = { Box(Modifier.size(20.dp)) },
            )
        }
        onNodeWithTag("slider").performTouchInput {
            down(center)
            moveBy(Offset(viewConfiguration.touchSlop * 2, 0f))
        }
        waitForIdle()
        current = second
        waitForIdle()
        assertTrue(toFirst.getOrNull(2) is DragInteraction.Cancel, "$toFirst")
        assertTrue(toFirst.getOrNull(3) is PressInteraction.Cancel, "$toFirst")
        assertEquals(1, finished, "the drag it cut off reports its end")
    }

    @Test
    fun aNewSourceTakesTheNextGesture() = runComposeUiTest {
        val first = MutableInteractionSource()
        val second = MutableInteractionSource()
        var current by mutableStateOf(first)
        val seen = mutableListOf<Interaction>()
        setContent {
            BasicColorSlider(
                value = 0.5f,
                onValueChange = {},
                modifier = Modifier.width(220.dp).testTag("slider"),
                interactionSource = current,
                track = {},
                thumb = recordingThumb(seen),
            )
        }
        // A first tap starts the gesture handler while the first source is current.
        onNodeWithTag("slider").performTouchInput { click(center) }
        waitForIdle()
        current = second
        waitForIdle()
        seen.clear()
        onNodeWithTag("slider").performTouchInput { click(center) }
        waitForIdle()
        assertTrue(seen.any { it is PressInteraction.Press }, "the second tap reached the new source: $seen")
    }
}
