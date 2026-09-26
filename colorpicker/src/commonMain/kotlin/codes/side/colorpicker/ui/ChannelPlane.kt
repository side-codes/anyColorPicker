package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import codes.side.color.ColorChannel
import codes.side.colorpicker.foundation.BasicChannelPlane
import codes.side.colorpicker.foundation.ColorPickerStrings
import codes.side.colorpicker.foundation.PlaneActionLabels
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes

/**
 * A two-dimensional picker over two channels of one space: [x] left to right and [y] bottom to top,
 * each over its [ColorChannel.referenceRange], with the space's other channels held at their
 * displayed values. Dragging writes both channels at once and leaves the color in their space; with a
 * [ChannelSlider] for the rest and an [AlphaSlider], it makes a full picker.
 *
 * HSL's saturation × lightness and HSV's saturation × value are drawn exactly, with two gradients. Any
 * other pair is sampled on a grid and drawn scaled, and the grid is rebuilt off the main thread when a
 * held channel changes. The library's own planes each have a grid measured to fit them; any other pair
 * takes 64 × 64.
 *
 * Like [ColorPlane], the plane is not mirrored in right-to-left layouts. An arrow key moves a channel by
 * its [ColorChannel.step], and Shift with an arrow by its [ColorChannel.pageStep]; at an edge the plane
 * passes the key on, so focus can leave. The four accessibility actions move by
 * [ColorChannel.pageStep].
 *
 * @param onValueChangeFinished called when a drag ends, and after each key press or accessibility
 * action that changes the value.
 * @param semanticLabel accessibility description of the surface, "Saturation and lightness" by
 * default; `null` omits it.
 * @param semanticValueText accessibility announcement of the pair of values.
 * @param actionLabels names the four accessibility actions after the channels; `null` omits them and
 * leaves the plane readable but not adjustable.
 * @param interactionSource receives the plane's interactions; see [ColorPlane]. Note that if `null` is
 * provided, interactions will still happen internally.
 * @param thumb optional replacement for the position indicator; see [ColorPlane].
 * @throws IllegalArgumentException unless [x] and [y] are two different channels of one space.
 */
@Composable
public fun ChannelPlane(
    state: ColorPickerState,
    x: ColorChannel,
    y: ColorChannel,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: () -> Unit = {},
    semanticLabel: String? = ColorPickerStrings.current.planeDescription(x, y),
    semanticValueText: String? = ColorPickerStrings.current.planeValue(x, state.displayValue(x), y, state.displayValue(y)),
    actionLabels: PlaneActionLabels? = ColorPickerStrings.current.planeActions(x, y),
    colors: ColorPickerColors = ColorPickerDefaults.currentColors(),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    interactionSource: MutableInteractionSource? = null,
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
) {
    val active = enabled && LocalPickerEnabled.current
    val dimensions = ColorPickerDefaults.currentDimensions()
    BasicChannelPlane(
        state = state,
        x = x,
        y = y,
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
