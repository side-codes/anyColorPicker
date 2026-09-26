package codes.side.colorpicker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import codes.side.colorpicker.foundation.BasicColorPlane
import codes.side.colorpicker.foundation.ColorPickerStrings
import codes.side.colorpicker.foundation.ColorPlaneScope
import codes.side.colorpicker.foundation.PlaneActionLabels
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes

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
    val active = enabled && LocalPickerEnabled.current
    val dimensions = ColorPickerDefaults.currentDimensions()
    BasicColorPlane(
        xValue = xValue,
        yValue = yValue,
        onValueChange = onValueChange,
        surface = surface,
        modifier = modifier
            .defaultMinSize(dimensions.planeMinSize, dimensions.planeMinSize)
            .disabledAppearance(active, colors),
        enabled = enabled,
        onValueChangeFinished = onValueChangeFinished,
        shape = shapes.planeShape,
        semanticLabel = semanticLabel,
        semanticValueText = semanticValueText,
        actionLabels = actionLabels,
        interactionSource = interactionSource,
        thumb = { PlaneHandle(thumb, dimensions.planeThumbSize) },
    )
}

/** A caller's [thumb], handed the plane's interactions, or the default indicator at [diameter]. */
@Composable
internal fun ColorPlaneScope.PlaneHandle(thumb: (@Composable (InteractionSource) -> Unit)?, diameter: Dp) {
    if (thumb != null) thumb(interactionSource) else PlaneThumb(diameter, interactionSource)
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
