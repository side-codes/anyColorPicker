package codes.side.colorpicker.ui

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics

/**
 * Refuses pointer input for everything below this point, and says so to accessibility.
 *
 * A ready-made picker applies this as well as passing `enabled` to each slider it composes.
 * The slider parameter is what dims one; this is what makes a *replaced* one inert, since a
 * caller who swaps a slider slot in to change its label can forget to forward `enabled` and
 * would otherwise be left with one live control inside a disabled picker.
 *
 * Events are consumed on [PointerEventPass.Initial], which is the pass that reaches an
 * ancestor before its children.
 */
internal fun Modifier.disabledInput(enabled: Boolean): Modifier =
    if (enabled) {
        this
    } else {
        this
            .semantics { disabled() }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                    }
                }
            }
    }
