package codes.side.colorpicker.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import codes.side.colorpicker.model.CmykColor
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.LabColor
import codes.side.colorpicker.model.OkhslColor
import codes.side.colorpicker.model.OkhsvColor
import codes.side.colorpicker.model.OklabColor
import codes.side.colorpicker.model.OklchColor
import codes.side.colorpicker.model.PickerColor
import codes.side.colorpicker.model.RgbColor

// Stable keys identifying the persisted color space. These values are part of
// the saved-state format — never renumber or reuse them.
private const val SPACE_KEY_HSL = 0f
private const val SPACE_KEY_RGB = 1f
private const val SPACE_KEY_CMYK = 2f
private const val SPACE_KEY_LAB = 3f
private const val SPACE_KEY_OKLAB = 4f
private const val SPACE_KEY_OKLCH = 5f
private const val SPACE_KEY_OKHSL = 6f
private const val SPACE_KEY_OKHSV = 7f

private const val SAVED_ARRAY_SIZE = 6

/**
 * Encodes the authoritative space and its native components in a single FloatArray.
 *
 * Layout: `[spaceKey, c0, c1, c2, c3, c4]`
 * - `spaceKey` = stable space key (0=HSL, 1=RGB, 2=CMYK, 3=LAB, 4=Oklab, 5=OkLCh,
 *   6=Okhsl, 7=Okhsv)
 * - HSL:   c0=hue, c1=saturation, c2=lightness, c3=alpha, c4 unused
 * - RGB:   c0=red, c1=green, c2=blue, c3=alpha, c4 unused
 * - CMYK:  c0=cyan, c1=magenta, c2=yellow, c3=key, c4=alpha
 * - LAB:   c0=l, c1=a, c2=b, c3=alpha, c4 unused
 * - Oklab: c0=l, c1=a, c2=b, c3=alpha, c4 unused
 * - OkLCh: c0=l, c1=chroma, c2=hue, c3=alpha, c4 unused
 * - Okhsl: c0=hue, c1=saturation, c2=lightness, c3=alpha, c4 unused
 * - Okhsv: c0=hue, c1=saturation, c2=value, c3=alpha, c4 unused
 *
 * This preserves the authoritative space across process death so the user's
 * "origin" choice survives rotation, not just the visible color.
 *
 * Restoring invalid data (unknown space key, wrong array size, or out-of-range
 * channels) returns `null` per the [Saver] contract instead of throwing.
 */
internal val ColorPickerStateSaver = Saver<ColorPickerState, FloatArray>(
    save = { state ->
        when (val color = state.pickerColor) {
            is HslColor -> floatArrayOf(
                SPACE_KEY_HSL,
                color.hue, color.saturation, color.lightness, color.alpha, 0f,
            )

            is RgbColor -> floatArrayOf(
                SPACE_KEY_RGB,
                color.red, color.green, color.blue, color.alpha, 0f,
            )

            is CmykColor -> floatArrayOf(
                SPACE_KEY_CMYK,
                color.cyan, color.magenta, color.yellow, color.key, color.alpha,
            )

            is LabColor -> floatArrayOf(
                SPACE_KEY_LAB,
                color.l, color.a, color.b, color.alpha, 0f,
            )

            is OklabColor -> floatArrayOf(
                SPACE_KEY_OKLAB,
                color.l, color.a, color.b, color.alpha, 0f,
            )

            is OklchColor -> floatArrayOf(
                SPACE_KEY_OKLCH,
                color.l, color.chroma, color.hue, color.alpha, 0f,
            )

            is OkhslColor -> floatArrayOf(
                SPACE_KEY_OKHSL,
                color.hue, color.saturation, color.lightness, color.alpha, 0f,
            )

            is OkhsvColor -> floatArrayOf(
                SPACE_KEY_OKHSV,
                color.hue, color.saturation, color.value, color.alpha, 0f,
            )
        }
    },
    restore = { array ->
        val color: PickerColor? = if (array.size != SAVED_ARRAY_SIZE) {
            null
        } else {
            try {
                when (array[0]) {
                    SPACE_KEY_HSL -> HslColor(
                        hue = array[1],
                        saturation = array[2],
                        lightness = array[3],
                        alpha = array[4],
                    )

                    SPACE_KEY_RGB -> RgbColor(
                        red = array[1],
                        green = array[2],
                        blue = array[3],
                        alpha = array[4],
                    )

                    SPACE_KEY_CMYK -> CmykColor(
                        cyan = array[1],
                        magenta = array[2],
                        yellow = array[3],
                        key = array[4],
                        alpha = array[5],
                    )

                    SPACE_KEY_LAB -> LabColor(
                        l = array[1],
                        a = array[2],
                        b = array[3],
                        alpha = array[4],
                    )

                    SPACE_KEY_OKLAB -> OklabColor(
                        l = array[1],
                        a = array[2],
                        b = array[3],
                        alpha = array[4],
                    )

                    SPACE_KEY_OKLCH -> OklchColor(
                        l = array[1],
                        chroma = array[2],
                        hue = array[3],
                        alpha = array[4],
                    )

                    SPACE_KEY_OKHSL -> OkhslColor(
                        hue = array[1],
                        saturation = array[2],
                        lightness = array[3],
                        alpha = array[4],
                    )

                    SPACE_KEY_OKHSV -> OkhsvColor(
                        hue = array[1],
                        saturation = array[2],
                        value = array[3],
                        alpha = array[4],
                    )

                    else -> null
                }
            } catch (_: IllegalArgumentException) {
                null
            }
        }
        color?.let { ColorPickerState(it) }
    },
)

/**
 * Like [rememberColorPickerState], but the state survives configuration changes
 * and process death (where supported by the platform).
 *
 * [initialColor] is read only once, when the state is first created; passing a
 * different value on later recompositions does NOT reset the state (matching the
 * `rememberScrollState` convention). The [Saver] persists the authoritative color's
 * native channels together with its color space, so both the visible color and the
 * user's origin-space choice are restored; [ColorPickerState.isInteracting] is
 * transient and not persisted.
 */
@Composable
public fun rememberSaveableColorPickerState(
    initialColor: PickerColor = HslColor(),
): ColorPickerState {
    return rememberSaveable(saver = ColorPickerStateSaver) {
        ColorPickerState(initialColor)
    }
}
