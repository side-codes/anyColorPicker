package codes.side.colorpicker.foundation

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// The narrowest width the picker goes beside its sliders at: under it, the sliders' half is too narrow for a
// label and a value above each track.
private val SideBySideMinWidth = 480.dp

private enum class DialogArrangement { Stacked, SideBySide, Unbounded }

// The tallest height the content has been laid out at in its current arrangement. Read and written in layout
// alone, so growing it recomposes nothing.
private class HeightFloor {
    var arrangement: DialogArrangement? = null
    var height = 0
}

/**
 * The body of a color dialog, with no window, title or buttons: [header], [spaceSwitcher] and the [picker],
 * arranged by the space it is given rather than by the window's size, since a dialog can sit in any window.
 *
 * - Stacked, header over switcher over the picker, when that fits the height available.
 * - Otherwise, when the width available is at least 480 dp, header and switcher side by side above the
 *   picker, and [picker] is handed [Orientation.Horizontal], so the plane sits beside the sliders.
 * - Otherwise stacked, scrolling.
 *
 * Arranged side by side, it scrolls when even that does not fit. Given no height limit, as in a scrolling
 * column, it stacks at its own height.
 *
 * Stacked, the switcher spans the width; side by side, it keeps its own width and the header takes the rest.
 * The parts are measured, never asked for intrinsic sizes, so a slot built on `SubcomposeLayout`, as
 * `BoxWithConstraints` and lazy lists are, lays out as any other does.
 *
 * It never shrinks while it is composed for the same [state] and arranged the same way: a space with no plane
 * is laid out at the height of the tallest space shown so far, so the dialog around it does not jump when the
 * space changes. Arranged another way, as when a window resized too short to stack puts the plane beside the
 * sliders, it starts again from the new arrangement's own height.
 *
 * @param picker draws the picker in the orientation it is handed.
 * @param header drawn first; `null` leaves it out.
 * @param spaceSwitcher drawn after [header]; `null` leaves it out.
 * @param spacing the space between the parts.
 */
@Composable
public fun BasicColorPickerDialogContent(
    state: ColorPickerDialogState,
    picker: @Composable (Orientation) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null,
    spaceSwitcher: (@Composable () -> Unit)? = null,
    spacing: Dp = 0.dp,
) {
    val floor = remember(state) { HeightFloor() }
    // Whether the content is laid out side by side, which the stacked form, composed then only to be measured,
    // reads to keep out of the semantics tree: a composed node that is never placed is still in it, and a
    // screen reader would find every slider twice.
    val sideBySide = remember { mutableStateOf(false) }
    // Whether a part of the stacked form holds keyboard focus. Laid out side by side, that part is not placed and
    // nothing shows where focus is, so it is let go.
    val stackedHasFocus = remember { mutableStateOf(false) }
    if (sideBySide.value && stackedHasFocus.value) {
        val focusManager = LocalFocusManager.current
        SideEffect { focusManager.clearFocus(force = true) }
    }
    val stackedScroll = rememberScrollState()
    val sideBySideScroll = rememberScrollState()
    // Built here rather than in the measure block, so every measure pass hands SubcomposeLayout the same content,
    // and a slot recomposes only when what it reads changes.
    val stacked: @Composable () -> Unit = {
        StackedParts(
            modifier = Modifier
                .then(if (sideBySide.value) Modifier.clearAndSetSemantics {} else Modifier)
                .onFocusChanged { stackedHasFocus.value = it.hasFocus }
                .focusGroup()
                .verticalScroll(stackedScroll),
            header = header,
            spaceSwitcher = spaceSwitcher,
            picker = picker,
            spacing = spacing,
        )
    }
    // Without the scroll, which a height with no limit makes throw.
    val unbounded: @Composable () -> Unit = { StackedParts(Modifier, header, spaceSwitcher, picker, spacing) }
    val besideTheSliders: @Composable () -> Unit = {
        Column(Modifier.verticalScroll(sideBySideScroll), verticalArrangement = Arrangement.spacedBy(spacing)) {
            if (header != null || spaceSwitcher != null) {
                // The switcher, measured first, at its own width, which a row of labels needs whole, and the
                // header, which stretches, in the rest.
                Row(horizontalArrangement = Arrangement.spacedBy(spacing), verticalAlignment = Alignment.CenterVertically) {
                    header?.let { Box(Modifier.weight(1f)) { it() } }
                    spaceSwitcher?.invoke()
                }
            }
            picker(Orientation.Horizontal)
        }
    }
    SubcomposeLayout(modifier) { constraints ->
        val arrangement: DialogArrangement
        val placeable = if (!constraints.hasBoundedHeight) {
            arrangement = DialogArrangement.Unbounded
            if (Snapshot.withoutReadObservation { sideBySide.value }) sideBySide.value = false
            subcompose(DialogArrangement.Unbounded, unbounded).single().measure(constraints.copy(minHeight = 0))
        } else {
            // Measured whichever way it is laid out: whether it has to scroll is what decides.
            val stackedPlaceable = subcompose(DialogArrangement.Stacked, stacked).single().measure(constraints.copy(minHeight = 0))
            // Set as the scroll is measured. Unobserved, as the writes below are, or this layout would measure
            // again for its own reads and writes.
            val overflows = Snapshot.withoutReadObservation { stackedScroll.maxValue } > 0
            val beside = overflows && constraints.maxWidth >= SideBySideMinWidth.roundToPx()
            if (Snapshot.withoutReadObservation { sideBySide.value } != beside) sideBySide.value = beside
            if (beside) {
                arrangement = DialogArrangement.SideBySide
                subcompose(DialogArrangement.SideBySide, besideTheSliders).single().measure(constraints.copy(minHeight = 0))
            } else {
                arrangement = DialogArrangement.Stacked
                stackedPlaceable
            }
        }
        // A floor kept from another arrangement would hold this one at a height it has no use for: the stacked
        // form's, left empty under the plane and the sliders.
        if (floor.arrangement != arrangement) {
            floor.arrangement = arrangement
            floor.height = 0
        }
        val height = maxOf(placeable.height, floor.height).coerceIn(constraints.minHeight, constraints.maxHeight)
        floor.height = height
        layout(placeable.width, height) { placeable.place(0, 0) }
    }
}

// Header over switcher over the picker, the switcher spanning the width.
@Composable
private fun StackedParts(
    modifier: Modifier,
    header: (@Composable () -> Unit)?,
    spaceSwitcher: (@Composable () -> Unit)?,
    picker: @Composable (Orientation) -> Unit,
    spacing: Dp,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        header?.invoke()
        spaceSwitcher?.let { Box(Modifier.fillMaxWidth(), propagateMinConstraints = true) { it() } }
        picker(Orientation.Vertical)
    }
}
