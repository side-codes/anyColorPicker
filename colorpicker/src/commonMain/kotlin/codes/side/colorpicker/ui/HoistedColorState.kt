package codes.side.colorpicker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import codes.side.colorpicker.model.PickerColor
import codes.side.colorpicker.state.ColorPickerState

/**
 * A [ColorPickerState] driven by a colour the caller holds, for the pickers that take a value
 * and a callback rather than a state object.
 *
 * Two one-way bindings rather than a loop: [color] is written in whenever it differs from what
 * the state already holds, and a change the user makes is reported out only when it differs
 * from the [color] the caller is currently showing. Comparing in both directions is what stops
 * the two from bouncing a value back and forth.
 *
 * The state itself still owns the origin space, so the zero-drift guarantee survives the trip:
 * a caller round-tripping HSL through their own value never sees it re-derived.
 */
@Composable
internal fun <T : PickerColor> rememberHoistedColorState(
    color: T,
    read: ColorPickerState.() -> T,
    write: ColorPickerState.(T) -> Unit,
    onColorChange: (T) -> Unit,
): ColorPickerState {
    val state = remember { ColorPickerState(color) }
    val currentColor by rememberUpdatedState(color)
    val currentOnColorChange by rememberUpdatedState(onColorChange)

    LaunchedEffect(color) {
        if (state.read() != color) state.write(color)
    }

    LaunchedEffect(state) {
        snapshotFlow { state.read() }.collect { changed ->
            if (changed != currentColor) currentOnColorChange(changed)
        }
    }

    return state
}
