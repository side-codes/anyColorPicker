package codes.side.colorpicker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import codes.side.colorpicker.foundation.ColorPickerStrings
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

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

/**
 * Two-dimensional picker over a pair of colour channels, both in `0..1`.
 *
 * [xValue] runs left to right and [yValue] bottom to top, so `yValue = 1f` is the top edge.
 * Dragging reports both at once, which is what lets a caller write two channels in a single
 * update and leave the rest of the colour alone.
 *
 * Unlike the sliders, the surface is not mirrored in right-to-left layouts. It is a map of a
 * colour space rather than a progress control, and mirroring it would make the x channel grow
 * leftwards here while it still grows rightwards on the hue slider beside it.
 *
 * @param surface paints the field, filling the whole drawing area. It is drawn under the
 * position indicator and clipped to [ColorPickerShapes.planeShape].
 * @param semanticLabel accessibility description of the surface; `null` by default, since the plane
 * does not know what it shows. A [ChannelPlane] names its channels.
 * @param semanticValueText accessibility announcement of the current pair of values.
 * @param actionLabels names the four accessibility actions that move the plane, since a screen
 * reader has no gesture for a surface with two degrees of freedom; `null` omits them and leaves
 * the plane readable but not adjustable. Arrow keys move it by one percent and shift-arrow by
 * ten, and pressing the surface takes focus so they land where they were aimed.
 * @param interactionSource receives the plane's drag and focus interactions, and is what [thumb] is
 * handed. Note that if `null` is provided, interactions will still happen internally.
 * @param thumb optional replacement for the position indicator, receiving the surface's
 * [InteractionSource] so it can react to being dragged — and to being focused, which the
 * default indicator marks with a second ring and a replacement is expected to mark somehow,
 * since keyboard focus that shows nowhere on screen leaves its user guessing. The plane centres whatever it is
 * given on the current pair of values at whatever size that composable measures to, and
 * draws it outside the clipped surface so that it stays whole at the edges; the composable
 * only has to draw itself.
 */
@Composable
public fun ColorPlane(
    xValue: Float,
    yValue: Float,
    onValueChange: (x: Float, y: Float) -> Unit,
    surface: DrawScope.() -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
    semanticLabel: String? = null,
    semanticValueText: String? = null,
    actionLabels: PlaneActionLabels? = ColorPickerStrings.current.planeAxisActions(),
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    interactionSource: MutableInteractionSource? = null,
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
) {
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    ColorPlaneImpl(
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
        onValueChangeFinished = onValueChangeFinished,
        enabled = enabled,
        semanticLabel = semanticLabel,
        semanticValueText = semanticValueText,
        actionLabels = actionLabels,
        colors = colors,
        shapes = shapes,
        interactionSource = interactionSource,
        thumb = thumb,
    )
}

/**
 * [ColorPlane] with the size of a key or accessibility step left to [onStep], which gets the direction,
 * whether the coarse step was asked for, and returns false when the step changes nothing.
 */
@Composable
internal fun ColorPlaneImpl(
    xValue: Float,
    yValue: Float,
    onValueChange: (x: Float, y: Float) -> Unit,
    onStep: (dx: Int, dy: Int, coarse: Boolean) -> Boolean,
    surface: DrawScope.() -> Unit,
    modifier: Modifier = Modifier,
    onValueChangeFinished: (() -> Unit)? = null,
    enabled: Boolean = true,
    semanticLabel: String? = null,
    semanticValueText: String? = null,
    actionLabels: PlaneActionLabels? = ColorPickerStrings.current.planeAxisActions(),
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    interactionSource: MutableInteractionSource? = null,
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
) {
    val source = interactionSource ?: remember { MutableInteractionSource() }
    val focusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val dimensions = ColorPickerDefaults.currentDimensions()
    // The gesture handler outlives any one composition, so it reads the callbacks and the
    // painter through these rather than capturing the values it was built with.
    val currentOnValueChange by rememberUpdatedState(onValueChange)
    val currentOnFinished by rememberUpdatedState(onValueChangeFinished)
    val currentSurface by rememberUpdatedState(surface)
    val currentOnStep by rememberUpdatedState(onStep)
    val active = enabled && LocalPickerEnabled.current

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
                    .clip(shapes.planeShape)
                    .drawBehind { currentSurface() },
            )
            // A bare Box, so the indicator measures to its own size. Giving the wrapper a
            // size instead squeezes a larger custom thumb into the default diameter and
            // strands a smaller one in the corner of it, off the value it marks.
            Box {
                if (thumb != null) thumb(source)
                else PlaneThumb(dimensions.planeThumbSize, source)
            }
        },
        modifier = modifier
            .defaultMinSize(dimensions.planeMinSize, dimensions.planeMinSize)
            .disabledAppearance(active, colors)
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
                    val press = DragInteraction.Start()
                    scope.launch { source.emit(press) }
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
                    scope.launch {
                        source.emit(
                            if (completed) DragInteraction.Stop(press) else DragInteraction.Cancel(press),
                        )
                    }
                }
            },
    ) { measurables, constraints ->
        // defaultMinSize has already raised a loose minimum to PlaneMinSize, so the minimum
        // is the surface's size either way: the exact size a caller asked for, or the
        // fallback when the parent passes unbounded space down.
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
                x = (xValue * width - indicator.width / 2f).roundToInt(),
                y = ((1f - yValue) * height - indicator.height / 2f).roundToInt(),
            )
        }
    }
}

// How far outside the indicator the focus ring sits.
private val FocusRingGap = 4.dp

/** Default position indicator, sized from [ColorPickerDefaults.currentDimensions]. */
@Composable
private fun PlaneThumb(diameter: Dp, interactionSource: InteractionSource) {
    // Focus is marked on the indicator rather than around the plane: it is where the eye
    // already is, and it moves with the value the arrow keys are changing.
    //
    // Only for whoever needs it. Pressing the surface takes focus too, so a finger would
    // otherwise leave the ring sitting there after the drag, marking a thing the toucher has
    // no way to act on. A platform with no touch reports Keyboard throughout.
    val focused by interactionSource.collectIsFocusedAsState()
    val keyboard = LocalInputModeManager.current.inputMode == InputMode.Keyboard
    val showFocus = focused && keyboard

    Canvas(Modifier.size(if (showFocus) diameter + FocusRingGap * 2 else diameter)) {
        val outer = size.minDimension / 2f - 2.dp.toPx()
        val radius = if (showFocus) outer - FocusRingGap.toPx() else outer
        // A dark halo under a white ring keeps the indicator readable at both
        // ends of the surface, where a single-colour ring vanishes.
        fun ring(at: Float) {
            drawCircle(
                color = Color.Black.copy(alpha = 0.35f),
                radius = at,
                style = Stroke(width = 4.dp.toPx()),
            )
            drawCircle(
                color = Color.White,
                radius = at,
                style = Stroke(width = 2.dp.toPx()),
            )
        }

        ring(radius)
        if (showFocus) ring(outer)
    }
}
