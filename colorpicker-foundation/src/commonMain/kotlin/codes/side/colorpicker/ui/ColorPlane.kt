package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.DrawScope
import codes.side.colorpicker.foundation.BasicColorPlane
import codes.side.colorpicker.foundation.ColorPickerStrings
import codes.side.colorpicker.foundation.ColorPlaneScope
import codes.side.colorpicker.foundation.LocalColorPickerEnabled
import codes.side.colorpicker.foundation.PlaneActionLabels
import codes.side.colorpicker.theme.ColorPickerColors
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerDimensions
import codes.side.colorpicker.theme.ColorPickerShapes

/**
 * Two-dimensional picker over a pair of colour channels, both in `0..1`.
 *
 * [xValue] runs left to right and [yValue] bottom to top, so `yValue = 1f` is the top edge.
 * Dragging reports both at once, which is what lets a caller write two channels in a single
 * update and leave the rest of the colour alone. A value outside `0..1` is drawn at the nearer edge.
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
 * @param dimensions the size the plane falls back to when given none, and the default indicator's
 * diameter; see [ColorPickerDefaults.dimensions].
 * @param interactionSource receives the plane's drag and focus interactions, which [thumb] reads from
 * [ColorPlaneScope.interactionSource]. Note that if `null` is provided, interactions will still happen
 * internally.
 * @param thumb marks the current pair, reading it, whether the plane is enabled and its interactions
 * from [ColorPlaneScope]; [ColorPickerDefaults.PlaneThumb] by default. The plane centres whatever it is
 * given on the current pair of values at whatever size that composable measures to, and draws it outside
 * the clipped surface so that it stays whole at the edges. Keyboard focus that shows nowhere on screen
 * leaves its user guessing, so a replacement is expected to mark the source's focus somehow, as the
 * default's second ring does.
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
    dimensions: ColorPickerDimensions = ColorPickerDefaults.currentDimensions(),
    interactionSource: MutableInteractionSource? = null,
    // this., or this function's own interactionSource would shadow the scope's resolved one.
    thumb: @Composable ColorPlaneScope.() -> Unit = { ColorPickerDefaults.PlaneThumb(this.interactionSource, diameter = dimensions.planeThumbSize) },
) {
    val active = enabled && LocalColorPickerEnabled.current
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
        thumb = thumb,
    )
}
