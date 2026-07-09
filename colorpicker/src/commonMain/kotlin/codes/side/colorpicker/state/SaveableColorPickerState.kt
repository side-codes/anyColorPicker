package codes.side.colorpicker.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import codes.side.colorpicker.model.CmykColor
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.LabColor
import codes.side.colorpicker.model.PickerColor
import codes.side.colorpicker.model.RgbColor

/**
 * Encodes the origin space and its native components in a single FloatArray.
 *
 * Layout: `[ordinal, c0, c1, c2, c3, c4]`
 * - `ordinal` = the [ColorPickerState.Origin] enum ordinal
 * - `c0..c4` = up to 5 native floats (CMYK has 5: c, m, y, k, alpha)
 *
 * This preserves the authoritative space across process death so the user's
 * "origin" choice survives rotation, not just the visible color.
 */
private val ColorPickerStateSaver = Saver<ColorPickerState, FloatArray>(
    save = { state ->
        when (val color = state.pickerColor()) {
            is HslColor -> floatArrayOf(
                ColorPickerState.Origin.HSL.ordinal.toFloat(),
                color.hue, color.saturation, color.lightness, color.alpha, 0f,
            )

            is RgbColor -> floatArrayOf(
                ColorPickerState.Origin.RGB.ordinal.toFloat(),
                color.red, color.green, color.blue, color.alpha, 0f,
            )

            is CmykColor -> floatArrayOf(
                ColorPickerState.Origin.CMYK.ordinal.toFloat(),
                color.cyan, color.magenta, color.yellow, color.key, color.alpha,
            )

            is LabColor -> floatArrayOf(
                ColorPickerState.Origin.LAB.ordinal.toFloat(),
                color.l, color.a, color.b, color.alpha, 0f,
            )
        }
    },
    restore = { array ->
        val origin = ColorPickerState.Origin.entries[array[0].toInt()]
        val color: PickerColor = when (origin) {
            ColorPickerState.Origin.HSL -> HslColor(
                hue = array[1],
                saturation = array[2],
                lightness = array[3],
                alpha = array[4],
            )

            ColorPickerState.Origin.RGB -> RgbColor(
                red = array[1],
                green = array[2],
                blue = array[3],
                alpha = array[4],
            )

            ColorPickerState.Origin.CMYK -> CmykColor(
                cyan = array[1],
                magenta = array[2],
                yellow = array[3],
                key = array[4],
                alpha = array[5],
            )

            ColorPickerState.Origin.LAB -> LabColor(
                l = array[1],
                a = array[2],
                b = array[3],
                alpha = array[4],
            )
        }
        ColorPickerState(color)
    }
)

/**
 * Like [rememberColorPickerState], but the state survives configuration changes
 * and process death (where supported by the platform).
 */
@Composable
fun rememberSaveableColorPickerState(
    initialColor: PickerColor = HslColor(),
): ColorPickerState {
    return rememberSaveable(saver = ColorPickerStateSaver) {
        ColorPickerState(initialColor)
    }
}
