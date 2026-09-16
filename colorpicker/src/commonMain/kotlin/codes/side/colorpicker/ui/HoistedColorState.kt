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
 * holds once the gesture ends is written back in — so a callback that declines the change,
 * clamps it, or snaps it to a step leaves the picker showing what the caller settled on rather
 * than what the finger did. Keying the write-in on [color] alone is not enough for that: a
 * caller who rejects leaves their value untouched, and an effect keyed on an unchanged value
 * never runs again, so the report bumps [reports] to re-arm it.
 *
 * The re-arm waits for [ColorPickerState.isInteracting] to go false. Correcting mid-gesture
 * would fight a caller whose own value lands late — debounced, written by a background
 * coroutine, confirmed by a store — by putting their stale colour back under a moving finger,
 * every frame, so the thumb would never leave where the drag started. Such a caller is still
 * corrected once, when the finger lifts. **The callback is expected to update [color]
 * synchronously**; one that does not will show that single correction before its own value
 * arrives.
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
        // The colour the caller was last told about, so the emission that only marks the end
        // of a gesture does not hand them the same one a second time. Cleared once the two
        // ends agree, or a value reported and put back could never be reported again.
        var reported: T? = null
        snapshotFlow { state.read() to state.isInteracting }.collect { (changed, interacting) ->
            if (changed == currentColor) {
                reported = null
                return@collect
            }
            if (changed != reported) {
                reported = changed
                currentOnColorChange(changed)
            }
            if (!interacting) reports++
        }
    }

    return state
}
