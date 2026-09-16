package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import codes.side.colorpicker.conversion.toComposeColor
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes

/**
 * Two-dimensional saturation and lightness picker for the hue currently held by [state].
 *
 * Saturation runs left to right and lightness bottom to top, so the surface reads as white
 * along the top edge, black along the bottom, grey down the left, and the pure hue at the
 * right of the middle row. Dragging writes both channels at once; hue and alpha are left
 * alone, so a [HueSlider] and an [AlphaSlider] compose with this to make a full picker.
 *
 * @param semanticLabel accessibility description of the surface; pass a localized string to
 * replace the English default, or `null` to omit.
 * @param semanticValueText accessibility announcement of the current pair of values.
 * @param actionLabels names the four accessibility actions that move the plane; see
 * [ColorPlane].
 * @param thumb optional replacement for the position indicator; see [ColorPlane].
 */
@Composable
public fun HslPlane(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    semanticLabel: String? = "Saturation and lightness",
    semanticValueText: String? =
        "${state.hslColor.intSaturation}% saturation, ${state.hslColor.intLightness}% lightness",
    actionLabels: PlaneActionLabels? = PlaneActionLabels(
        increaseX = "Increase saturation",
        decreaseX = "Decrease saturation",
        increaseY = "Increase lightness",
        decreaseY = "Decrease lightness",
    ),
    shapes: ColorPickerShapes = ColorPickerDefaults.currentShapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
) {
    val hsl = state.hslColor
    val interaction = remember(state) { SliderInteractionGuard(state) }

    // The horizontal ramp is the hue at mid lightness, from fully desaturated to pure. The
    // vertical overlay then takes it to white and to black. That pair reproduces HSL
    // exactly rather than approximately: for any hue and saturation, the colour at
    // lightness L is the mid-lightness colour blended with white by 2L-1 above the middle
    // and with black by 1-2L below it, which is what alpha compositing the overlay does.
    val ramp = remember(hsl.hue) {
        Brush.horizontalGradient(
            listOf(
                HslColor(hue = hsl.hue, saturation = 0f, lightness = 0.5f).toComposeColor(),
                HslColor(hue = hsl.hue, saturation = 1f, lightness = 0.5f).toComposeColor(),
            ),
        )
    }
    val shading = remember {
        Brush.verticalGradient(listOf(Color.White, Color.Transparent, Color.Black))
    }

    ColorPlane(
        xValue = hsl.saturation,
        yValue = hsl.lightness,
        onValueChange = { x, y ->
            interaction.begin()
            state.updateFromHsl(state.hslColor.copy(saturation = x, lightness = y))
        },
        surface = {
            drawRect(ramp)
            drawRect(shading)
        },
        modifier = modifier,
        enabled = enabled,
        onValueChangeFinished = { interaction.end() },
        semanticLabel = semanticLabel,
        semanticValueText = semanticValueText,
        actionLabels = actionLabels,
        shapes = shapes,
        thumb = thumb,
    )
}
