package codes.side.colorpicker.foundation

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.constrainWidth
import androidx.compose.ui.unit.dp
import codes.side.color.AnalogousCategory
import codes.side.color.ColorChannel
import codes.side.color.ColorSpace
import codes.side.color.ColorValue
import codes.side.color.compose.toColorValue
import codes.side.color.compose.toComposeColor
import codes.side.colorpicker.state.ColorPickerState
import codes.side.colorpicker.state.ColoringMode
import kotlin.math.max

/** Whether a picker over [space] shows a plane: [space] has one hue and two other channels. */
internal fun hasPlane(space: ColorSpace): Boolean = space.channels.size == 3 && space.channels.count { it.isHue } == 1

/**
 * The channels a picker's plane over [space] shows, or null for a space with no plane: across, the
 * channel tagged as colorfulness, or else the first that is not a hue; up, the other that is not a hue.
 */
internal fun planeAxes(space: ColorSpace): Pair<ColorChannel, ColorChannel>? {
    if (!hasPlane(space)) return null
    val others = space.channels.filter { !it.isHue }
    val x = others.firstOrNull { it.analogous == AnalogousCategory.Colorfulness } ?: others.first()
    return x to others.first { it !== x }
}

/**
 * What [BasicColorPicker] hands each part it lays out. A slot takes it as a parameter rather than as its
 * receiver: a caller's own variable named `state` would hide a receiver's member, and in a dialog, whose
 * picker edits a state of its own, the slot would then edit the caller's.
 *
 * Only the library implements it, so it can gain members without breaking a slot.
 */
@Stable
public sealed interface ColorPickerPart {
    /** The state the picker edits. */
    public val state: ColorPickerState
}

/** What [BasicColorPicker]'s plane slot is handed: the state and the plane's axes. */
@Stable
public sealed interface PlanePart : ColorPickerPart {
    /** The channel across the plane: the one tagged as colorfulness, or else the first that is not a hue. */
    public val x: ColorChannel

    /** The channel up the plane: the other that is not a hue. */
    public val y: ColorChannel
}

/** What [BasicColorPicker]'s channel slider slot is handed, once for each channel: the state and the channel. */
@Stable
public sealed interface ChannelSliderPart : ColorPickerPart {
    /** The channel this slider is for. */
    public val channel: ColorChannel
}

/** What [BasicColorPicker]'s alpha slider slot is handed: the state. */
@Stable
public sealed interface AlphaSliderPart : ColorPickerPart

private class PickerPlane(override val state: ColorPickerState, override val x: ColorChannel, override val y: ColorChannel) : PlanePart

private class PickerChannel(override val state: ColorPickerState, override val channel: ColorChannel) : ChannelSliderPart

private class PickerAlpha(override val state: ColorPickerState) : AlphaSliderPart

/**
 * The coloring mode of the picker around, which a channel slider inside it takes unless given its own; null
 * outside a picker.
 */
internal val LocalPickerColoringMode: ProvidableCompositionLocal<ColoringMode?> = compositionLocalOf { null }

/**
 * The picker's report that an edit ended, which the library's sliders and planes inside it call beside their
 * own `onValueChangeFinished`; null outside a picker.
 */
internal val LocalPickerEditFinished: ProvidableCompositionLocal<(() -> Unit)?> = compositionLocalOf { null }

/**
 * A complete picker for [space] over [state], laying out the parts it is given: [plane] over two of the
 * space's channels when it has one hue and two other channels, [channelSlider] for each channel in
 * channel order, and [alphaSlider]. It draws nothing itself.
 *
 * [plane] is handed its axes: across, the channel tagged as colorfulness, or else the first that is not
 * a hue; up, the other that is not a hue. For HSL that is S × L, for HSV S × V, for HWB W × B, for LCH
 * and OkLCh C × L.
 *
 * A part inherits the picker's settings without the slot forwarding them, as long as it is built from the
 * library's sliders and planes. While [enabled] is false a part is refused pointer input even if it was
 * never given [enabled] itself, and the library's sliders and planes inside it are disabled outright: deaf
 * to the keyboard and a screen reader too, and reporting themselves disabled to their slots. A channel
 * slider given no coloring mode of its own takes [coloringMode], and every slider and plane reports the end
 * of an edit to [onValueChangeFinished] as well as to its own.
 *
 * @param plane draws the plane from its [PlanePart]; `null` leaves it out, and a space without a plane never
 * calls it.
 * @param channelSlider draws each channel's slider from its [ChannelSliderPart].
 * @param alphaSlider draws the alpha slider from its [AlphaSliderPart]; `null` leaves it out.
 * @param orientation [Orientation.Vertical] stacks the plane, the channel sliders and alpha;
 * [Orientation.Horizontal] puts the plane in the start half and the sliders and alpha in the end
 * half, and gives the sliders the whole width when there is no plane. Beside the sliders the plane is
 * at least as tall as they are, so the two start and end together.
 * @param coloringMode how the channel sliders' tracks are drawn: [ColoringMode.Independent] by default for a
 * space with a hue, so a hue track stays a full spectrum, else [ColoringMode.Contextual].
 * @param onValueChangeFinished called when a tap or drag on any of the library's sliders and planes in the
 * picker ends, and after each key press or accessibility step.
 * @param spacing the space between the parts.
 */
@Composable
public fun BasicColorPicker(
    state: ColorPickerState,
    space: ColorSpace,
    plane: (@Composable (PlanePart) -> Unit)?,
    channelSlider: @Composable (ChannelSliderPart) -> Unit,
    alphaSlider: (@Composable (AlphaSliderPart) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Vertical,
    coloringMode: ColoringMode = ColoringMode.defaultFor(space),
    onValueChangeFinished: () -> Unit = {},
    spacing: Dp = 0.dp,
) {
    val axes = if (plane != null) planeAxes(space) else null
    val planePart = remember(state, axes) { axes?.let { PickerPlane(state, it.first, it.second) } }
    val currentOnFinished by rememberUpdatedState(onValueChangeFinished)
    val editFinished = remember { { currentOnFinished() } }
    // Provided rather than passed down, so a replaced part is disabled with the picker, and takes its
    // coloring and reports to it, without the call site forwarding any of it.
    CompositionLocalProvider(
        LocalPickerEnabled provides (enabled && LocalPickerEnabled.current),
        LocalPickerColoringMode provides coloringMode,
        LocalPickerEditFinished provides editFinished,
    ) {
        when (orientation) {
            Orientation.Vertical -> Column(
                modifier = modifier.disabledInput(enabled),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                if (plane != null && planePart != null) plane(planePart)
                PickerSliders(state, space, channelSlider, alphaSlider)
            }
            Orientation.Horizontal -> SideBySide(
                modifier = modifier.disabledInput(enabled),
                spacing = spacing,
                plane = if (plane != null && planePart != null) {
                    { plane(planePart) }
                } else {
                    null
                },
                sliders = { PickerSliders(state, space, channelSlider, alphaSlider) },
            )
        }
    }
}

/**
 * [BasicColorPicker] over a value the caller holds, fully controlled. Every change the user makes
 * reaches [onValueChange] in the same event, as the whole new value: in [space], or in the value's own
 * space when only alpha changed, so opacity never pulls a color [space] cannot hold to its edge. The
 * picker draws only [value], and a value passed in is never reported back.
 *
 * Update your value in the callback. A caller that ignores it holds the picker still, and one that
 * clamps or rounds shows the clamp or the rounding at once. A value that arrives late is drawn when it
 * arrives, and a drag carries on from the finger.
 *
 * When [value] is the one the picker last reported, the picker keeps that exact value. Any other value
 * becomes the picker's; a grey arriving without a hue keeps the hue last chosen. The remembered hues are
 * saved across configuration changes.
 *
 * The remaining parameters are the state form's.
 */
@Composable
public fun BasicColorPicker(
    value: ColorValue,
    onValueChange: (ColorValue) -> Unit,
    space: ColorSpace,
    plane: (@Composable (PlanePart) -> Unit)?,
    channelSlider: @Composable (ChannelSliderPart) -> Unit,
    alphaSlider: (@Composable (AlphaSliderPart) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Vertical,
    coloringMode: ColoringMode = ColoringMode.defaultFor(space),
    onValueChangeFinished: () -> Unit = {},
    spacing: Dp = 0.dp,
) {
    val state = rememberControlledPickerState(value, space, onValueChange, toValue = { it }, fromValue = { it })
    BasicColorPicker(state, space, plane, channelSlider, alphaSlider, modifier, enabled, orientation, coloringMode, onValueChangeFinished, spacing)
}

/**
 * [BasicColorPicker] over a Compose [Color] the caller holds, fully controlled as the [ColorValue] form
 * is. [onColorChange] receives each change as `toComposeColor()`: brought into sRGB by CSS gamut
 * mapping, eight bits a channel. The picker keeps the exact value behind the last color it reported,
 * so it never steps through 8-bit sRGB itself.
 *
 * The remaining parameters are the state form's.
 *
 * @throws IllegalArgumentException for [Color.Unspecified] and Compose's HDR spaces, which
 * [toColorValue] refuses.
 */
@Composable
public fun BasicColorPicker(
    color: Color,
    onColorChange: (Color) -> Unit,
    space: ColorSpace,
    plane: (@Composable (PlanePart) -> Unit)?,
    channelSlider: @Composable (ChannelSliderPart) -> Unit,
    alphaSlider: (@Composable (AlphaSliderPart) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Vertical,
    coloringMode: ColoringMode = ColoringMode.defaultFor(space),
    onValueChangeFinished: () -> Unit = {},
    spacing: Dp = 0.dp,
) {
    val state = rememberControlledPickerState(
        color,
        space,
        onColorChange,
        toValue = { it.toColorValue() },
        fromValue = { it.toComposeColor() },
    )
    BasicColorPicker(state, space, plane, channelSlider, alphaSlider, modifier, enabled, orientation, coloringMode, onValueChangeFinished, spacing)
}

// The plane in the start half and the sliders in the end half, or the sliders across the whole width when there
// is no plane. The sliders are measured first and the plane is given their height as its least, so the two start
// and end together; a plane that asks for more height gets it.
@Composable
private fun SideBySide(
    modifier: Modifier,
    spacing: Dp,
    plane: (@Composable () -> Unit)?,
    sliders: @Composable () -> Unit,
) {
    Layout(
        content = {
            if (plane != null) Box(propagateMinConstraints = true) { plane() }
            Column(verticalArrangement = Arrangement.spacedBy(spacing)) { sliders() }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val gap = spacing.roundToPx()
        val planeMeasurable = measurables.takeIf { it.size == 2 }?.first()
        // Null in a row with no width limit, where the sliders take their own width and the plane matches it.
        val half = when {
            !constraints.hasBoundedWidth -> null
            planeMeasurable == null -> constraints.maxWidth
            else -> ((constraints.maxWidth - gap) / 2).coerceAtLeast(0)
        }
        val sliders = measurables.last().measure(
            Constraints(minWidth = half ?: 0, maxWidth = half ?: Constraints.Infinity, maxHeight = constraints.maxHeight),
        )
        val planePlaceable = planeMeasurable?.let {
            val planeWidth = half ?: sliders.width
            it.measure(
                Constraints(
                    minWidth = planeWidth,
                    maxWidth = planeWidth,
                    minHeight = sliders.height.coerceAtMost(constraints.maxHeight),
                    maxHeight = constraints.maxHeight,
                ),
            )
        }
        val slidersX = if (planePlaceable != null) planePlaceable.width + gap else 0
        val width = constraints.constrainWidth(slidersX + sliders.width)
        val height = constraints.constrainHeight(max(sliders.height, planePlaceable?.height ?: 0))
        layout(width, height) {
            planePlaceable?.placeRelative(0, 0)
            sliders.placeRelative(slidersX, 0)
        }
    }
}

// A slider for each of [space]'s channels in channel order, then alpha.
@Composable
private fun PickerSliders(
    state: ColorPickerState,
    space: ColorSpace,
    channelSlider: @Composable (ChannelSliderPart) -> Unit,
    alphaSlider: (@Composable (AlphaSliderPart) -> Unit)?,
) {
    for (channel in space.channels) {
        key(channel) { channelSlider(remember(state, channel) { PickerChannel(state, channel) }) }
    }
    if (alphaSlider != null) alphaSlider(remember(state) { PickerAlpha(state) })
}
