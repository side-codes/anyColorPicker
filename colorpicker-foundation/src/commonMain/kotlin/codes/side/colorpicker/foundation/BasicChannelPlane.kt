package codes.side.colorpicker.foundation

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import codes.side.color.ColorChannel
import codes.side.colorpicker.state.ColorPickerState

/**
 * What a [BasicChannelPlane]'s thumb draws from: a [ColorPlaneScope] with the plane's channels. Only
 * the library implements it.
 */
@Stable
public sealed interface ChannelPlaneScope : ColorPlaneScope {
    /** The channel across the plane. */
    public val x: ColorChannel

    /** The channel up the plane. */
    public val y: ColorChannel

    /** The color under the thumb, opaque and in sRGB. */
    public val thumbColor: Color
}

/**
 * A two-dimensional picker over two channels of [state]'s color with no look of its own: it paints the
 * field, and [thumb] marks the current pair, reading the channels and the color under it from
 * [ChannelPlaneScope]. [x] runs left to right and [y] bottom to top, each over its
 * [ColorChannel.referenceRange], with the space's other channels held at their displayed values.
 * Dragging writes both channels at once and leaves the color in their space.
 *
 * HSL's saturation × lightness and HSV's saturation × value are drawn exactly, with two gradients. Any
 * other pair is sampled on a grid and drawn scaled, and the grid is rebuilt off the main thread when a
 * held channel changes. The library's own planes each have a grid measured to fit them; any other pair
 * takes 64 × 64.
 *
 * It takes input as [BasicColorPlane] does and imposes no size. An arrow key moves a channel by its
 * [ColorChannel.step], and Shift with an arrow by its [ColorChannel.pageStep]; at an edge the plane
 * passes the key on, so focus can leave. The four accessibility actions move by
 * [ColorChannel.pageStep].
 *
 * @param onValueChangeFinished called when a drag ends, and after each key press or accessibility
 * action that changes the value.
 * @param shape clips the field. The thumb is drawn outside it, so it stays whole at the edges.
 * @param semanticLabel accessibility description of the surface, "Saturation and lightness" by
 * default; `null` omits it.
 * @param semanticValueText accessibility announcement of the pair of values.
 * @param actionLabels names the four accessibility actions after the channels; `null` omits them and
 * leaves the plane readable but not adjustable.
 * @param interactionSource receives the plane's interactions; see [BasicColorPlane]. Note that if
 * `null` is provided, interactions will still happen internally.
 * @param thumb marks the current pair; see [BasicColorPlane].
 * @throws IllegalArgumentException unless [x] and [y] are two different channels of one space.
 */
@Composable
public fun BasicChannelPlane(
    state: ColorPickerState,
    x: ColorChannel,
    y: ColorChannel,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: () -> Unit = {},
    shape: Shape = RectangleShape,
    semanticLabel: String? = ColorPickerStrings.current.planeDescription(x, y),
    semanticValueText: String? = ColorPickerStrings.current.planeValue(x, state.displayValue(x), y, state.displayValue(y)),
    actionLabels: PlaneActionLabels? = ColorPickerStrings.current.planeActions(x, y),
    interactionSource: MutableInteractionSource? = null,
    thumb: @Composable ChannelPlaneScope.() -> Unit,
) {
    requirePlaneChannels(x, y)
    val displayed = state.displayComponents(x.space)
    val displayedKey = displayed.toList()
    val thumbColor = remember(x, y, displayedKey) { planeThumbColor(x, y, displayed) }
    val interaction = remember(state) { SliderInteractionGuard(state) }
    val currentFinished by rememberUpdatedState(onValueChangeFinished)

    BasicColorPlaneImpl(
        xValue = fractionOf(displayed[x.index], x.referenceRange),
        yValue = fractionOf(displayed[y.index], y.referenceRange),
        onValueChange = { fx, fy ->
            interaction.begin()
            state.edit(
                x,
                channelValueAt(x, x.referenceRange, fx.toDouble()),
                y,
                channelValueAt(y, y.referenceRange, fy.toDouble()),
            )
        },
        // A key moves from the values edits build on: the state's, or the last ones emitted and not
        // yet answered.
        onStep = { dx, dy, coarse ->
            val now = state.displayComponents(x.space, state.editBase)
            val xNow = now[x.index]
            val yNow = now[y.index]
            val nextX = if (dx == 0) xNow else clampToRange(x, x.referenceRange, xNow + dx * if (coarse) x.pageStep else x.step)
            val nextY = if (dy == 0) yNow else clampToRange(y, y.referenceRange, yNow + dy * if (coarse) y.pageStep else y.step)
            if (nextX == xNow && nextY == yNow) {
                false
            } else {
                state.edit(x, nextX, y, nextY)
                true
            }
        },
        surface = rememberPlaneSurface(x, y, displayed),
        modifier = modifier,
        enabled = enabled,
        onValueChangeFinished = {
            interaction.end()
            currentFinished()
        },
        shape = shape,
        semanticLabel = semanticLabel,
        semanticValueText = semanticValueText,
        actionLabels = actionLabels,
        interactionSource = interactionSource,
        thumb = { channelPlaneSlots(this, x, y, thumbColor).thumb() },
    )
}

/** @throws IllegalArgumentException unless [x] and [y] are two different channels of one space. */
internal fun requirePlaneChannels(x: ColorChannel, y: ColorChannel) {
    require(x !== y && x.space == y.space) { "A plane takes two different channels of one space, not $x and $y" }
}

// The color at the thumb: [displayed] with the plane's two channels held to their ranges, as the thumb
// is, brought into sRGB and opaque.
private fun planeThumbColor(x: ColorChannel, y: ColorChannel, displayed: DoubleArray): Color {
    val held = displayed.copyOf()
    held[y.index] = displayed[y.index].coerceIn(y.referenceRange.start, y.referenceRange.endInclusive)
    return trackColorAt(x, held, displayed[x.index].coerceIn(x.referenceRange.start, x.referenceRange.endInclusive))
}

// The plain plane's scope with the channels added.
@Composable
private fun channelPlaneSlots(base: ColorPlaneScope, x: ColorChannel, y: ColorChannel, thumbColor: Color): ChannelPlaneScope =
    remember(base, x, y, thumbColor) { ChannelPlaneSlots(base, x, y, thumbColor) }

private class ChannelPlaneSlots(
    base: ColorPlaneScope,
    override val x: ColorChannel,
    override val y: ColorChannel,
    override val thumbColor: Color,
) : ChannelPlaneScope, ColorPlaneScope by base
