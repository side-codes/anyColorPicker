package codes.side.colorpicker.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import codes.side.color.ColorSpace
import codes.side.color.ColorSpaces
import codes.side.color.ColorValue
import codes.side.colorpicker.state.ColorPickerState

/**
 * The state behind a picker over a value of type [T] that its caller holds. It is a saved
 * [ColorPickerState], which keeps the remembered hues and the exact value behind the last report,
 * while [external] decides what is drawn.
 *
 * Each edit is converted into [space], unless it changed alpha alone, and handed to [onChange] at once,
 * and not applied; later edits build on it until the caller answers. Each composition then takes [external] in, compared in the
 * caller's type:
 * - equal to the last emission, the state keeps the exact value it emitted, so a [T] of 8-bit sRGB
 *   does not drag an Okhsl picker through 8-bit sRGB;
 * - equal to the current value, nothing changes;
 * - anything else becomes the value, under the hue memory's rules, so a late grey keeps its hue.
 *
 * This runs during composition, as Material's `Slider` sets its state's value from its `value`.
 */
@Composable
internal fun <T> rememberControlledPickerState(
    external: T,
    space: ColorSpace,
    onChange: (T) -> Unit,
    toValue: (T) -> ColorValue,
    fromValue: (ColorValue) -> T,
): ColorPickerState {
    val saver = remember(space) { ColorPickerState.Saver(ColorSpaces.all + space) }
    val state = rememberSaveable(saver = saver) { ColorPickerState(toValue(external)) }
    val currentOnChange by rememberUpdatedState(onChange)
    val currentFromValue by rememberUpdatedState(fromValue)
    state.onEdit = { edited ->
        // An edit of alpha alone keeps the value's space, as BasicAlphaSlider's does over a state: converting it
        // would pull a color the picker's space cannot hold to that space's edge for an opacity change.
        val base = state.editBase
        val alphaOnly = edited.withAlpha(if (base.isAlphaMissing) null else base.alpha) == base
        val emitted = if (alphaOnly) edited else edited.to(space)
        state.emit(emitted)
        currentOnChange(currentFromValue(emitted))
    }
    val emitted = state.lastEmission
    state.write(
        when {
            emitted != null && fromValue(emitted) == external -> emitted
            fromValue(state.value) == external -> state.value
            else -> toValue(external)
        },
    )
    return state
}
