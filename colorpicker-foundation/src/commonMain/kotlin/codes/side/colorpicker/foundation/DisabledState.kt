package codes.side.colorpicker.foundation

import androidx.compose.runtime.CompositionLocal
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics

/**
 * False inside a disabled picker. The library's sliders and planes read it beside their own `enabled`,
 * so one in a replaced slot that was never given `enabled` is disabled too: dimmed, unfocusable, and
 * offering a screen reader nothing to adjust, where [disabledInput] alone stops only the pointer.
 */
internal val LocalPickerEnabled: ProvidableCompositionLocal<Boolean> = compositionLocalOf { true }

/**
 * False inside a disabled [BasicColorPicker], and true everywhere else.
 *
 * The library's components already read it beside their own `enabled`, and tell their slots through
 * [ColorSliderScope.enabled] and [ColorPlaneScope.enabled]. Read it for what lies outside every scope: a
 * label row drawn above a slider, or a frame around a plane, that should look disabled with the picker
 * even in a slot that was never handed `enabled`.
 */
public val LocalColorPickerEnabled: CompositionLocal<Boolean> get() = LocalPickerEnabled

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
