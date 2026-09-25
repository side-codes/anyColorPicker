package codes.side.colorpicker.ui

import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import codes.side.colorpicker.theme.ColorPickerColors

/**
 * False inside a disabled picker. The library's sliders and planes read it beside their own `enabled`,
 * so one in a replaced slot that was never given `enabled` is disabled too: dimmed, unfocusable, and
 * offering a screen reader nothing to adjust, where [disabledInput] alone stops only the pointer.
 */
internal val LocalPickerEnabled: ProvidableCompositionLocal<Boolean> = compositionLocalOf { true }

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

/**
 * Draws [this] the way [colors] says a disabled control should look.
 *
 * Two knobs rather than one, because the Material convention and this control disagree. Material
 * dims a disabled component to 38% and that is what [ColorPickerColors.disabledAlpha] does, but a
 * colour picker's track is made of the colours being chosen: dimming it shows paler versions of
 * real colours, which is a wrong answer rather than an absent one, and it composites against a
 * background the library does not own, so the same value pales on white and darkens on black.
 * [ColorPickerColors.disabledSaturation] drains the colour instead, which is background
 * independent and claims nothing.
 *
 * Leave either at its neutral value to switch it off: `1f` alpha keeps full opacity and `1f`
 * saturation keeps full colour, so a caller can have dimming, draining, both, or neither. Both
 * are applied in one layer, so asking for both costs no more than asking for one.
 */
internal fun Modifier.disabledAppearance(enabled: Boolean, colors: ColorPickerColors): Modifier {
    val alpha = colors.disabledAlpha
    val saturation = colors.disabledSaturation
    if (enabled || (alpha == 1f && saturation == 1f)) return this
    return drawWithContent {
        val paint = Paint().also { paint ->
            paint.alpha = alpha
            if (saturation != 1f) {
                paint.colorFilter = ColorFilter.colorMatrix(
                    ColorMatrix().also { it.setToSaturation(saturation) },
                )
            }
        }
        drawIntoCanvas { canvas ->
            canvas.saveLayer(size.toRect(), paint)
            drawContent()
            canvas.restore()
        }
    }
}
