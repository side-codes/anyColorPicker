package codes.side.colorpicker.foundation

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import codes.side.colorpicker.ui.LocalPickerEnabled
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException

// One percent of the field per arrow press and ten with shift held, the step the M3 slider
// uses for its own arrow keys. The accessibility actions take the coarse step whatever is
// held: there is no modifier to hold in a screen reader's action menu, and a hundred taps to
// cross the field is not a way to pick a colour.
internal const val PlaneKeyStep: Float = 0.01f
internal const val PlaneCoarseKeyStep: Float = 0.1f

/** The direction an arrow key asks for, as (dx, dy), or `null` if [event] is not an arrow press. */
private fun planeKeyDirection(event: KeyEvent): Pair<Int, Int>? {
    if (event.type != KeyEventType.KeyDown) return null
    return when (event.key) {
        Key.DirectionLeft -> -1 to 0
        Key.DirectionRight -> 1 to 0
        // yValue grows upward, so the up arrow adds.
        Key.DirectionUp -> 0 to 1
        Key.DirectionDown -> 0 to -1
        else -> null
    }
}

/** The fraction across a plane [width] pixels wide that a pointer at [x] sits at. */
internal fun planeXFraction(x: Float, width: Int): Float =
    if (width <= 0) 0f else (x / width).coerceIn(0f, 1f)

/** The fraction up a plane [height] pixels tall that a pointer at [y] sits at. */
internal fun planeYFraction(y: Float, height: Int): Float =
    if (height <= 0) 0.5f else (1f - y / height).coerceIn(0f, 1f)

/** What a [BasicColorPlane]'s thumb draws from. Only the library implements it. */
@Stable
public sealed interface ColorPlaneScope {
    /** Where the thumb is across the plane: `0` at the left edge, `1` at the right. */
    public val xFraction: Float

    /** Where the thumb is up the plane: `0` at the bottom, `1` at the top. */
    public val yFraction: Float

    /** False while the plane, or a picker around it, is disabled. */
    public val enabled: Boolean

    /** The plane's drag and focus interactions. */
    public val interactionSource: InteractionSource
}

/**
 * A two-dimensional picker over a pair of values in `0..1` with no look of its own: [surface] paints
 * the field and [thumb] marks the current pair, reading it from [ColorPlaneScope].
 *
 * [xValue] runs left to right and [yValue] bottom to top, so `yValue = 1f` is the top edge. Dragging
 * reports both at once, which is what lets a caller write two channels in a single update and leave
 * the rest of the colour alone. A value outside `0..1` is drawn at the nearer edge.
 *
 * Unlike the sliders, the surface is not mirrored in right-to-left layouts. It is a map of a colour
 * space rather than a progress control, and mirroring it would make the x channel grow leftwards here
 * while it still grows rightwards on the hue slider beside it.
 *
 * The plane imposes no size: it takes the size its modifier gives it, and none when given none.
 * Pressing the surface takes focus, so the arrow keys carry on from where the finger left off. An
 * arrow moves the pair by one percent and Shift with an arrow by ten, and an arrow the plane cannot
 * use at an edge is passed on, so focus can leave.
 *
 * @param surface paints the field, filling the plane. It is drawn under [thumb] and clipped to [shape].
 * @param onValueChangeFinished called when a drag ends, and after each key press or accessibility
 * action that changes the value.
 * @param shape clips [surface]. The thumb is drawn outside it, so it stays whole at the edges.
 * @param semanticLabel accessibility description of the surface; `null` by default, since the plane
 * does not know what it shows.
 * @param semanticValueText accessibility announcement of the current pair of values.
 * @param actionLabels names the four accessibility actions that move the plane by ten percent, since a
 * screen reader has no gesture for a surface with two degrees of freedom; `null` omits them and leaves
 * the plane readable but not adjustable.
 * @param interactionSource receives the plane's drag and focus interactions, which [thumb] reads from
 * [ColorPlaneScope.interactionSource]. Note that if `null` is provided, interactions will still happen
 * internally.
 * @param thumb marks the current pair. The plane centres it on the pair at whatever size it measures
 * to. Keyboard focus that shows nowhere on screen leaves its user guessing, so a thumb is expected to
 * mark the source's focus somehow.
 */
@Composable
public fun BasicColorPlane(
    xValue: Float,
    yValue: Float,
    onValueChange: (x: Float, y: Float) -> Unit,
    surface: DrawScope.() -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
    shape: Shape = RectangleShape,
    semanticLabel: String? = null,
    semanticValueText: String? = null,
    actionLabels: PlaneActionLabels? = ColorPickerStrings.current.planeAxisActions(),
    interactionSource: MutableInteractionSource? = null,
    thumb: @Composable ColorPlaneScope.() -> Unit,
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    BasicColorPlaneImpl(
        xValue = xValue,
        yValue = yValue,
        onValueChange = onValueChange,
        onStep = { dx, dy, coarse ->
            val step = if (coarse) PlaneCoarseKeyStep else PlaneKeyStep
            val newX = (xValue + dx * step).coerceIn(0f, 1f)
            val newY = (yValue + dy * step).coerceIn(0f, 1f)
            if (newX == xValue && newY == yValue) {
                false
            } else {
                currentOnValueChange(newX, newY)
                true
            }
        },
        surface = surface,
        modifier = modifier,
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
        shape = shape,
        semanticLabel = semanticLabel,
        semanticValueText = semanticValueText,
        actionLabels = actionLabels,
        interactionSource = interactionSource,
        thumb = thumb,
    )
}

/**
 * [BasicColorPlane] with the size of a key or accessibility step left to [onStep], which gets the
 * direction, whether the coarse step was asked for, and returns false when the step changes nothing.
 */
@Composable
internal fun BasicColorPlaneImpl(
    xValue: Float,
    yValue: Float,
    onValueChange: (x: Float, y: Float) -> Unit,
    onStep: (dx: Int, dy: Int, coarse: Boolean) -> Boolean,
    surface: DrawScope.() -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    onValueChangeFinished: (() -> Unit)?,
    shape: Shape,
    semanticLabel: String?,
    semanticValueText: String?,
    actionLabels: PlaneActionLabels?,
    interactionSource: MutableInteractionSource?,
    thumb: @Composable ColorPlaneScope.() -> Unit,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    // The gesture handler outlives any one composition, so it reads the callbacks and the
    // painter through these rather than capturing the values it was built with.
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnFinished by rememberUpdatedState(onValueChangeFinished)
    val currentSurface by rememberUpdatedState(surface)
    val currentOnStep by rememberUpdatedState(onStep)
    val active = enabled && LocalPickerEnabled.current
    val xFraction = sliderFraction(xValue)
    val yFraction = sliderFraction(yValue)
    val slots = remember(xFraction, yFraction, active, source) { PlaneSlots(xFraction, yFraction, active, source) }

    // False when the plane is already against that edge. An arrow the plane keeps is an arrow
    // focus cannot leave on, and a device driven by a D-pad alone has nothing else to press.
    fun step(dx: Int, dy: Int, coarse: Boolean): Boolean {
        if (!currentOnStep(dx, dy, coarse)) return false
        currentOnFinished?.invoke()
        return true
    }

    Layout(
        content = {
            // The shape clips the surface alone. Clipping the whole plane would take the
            // indicator with it, and at a corner the rounding leaves almost none of the
            // ring behind — the one place a picker has to show where the colour came from.
            Box(
                modifier = Modifier
                    .clip(shape)
                    .drawBehind { currentSurface() },
            )
            // A bare Box, so the indicator measures to its own size. Giving the wrapper a
            // size instead squeezes a larger custom thumb into the default diameter and
            // strands a smaller one in the corner of it, off the value it marks.
            Box { slots.thumb() }
        },
        modifier = modifier
            .semantics {
                semanticLabel?.let { contentDescription = it }
                semanticValueText?.let { stateDescription = it }
                if (!active) disabled()
                if (active && actionLabels != null) {
                    customActions = listOf(
                        CustomAccessibilityAction(actionLabels.increaseX) {
                            step(1, 0, coarse = true)
                        },
                        CustomAccessibilityAction(actionLabels.decreaseX) {
                            step(-1, 0, coarse = true)
                        },
                        CustomAccessibilityAction(actionLabels.increaseY) {
                            step(0, 1, coarse = true)
                        },
                        CustomAccessibilityAction(actionLabels.decreaseY) {
                            step(0, -1, coarse = true)
                        },
                    )
                }
            }
            .onKeyEvent { event ->
                if (!active) return@onKeyEvent false
                val (dx, dy) = planeKeyDirection(event) ?: return@onKeyEvent false
                step(dx, dy, coarse = event.isShiftPressed)
            }
            .focusRequester(focusRequester)
            .focusable(active, source)
            // Keyed on active so the handler is torn down rather than left running with a
            // flag it checks: a gesture in flight when the plane is disabled ends there. Keyed
            // on the source too, or a handler kept across a new one goes on reporting to the old.
            .pointerInput(active, source) {
                if (!active) return@pointerInput
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    // Pressing takes focus, so the arrow keys carry on from where the finger
                    // left off rather than doing nothing until something is tabbed to.
                    focusRequester.requestFocus()
                    // Emitted at once rather than from a coroutine, so they stay in order and a drag
                    // torn down with the plane still ends on the caller's source.
                    val press = DragInteraction.Start()
                    source.tryEmit(press)
                    try {
                        currentOnValueChange(
                            planeXFraction(down.position.x, size.width),
                            planeYFraction(down.position.y, size.height),
                        )
                        down.consume()

                        val completed = drag(down.id) { change ->
                            currentOnValueChange(
                                planeXFraction(change.position.x, size.width),
                                planeYFraction(change.position.y, size.height),
                            )
                            change.consume()
                        }

                        currentOnFinished?.invoke()
                        source.tryEmit(if (completed) DragInteraction.Stop(press) else DragInteraction.Cancel(press))
                    } catch (e: CancellationException) {
                        // Torn down mid-drag, as when the plane is disabled under the finger: the drag
                        // has still ended, and a thumb drawn from the source would otherwise stay dragged.
                        source.tryEmit(DragInteraction.Cancel(press))
                        currentOnFinished?.invoke()
                        throw e
                    }
                }
            },
    ) { measurables, constraints ->
        // The plane imposes no size: it takes the minimum it is given, the exact size a caller asked
        // for, or nothing when it asked for none.
        val width = constraints.minWidth
        val height = constraints.minHeight
        val surfacePlaceable = measurables[0].measure(Constraints.fixed(width, height))
        val indicator = measurables[1].measure(Constraints(maxWidth = width, maxHeight = height))

        layout(width, height) {
            surfacePlaceable.place(0, 0)
            // place, not placeRelative: the surface, the field and the pointer mapping are
            // all unmirrored, so an indicator that flipped in right-to-left layouts would
            // sit on the opposite colour from the one it points at.
            indicator.place(
                x = (xFraction * width - indicator.width / 2f).roundToInt(),
                y = ((1f - yFraction) * height - indicator.height / 2f).roundToInt(),
            )
        }
    }
}

private class PlaneSlots(
    override val xFraction: Float,
    override val yFraction: Float,
    override val enabled: Boolean,
    override val interactionSource: InteractionSource,
) : ColorPlaneScope
