package codes.side.colorpicker.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import codes.side.color.ColorSpace
import codes.side.color.ColorSpaces
import codes.side.color.ColorValue
import codes.side.color.HueFamily
import codes.side.color.compose.toColorValue
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.PickerColor

// The first element of every saved list. A later format changes it, and a list of another format
// restores as null rather than being misread.
private const val SAVED_FORMAT = 1

// A saved state is a flat list of primitives every platform's saved state holds:
// [SAVED_FORMAT, space id, each component, missing mask, alpha, alpha missing,
//  then a family id and its hue for each remembered hue].
internal fun colorPickerStateSaver(knownSpaces: Collection<ColorSpace>): Saver<ColorPickerState, Any> {
    val spaces = spacesById(knownSpaces)
    return Saver(
        save = { state -> saved(state) },
        restore = { saved -> (saved as? List<*>)?.let { restored(it, spaces) } },
    )
}

// [knownSpaces] by id, then the library's own. Two different spaces under one id are refused, as
// parseCss refuses them.
private fun spacesById(knownSpaces: Collection<ColorSpace>): Map<String, ColorSpace> {
    val spaces = LinkedHashMap<String, ColorSpace>()
    for (space in knownSpaces) {
        require(spaces.getOrPut(space.id) { space } === space) { "Two different color spaces have the id ${space.id}" }
    }
    for (space in ColorSpaces.all) spaces.getOrPut(space.id) { space }
    return spaces
}

private fun saved(state: ColorPickerState): ArrayList<Any> {
    val value = state.value
    val saved = arrayListOf<Any>(SAVED_FORMAT, value.space.id)
    for (component in value.components()) saved.add(component)
    saved.add(value.missingMask)
    saved.add(value.alpha)
    saved.add(value.isAlphaMissing)
    for ((family, hue) in state.rememberedHues) {
        saved.add(family.id)
        saved.add(hue)
    }
    return saved
}

private fun restored(saved: List<*>, spaces: Map<String, ColorSpace>): ColorPickerState? {
    if (saved.firstOrNull() != SAVED_FORMAT) return null
    val space = spaces[saved.getOrNull(1) as? String ?: return null] ?: return null
    val count = space.channels.size
    val huesStart = 2 + count + 3
    if (saved.size < huesStart || (saved.size - huesStart) % 2 != 0) return null
    val components = DoubleArray(count)
    for (i in 0 until count) components[i] = saved[2 + i] as? Double ?: return null
    val missing = saved[2 + count] as? Int ?: return null
    val alpha = saved[3 + count] as? Double ?: return null
    val alphaMissing = saved[4 + count] as? Boolean ?: return null
    val hues = LinkedHashMap<HueFamily, Double>()
    for (i in huesStart until saved.size step 2) {
        val family = saved[i] as? String ?: return null
        val hue = saved[i + 1] as? Double ?: return null
        if (hue !in 0.0..<360.0) return null
        hues[HueFamily(family)] = hue
    }
    val value = try {
        space.color(components, if (alphaMissing) null else alpha, missing)
    } catch (_: IllegalArgumentException) {
        return null
    }
    return ColorPickerState(value).apply { restoreHues(hues, spaces.values) }
}

/**
 * Like [rememberColorPickerState], but the state survives configuration changes and process death
 * where the platform restores saved state. The value and the remembered hues are saved;
 * [ColorPickerState.isInteracting] is not.
 *
 * [initialValue] is read once. Its space is known to the saver, and so is every space in
 * [knownSpaces]; a value later edited into a space in neither restores as [initialValue].
 */
@Composable
public fun rememberSaveableColorPickerState(
    initialValue: ColorValue,
    knownSpaces: Collection<ColorSpace> = ColorSpaces.all,
): ColorPickerState = rememberSaveable(saver = ColorPickerState.Saver(knownSpaces + initialValue.space)) {
    ColorPickerState(initialValue)
}

/** Like [rememberSaveableColorPickerState], starting from a Compose [Color]. */
@Composable
public fun rememberSaveableColorPickerState(
    initialColor: Color,
    knownSpaces: Collection<ColorSpace> = ColorSpaces.all,
): ColorPickerState = rememberSaveableColorPickerState(initialColor.toColorValue(), knownSpaces)

/** Like [rememberSaveableColorPickerState], starting from a color of the model package. */
@Composable
public fun rememberSaveableColorPickerState(
    initialColor: PickerColor = HslColor(),
): ColorPickerState = rememberSaveableColorPickerState(initialColor.toColorValue())
