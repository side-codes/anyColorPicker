package codes.side.colorpicker.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import codes.side.colorpicker.model.PickerColor
import codes.side.colorpicker.state.ColorPickerState

/**
 * A [ColorPickerState] driven by a colour the caller holds, for the pickers that take a value
 * and a callback rather than a state object.
 *
 * The caller's value wins. A change the user makes is reported out, and whatever the caller
 * holds afterwards is written back in — so a callback that declines the change, clamps it, or
 * snaps it to a step leaves the picker showing what the caller settled on rather than what the
 * finger did. Keying the write-in on [color] alone is not enough for that: a caller who rejects
 * leaves their value untouched, and an effect keyed on an unchanged value never runs again.
 * [reports] is bumped on the way out so the write-in is re-armed by the report itself.
 *
 * Reporting out compares against the caller's current value, so a colour the caller writes in
 * is shown without being handed straight back as a change.
 *
 * The state itself still owns the origin space, so the zero-drift guarantee survives the trip:
 * a caller round-tripping HSL through their own value never sees it re-derived. That also keeps
 * this from oscillating — `write(x)` followed by `read()` has to return `x`, which is what the
 * `updateFrom*` reads-back-the-exact-instance tests pin down.
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
    var reports by remember { mutableIntStateOf(0) }

    LaunchedEffect(color, reports) {
        if (state.read() != color) state.write(color)
    }

    LaunchedEffect(state) {
        snapshotFlow { state.read() }.collect { changed ->
            if (changed != currentColor) {
                currentOnColorChange(changed)
                reports++
            }
        }
    }

    return state
}
