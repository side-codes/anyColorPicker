package codes.side.colorpicker.foundation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.test.withKeyDown
import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class BasicColorPlaneTest {

    /** A pair a test holds for a [BasicColorPlane], written back on each report. */
    private class HeldPlane(x: Float = 0.5f, y: Float = 0.5f) {
        var x by mutableFloatStateOf(x)
        var y by mutableFloatStateOf(y)
        var enabled by mutableStateOf(true)
        var finished = 0
    }

    // [held]'s plane, tagged "plane", 200 dp square unless [modifier] says otherwise, with a 20 dp thumb tagged "thumb".
    private fun ComposeUiTest.showPlane(
        held: HeldPlane,
        modifier: Modifier = Modifier.size(200.dp),
        actionLabels: PlaneActionLabels? = PlaneActionLabels("right", "left", "up", "down"),
        thumb: @Composable ColorPlaneScope.() -> Unit = { Box(Modifier.size(20.dp).testTag("thumb")) },
    ) {
        setContent {
            BasicColorPlane(
                xValue = held.x,
                yValue = held.y,
                onValueChange = { x, y ->
                    held.x = x
                    held.y = y
                },
                surface = {},
                modifier = modifier.testTag("plane"),
                enabled = held.enabled,
                onValueChangeFinished = { held.finished++ },
                actionLabels = actionLabels,
                thumb = thumb,
            )
        }
    }

    // Where the centre of the node tagged [tag] sits, in dp from the plane's top left.
    private fun ComposeUiTest.centreOf(tag: String): Offset {
        val plane = onNodeWithTag("plane").getUnclippedBoundsInRoot()
        val node = onNodeWithTag(tag).getUnclippedBoundsInRoot()
        return Offset(((node.left + node.right) / 2 - plane.left).value, ((node.top + node.bottom) / 2 - plane.top).value)
    }

    @Test
    fun aPlaneGivenNoSizeTakesNone() = runComposeUiTest {
        setContent {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                BasicColorPlane(0.5f, 0.5f, { _, _ -> }, surface = {}, modifier = Modifier.testTag("plane"), thumb = {})
            }
        }
        val plane = onNodeWithTag("plane").getUnclippedBoundsInRoot()
        assertEquals(0f, (plane.right - plane.left).value)
        assertEquals(0f, (plane.bottom - plane.top).value)
    }

    @Test
    fun theThumbIsCentredOnThePair() = runComposeUiTest {
        showPlane(HeldPlane(0.25f, 0.75f))
        val centre = centreOf("thumb")
        assertEquals(50f, centre.x, 0.5f)
        assertEquals(50f, centre.y, 0.5f, "y grows upward, so 0.75 is a quarter down")
    }

    @Test
    fun aValueOutsideIsDrawnAtTheNearerEdge() = runComposeUiTest {
        showPlane(HeldPlane(1.5f, -0.5f))
        val centre = centreOf("thumb")
        assertEquals(200f, centre.x, 0.5f)
        assertEquals(200f, centre.y, 0.5f, "a y below 0 sits on the bottom edge")
    }

    @Test
    fun theThumbSeesThePairAndWhetherThePlaneIsEnabled() = runComposeUiTest {
        val held = HeldPlane(0.25f, 0.75f)
        var seen: Triple<Float, Float, Boolean>? = null
        showPlane(
            held,
            thumb = {
                seen = Triple(xFraction, yFraction, enabled)
                Box(Modifier.size(20.dp))
            },
        )
        assertEquals(Triple(0.25f, 0.75f, true), seen)
        held.enabled = false
        waitForIdle()
        assertEquals(Triple(0.25f, 0.75f, false), seen)
    }

    @Test
    fun aTouchReportsBothFractions() = runComposeUiTest {
        val held = HeldPlane()
        showPlane(held)
        onNodeWithTag("plane").performTouchInput { click(Offset(width * 0.25f, height * 0.25f)) }
        assertEquals(0.25f, held.x, 0.01f)
        assertEquals(0.75f, held.y, 0.01f, "y grows upward")
        assertEquals(1, held.finished)
    }

    @Test
    fun arrowsMoveAHundredthAndShiftArrowsATenth() = runComposeUiTest {
        val held = HeldPlane()
        showPlane(held)
        onNodeWithTag("plane").requestFocus()
        onNodeWithTag("plane").performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals(0.51f, held.x, 1e-6f)
        onNodeWithTag("plane").performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.DirectionUp) } }
        assertEquals(0.6f, held.y, 1e-6f)
    }

    @Test
    fun theActionsAreNamedByTheLabels() = runComposeUiTest {
        showPlane(HeldPlane())
        val actions = onNodeWithTag("plane").fetchSemanticsNode().config[SemanticsActions.CustomActions]
        assertEquals(listOf("right", "left", "up", "down"), actions.map { it.label })
    }

    @Test
    fun noLabelsLeaveTheActionsOut() = runComposeUiTest {
        showPlane(HeldPlane(), actionLabels = null)
        assertTrue(SemanticsActions.CustomActions !in onNodeWithTag("plane").fetchSemanticsNode().config)
    }

    @Test
    fun aDisabledPlaneSaysSoAndIgnoresTouch() = runComposeUiTest {
        val held = HeldPlane().apply { enabled = false }
        showPlane(held)
        assertTrue(SemanticsProperties.Disabled in onNodeWithTag("plane").fetchSemanticsNode().config)
        onNodeWithTag("plane").performTouchInput { click(Offset(width * 0.25f, height * 0.25f)) }
        assertEquals(0.5f, held.x)
    }

    @Test
    fun aPlaneCanStepByItsOwnAmount() = runComposeUiTest {
        var x = 0.5f
        var y = 0.5f
        setContent {
            BasicColorPlaneImpl(
                xValue = x,
                yValue = y,
                onValueChange = { newX, newY ->
                    x = newX
                    y = newY
                },
                onStep = { dx, dy, coarse ->
                    val amount = if (coarse) 0.25f else 0.05f
                    x += dx * amount
                    y += dy * amount
                    true
                },
                surface = {},
                modifier = Modifier.testTag("plane").size(200.dp),
                enabled = true,
                onValueChangeFinished = null,
                shape = RectangleShape,
                semanticLabel = null,
                semanticValueText = null,
                actionLabels = null,
                interactionSource = null,
                thumb = {},
            )
        }
        onNodeWithTag("plane").requestFocus()
        onNodeWithTag("plane").performKeyInput { pressKey(Key.DirectionRight) }
        assertEquals(0.55f, x, 1e-6f)
        onNodeWithTag("plane").performKeyInput { withKeyDown(Key.ShiftLeft) { pressKey(Key.DirectionUp) } }
        assertEquals(0.75f, y, 1e-6f)
    }
}
