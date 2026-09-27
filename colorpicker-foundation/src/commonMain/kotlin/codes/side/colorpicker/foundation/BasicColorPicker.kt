package codes.side.colorpicker.foundation

import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
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
 * A complete picker for [space] over [state], laying out the parts it is given: [plane] over two of the
 * space's channels when it has one hue and two other channels, [channelSlider] for each channel in
 * channel order, and [alphaSlider]. It draws nothing itself.
 *
 * [plane] is handed its axes: across, the channel tagged as colorfulness, or else the first that is not
 * a hue; up, the other that is not a hue. For HSL that is S × L, for HSV S × V, for HWB W × B, for LCH
 * and OkLCh C × L.
 *
 * While [enabled] is false a part is refused pointer input even if it was never given [enabled]
 * itself, and the library's sliders and planes inside it are disabled outright: deaf to the keyboard
 * and a screen reader too, and reporting themselves disabled to their slots.
 *
 * @param plane draws the plane; `null` leaves it out, and a space without a plane never calls it.
 * @param channelSlider draws each channel's slider, given the state and the channel.
 * @param alphaSlider draws the alpha slider; `null` leaves it out.
 * @param orientation [Orientation.Vertical] stacks the plane, the channel sliders and alpha;
 * [Orientation.Horizontal] puts the plane in the start half and the sliders and alpha in the end
 * half, and gives the sliders the whole width when there is no plane. Beside the sliders the plane is
 * at least as tall as they are, so the two start and end together.
 * @param spacing the space between the parts.
 */
@Composable
public fun BasicColorPicker(
    state: ColorPickerState,
    space: ColorSpace,
    plane: (@Composable (ColorPickerState, x: ColorChannel, y: ColorChannel) -> Unit)?,
    channelSlider: @Composable (ColorPickerState, ColorChannel) -> Unit,
    alphaSlider: (@Composable (ColorPickerState) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Vertical,
    spacing: Dp = 0.dp,
) {
    val axes = if (plane != null) planeAxes(space) else null
    // Provided rather than passed down, so a replaced part is disabled with the picker without the call
    // site forwarding it.
    CompositionLocalProvider(LocalPickerEnabled provides (enabled && LocalPickerEnabled.current)) {
        when (orientation) {
            Orientation.Vertical -> Column(
                modifier = modifier.disabledInput(enabled),
                verticalArrangement = Arrangement.spacedBy(spacing),
            ) {
                if (plane != null && axes != null) plane(state, axes.first, axes.second)
                PickerSliders(state, space, channelSlider, alphaSlider)
            }
            Orientation.Horizontal -> SideBySide(
                modifier = modifier.disabledInput(enabled),
                spacing = spacing,
                plane = if (plane != null && axes != null) {
                    { plane(state, axes.first, axes.second) }
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
    plane: (@Composable (ColorPickerState, x: ColorChannel, y: ColorChannel) -> Unit)?,
    channelSlider: @Composable (ColorPickerState, ColorChannel) -> Unit,
    alphaSlider: (@Composable (ColorPickerState) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Vertical,
    spacing: Dp = 0.dp,
) {
    val state = rememberControlledPickerState(value, space, onValueChange, toValue = { it }, fromValue = { it })
    BasicColorPicker(state, space, plane, channelSlider, alphaSlider, modifier, enabled, orientation, spacing)
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
    plane: (@Composable (ColorPickerState, x: ColorChannel, y: ColorChannel) -> Unit)?,
    channelSlider: @Composable (ColorPickerState, ColorChannel) -> Unit,
    alphaSlider: (@Composable (ColorPickerState) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    orientation: Orientation = Orientation.Vertical,
    spacing: Dp = 0.dp,
) {
    val state = rememberControlledPickerState(
        color,
        space,
        onColorChange,
        toValue = { it.toColorValue() },
        fromValue = { it.toComposeColor() },
    )
    BasicColorPicker(state, space, plane, channelSlider, alphaSlider, modifier, enabled, orientation, spacing)
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
    channelSlider: @Composable (ColorPickerState, ColorChannel) -> Unit,
    alphaSlider: (@Composable (ColorPickerState) -> Unit)?,
) {
    for (channel in space.channels) key(channel) { channelSlider(state, channel) }
    alphaSlider?.invoke(state)
}
