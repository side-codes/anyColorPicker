package codes.side.colorpicker.ui

import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import codes.side.colorpicker.conversion.okhsvRowFiller
import codes.side.colorpicker.conversion.toComposeColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.theme.ColorPickerDefaults
import codes.side.colorpicker.theme.ColorPickerShapes

/**
 * Two-dimensional saturation and value picker for the hue currently held by [state], in
 * Okhsv.
 *
 * The arrangement artists expect from a colour picker, with the perceptual behaviour Okhsv
 * gives it: the top right is the most vivid form of the hue, pulling down the surface darkens
 * toward black, and every point in the square is a colour the display can actually show.
 * Prefer [OkhslPlane] when the middle of the vertical axis should be a mid tone rather than a
 * bright one. Dragging writes both channels at once; hue and alpha are left alone, so an
 * [OkhsvHueSlider] and an [AlphaSlider] compose with this to make a full picker.
 *
 * The field is sampled rather than interpolated from a pair of gradients, because Okhsv is
 * linear in neither axis. Okhsv puts its cusp on the corner of the square instead of running
 * it through the middle, so it needs a quarter of the rows [OkhslPlane] does and lands at
 * about 2.3 of 255; see the row count this plane is rasterized at.
 *
 * @param semanticLabel accessibility description of the surface; pass a localized string to
 * replace the English default, or `null` to omit.
 * @param semanticValueText accessibility announcement of the current pair of values.
 * @param thumb optional replacement for the position indicator; see [ColorPlane].
 */
@Composable
public fun OkhsvPlane(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    semanticLabel: String? = "Saturation and value",
    semanticValueText: String? =
        "${state.okhsvColor.intSaturation}% saturation, ${state.okhsvColor.intValue}% value",
    shapes: ColorPickerShapes = ColorPickerDefaults.shapes(),
    thumb: (@Composable (InteractionSource) -> Unit)? = null,
) {
    val okhsv = state.okhsvColor
    val interaction = remember(state) { SliderInteractionGuard(state) }
    val field = remember(okhsv.hue) { okhsvRowFiller(okhsv.hue) }
    val bitmap = rememberPlaneBitmap(
        key = okhsv.hue,
        width = OK_PLANE_COLUMNS,
        height = OKHSV_PLANE_ROWS,
        fillRow = field,
    )

    ColorPlane(
        xValue = okhsv.saturation,
        yValue = okhsv.value,
        onValueChange = { x, y ->
            interaction.begin()
            state.updateFromOkhsv(state.okhsvColor.copy(saturation = x, value = y))
        },
        surface = { drawPlaneBitmap(bitmap) },
        modifier = modifier,
        onValueChangeFinished = { interaction.end() },
        semanticLabel = semanticLabel,
        semanticValueText = semanticValueText,
        shapes = shapes,
        thumb = thumb,
    )
}
