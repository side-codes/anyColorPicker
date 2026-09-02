package codes.side.colorpicker.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import codes.side.colorpicker.conversion.toComposeColor
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/** Saturation for a pointer at [x] across a plane [width] pixels wide. */
internal fun saturationAt(x: Float, width: Int): Float =
    if (width <= 0) 0f else (x / width).coerceIn(0f, 1f)

/** Lightness for a pointer at [y] down a plane [height] pixels tall; the top is white. */
internal fun lightnessAt(y: Float, height: Int): Float =
    if (height <= 0) 0.5f else (1f - y / height).coerceIn(0f, 1f)

/**
 * Two-dimensional saturation and lightness picker for the hue currently held by [state].
 *
 * Saturation runs left to right and lightness bottom to top, so the surface reads as white
 * along the top edge, black along the bottom, grey down the left, and the pure hue at the
 * right of the middle row. Dragging writes both channels at once; hue and alpha are left
 * alone, so a [HueSlider] and an [AlphaSlider] compose with this to make a full picker.
 *
 * Unlike the sliders, the surface is not mirrored in right-to-left layouts. It is a map of a
 * colour space rather than a progress control, and mirroring it would make saturation grow
 * leftwards here while it still grows rightwards on the hue slider beside it.
 *
 * @param semanticLabel accessibility description of the surface; pass a localized string to
 * replace the English default, or `null` to omit.
 * @param semanticValueText accessibility announcement of the current pair of values.
 * @param thumb optional replacement for the position indicator, receiving the surface's
 * [InteractionSource] so it can react to being dragged. The plane positions whatever it is
 * given; the composable only has to draw itself.
 */
@Composable
public fun SaturationLightnessPlane(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    semanticLabel: String? = "Saturation and lightness",
    semanticValueText: String? =
        "${state.hslColor.intSaturation}% saturation, ${state.hslColor.intLightness}% lightness",
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
) {
    val hsl = state.hslColor
    val interactionSource = remember { MutableInteractionSource() }
    val guard = remember(state) { SliderInteractionGuard(state) }
    val scope = rememberCoroutineScope()
    var planeSize by remember { mutableStateOf(IntSize.Zero) }

    // The horizontal ramp is the hue at mid lightness, from fully desaturated to pure. The
    // vertical overlay then takes it to white and to black. That pair reproduces HSL
    // exactly rather than approximately: for any hue and saturation, the colour at
    // lightness L is the mid-lightness colour blended with white by 2L-1 above the middle
    // and with black by 1-2L below it, which is what alpha compositing the overlay does.
    val desaturated = remember(hsl.hue) {
        HslColor(hue = hsl.hue, saturation = 0f, lightness = 0.5f).toComposeColor()
    }
    val pure = remember(hsl.hue) {
        HslColor(hue = hsl.hue, saturation = 1f, lightness = 0.5f).toComposeColor()
    }

    Box(
        modifier = modifier
            .defaultMinSize(ColorPickerDefaults.PlaneMinSize, ColorPickerDefaults.PlaneMinSize)
            .clip(shapes.planeShape)
            .onSizeChanged { planeSize = it }
            .semantics {
                semanticLabel?.let { contentDescription = it }
                semanticValueText?.let { stateDescription = it }
            }
            .drawBehind {
                drawRect(Brush.horizontalGradient(listOf(desaturated, pure)))
                drawRect(
                    Brush.verticalGradient(
                        listOf(Color.White, Color.Transparent, Color.Black),
                    ),
                )
            }
            .pointerInput(state) {
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val press = DragInteraction.Start()
                    scope.launch { interactionSource.emit(press) }
                    guard.begin()
                    state.commitPlane(down.position, size.width, size.height)
                    down.consume()

                    val completed = drag(down.id) { change ->
                        state.commitPlane(change.position, size.width, size.height)
                        change.consume()
                    }

                    guard.end()
                    scope.launch {
                        interactionSource.emit(
                            if (completed) DragInteraction.Stop(press) else DragInteraction.Cancel(press),
                        )
                    }
                }
            },
    ) {
        val diameter = ColorPickerDefaults.PlaneThumbSize
        Box(
            modifier = Modifier
                .offset {
                    val radius = diameter.toPx() / 2f
                    IntOffset(
                        x = (hsl.saturation * planeSize.width - radius).roundToInt(),
                        y = ((1f - hsl.lightness) * planeSize.height - radius).roundToInt(),
                    )
                }
                .size(diameter),
        ) {
            if (thumb != null) {
                thumb(interactionSource)
            } else {
                Canvas(Modifier.size(diameter)) {
                    val radius = size.minDimension / 2f - 2.dp.toPx()
                    // A dark halo under a white ring keeps the indicator readable at both
                    // ends of the surface, where a single-colour ring vanishes.
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.35f),
                        radius = radius,
                        style = Stroke(width = 4.dp.toPx()),
                    )
                    drawCircle(
                        color = Color.White,
                        radius = radius,
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
        }
    }
}

/** Writes both channels in one update, so hue and alpha survive the gesture untouched. */
private fun ColorPickerState.commitPlane(position: Offset, width: Int, height: Int) {
    updateFromHsl(
        hslColor.copy(
            saturation = saturationAt(position.x, width),
            lightness = lightnessAt(position.y, height),
        ),
    )
}
