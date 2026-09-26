package codes.side.colorpicker.foundation

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalLayoutDirection
import codes.side.color.GamutMapping
import codes.side.color.compose.toComposeColor
import codes.side.colorpicker.state.ColorPickerState

// How far Left or Right and Page Up or Page Down move alpha; a screen reader steps by the first.
private const val ALPHA_STEP = 0.01
private const val ALPHA_PAGE_STEP = 0.1

/**
 * What a [BasicAlphaSlider]'s track and thumb draw from: a [ColorSliderScope] with the alpha. Only the
 * library implements it.
 */
@Stable
public sealed interface AlphaSliderScope : ColorSliderScope {
    /** The color's alpha, `0` when it is missing, as CSS reads `none`. */
    public val alpha: Double

    /**
     * The track's colors: the color itself, faded in from transparent at the track's start to opaque at
     * its end in the layout direction. Draw a checkerboard under it, or its transparent end shows whatever
     * is behind the slider; see [checkerboard].
     */
    public val gradient: Brush
}

/**
 * A slider for the alpha of [state] with no look of its own: [track] and [thumb] draw it, reading the
 * alpha and the track's colors from [AlphaSliderScope]. It edits alpha alone and keeps the color's
 * space. A missing alpha reads 0, as CSS reads `none`, and moving the slider gives it a value.
 *
 * It moves as [BasicColorSlider] does: Left and Right by 0.01, Page Up and Page Down by 0.1, and Home
 * and End to transparent and opaque; a screen reader steps by 0.01. [ColorSliderScope.thumbColor] is
 * the color itself, opaque, so a thumb stays visible at alpha 0.
 *
 * @param onValueChangeFinished called when a tap or drag ends, and after each key press or screen reader
 * step that changes the value.
 * @param semanticLabel what a screen reader calls the slider, "Alpha" from [ColorPickerStrings] by
 * default; `null` omits it.
 * @param semanticValueText how a screen reader announces the value (`0..255`), in the locale's number
 * format by default; `null` omits it.
 * @param interactionSource receives the slider's press, drag, focus and hover interactions; see
 * [BasicColorSlider]. Note that if `null` is provided, interactions will still happen internally.
 * @param track draws the track at the width it is measured at.
 * @param thumb draws the thumb at the size it measures to.
 */
@Composable
public fun BasicAlphaSlider(
    state: ColorPickerState,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onValueChangeFinished: () -> Unit = {},
    semanticLabel: String? = ColorPickerStrings.current.alphaName(),
    semanticValueText: String? = ColorPickerStrings.current.alphaValue(state.value.alpha, true),
    interactionSource: MutableInteractionSource? = null,
    track: @Composable AlphaSliderScope.() -> Unit,
    thumb: @Composable AlphaSliderScope.() -> Unit,
) {
    val value = state.value
    val opaque = value.withAlpha(1.0)
    // The color itself, brought into sRGB as the other tracks are, faded in from transparent so the
    // gradient previews the color instead of fading through transparent black.
    val opaqueColor = remember(opaque) { opaque.toComposeColor(mapping = GamutMapping.ChromaReduction) }
    val layoutDirection = LocalLayoutDirection.current
    val gradient = remember(opaqueColor, layoutDirection) {
        TrackStops(listOf(opaqueColor.copy(alpha = 0f), opaqueColor), null).brush(layoutDirection)
    }
    val interaction = remember(state) { SliderInteractionGuard(state) }
    val currentFinished by rememberUpdatedState(onValueChangeFinished)

    // A key press moves from the alpha edits build on, so two presses before a recomposition move two
    // steps.
    fun step(direction: Int, page: Boolean): Boolean {
        val current = state.editBase.alpha
        val next = (current + direction * if (page) ALPHA_PAGE_STEP else ALPHA_STEP).coerceIn(0.0, 1.0)
        if (next == current) return false
        state.editAlpha(next)
        return true
    }

    BasicColorSliderImpl(
        value = value.alpha.toFloat(),
        onValueChange = {
            interaction.begin()
            state.editAlpha(it.toDouble())
        },
        onStep = ::step,
        accessibilitySteps = accessibilitySteps(0.0..1.0, ALPHA_STEP),
        modifier = modifier,
        enabled = enabled,
        thumbColor = opaqueColor,
        onValueChangeFinished = {
            interaction.end()
            currentFinished()
        },
        semanticLabel = semanticLabel,
        semanticValueText = semanticValueText,
        interactionSource = interactionSource,
        track = { alphaSlots(this, value.alpha, gradient).track() },
        thumb = { alphaSlots(this, value.alpha, gradient).thumb() },
    )
}

// The plain slider's scope with the alpha added.
@Composable
private fun alphaSlots(base: ColorSliderScope, alpha: Double, gradient: Brush): AlphaSliderScope =
    remember(base, alpha, gradient) { AlphaSliderSlots(base, alpha, gradient) }

private class AlphaSliderSlots(
    base: ColorSliderScope,
    override val alpha: Double,
    override val gradient: Brush,
) : AlphaSliderScope, ColorSliderScope by base
