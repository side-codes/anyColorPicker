package codes.side.colorpicker.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.graphics.Color
import codes.side.color.ColorChannel
import codes.side.color.ColorSpace
import codes.side.color.ColorSpaces
import codes.side.color.ColorValue
import codes.side.color.HueFamily
import codes.side.color.compose.toColorValue
import codes.side.color.compose.toComposeColor

/**
 * The color a picker edits, and what the picker remembers beside it.
 *
 * [value] is one [ColorValue], in whichever space it was last written or edited in; reading another
 * space converts it. The library's sliders and planes write it as the user moves them, and the app
 * may write it at any time.
 *
 * A grey has no hue, so a hue slider has nothing to show for one. The state remembers the last hue
 * chosen in each [HueFamily] and shows that ([displayValue]), and an edit that makes a grey colorful
 * writes it into the color, so a grey the user darkened or desaturated comes back in the hue they
 * had rather than red. A grey arriving without a hue, from hex, a Compose [Color] or an sRGB value,
 * leaves what is remembered alone. A hue chosen in one family is carried into the others, so an
 * Okhsl slider shows the hue last picked on an HSL one; that is this library's own, as CSS carries
 * no hue across spaces (csswg-drafts#8484).
 *
 * Backed by snapshot state: read from any thread, write on the main thread. Create one with
 * [rememberColorPickerState] or [rememberSaveableColorPickerState], or hold one in a view model.
 */
@Stable
public class ColorPickerState(initialValue: ColorValue) {

    /** A state starting from [initialColor]: in sRGB, unless it is in another Compose color space. */
    public constructor(initialColor: Color) : this(initialColor.toColorValue())

    private val memory = HueMemory()
    private var current by mutableStateOf(initialValue)

    // A value handed to the edit sink and not yet answered by a write of [value]. Edits build on it,
    // so two key presses before the caller recomposes move two steps. Snapshot state, so a picker
    // reconciling its caller's value recomposes after every emission, even one its caller ignores.
    private var pending by mutableStateOf<ColorValue?>(null)

    init {
        // Unobserved: a state is built in composition, and whatever it read here, the composable building
        // it would read too. That composable would then recompose on every new hue, and one that builds a
        // state without remembering it would build another each time, forever.
        Snapshot.withoutReadObservation { memory.learn(initialValue) }
    }

    /** The color. Writing one equal to it changes nothing. */
    public var value: ColorValue
        get() = current
        set(value) {
            pending = null
            if (value == current) return
            current = value
            memory.learn(value)
        }

    /** [value] as a Compose color, mapped into sRGB with `GamutMapping.Css`. */
    public val color: Color get() = current.toComposeColor()

    // How many components are mid-gesture, not whether any is: two fingers can drag two sliders at
    // once, and a flag would clear when the first one let go.
    private var interactions by mutableIntStateOf(0)

    /** True while the user drags any of the library's sliders or planes over this state. */
    public val isInteracting: Boolean get() = interactions > 0

    internal fun beginInteraction() {
        interactions++
    }

    internal fun endInteraction() {
        if (interactions > 0) interactions--
    }

    /** [channel]'s value in its own space, or null when it is `none`, as a grey's hue is. */
    public operator fun get(channel: ColorChannel): Double? = current.to(channel.space)[channel]

    /**
     * What a slider on [channel] shows: its value; for a missing hue, the one last chosen in its
     * family, or 0 when none has been; for any other missing component, 0, as CSS reads `none`.
     */
    public fun displayValue(channel: ColorChannel): Double = displayComponents(channel.space)[channel.index]

    // [displayValue] for every channel of [space], converting [of] once: what a track or a plane
    // holds still, and what a key press steps from.
    internal fun displayComponents(space: ColorSpace, of: ColorValue = current): DoubleArray {
        val color = of.to(space)
        return DoubleArray(space.channels.size) { index ->
            val channel = space.channels[index]
            color[channel] ?: if (channel.isHue) memory.hue(channel) ?: 0.0 else 0.0
        }
    }

    /**
     * Sets [channel] to [value], or to `none` when it is null, and leaves the color in [channel]'s
     * space. A missing hue there takes [displayValue] first, so raising a grey's saturation brings
     * back its remembered hue rather than red.
     *
     * Okhsl and Okhsv describe sRGB alone, so setting one of their channels brings a color from
     * outside sRGB to sRGB's edge.
     *
     * @throws IllegalArgumentException if [value] is not finite or lies outside [ColorChannel.limit].
     */
    public operator fun set(channel: ColorChannel, value: Double?) {
        this.value = edited(channel, value)
    }

    // Where the library's components send an edit: into [value], unless a picker that reports edits
    // instead of applying them has set a sink.
    internal var onEdit: ((ColorValue) -> Unit)? = null

    /** The value edits build on: the last one emitted and not yet answered, else [value]. */
    internal val editBase: ColorValue get() = pending ?: current

    /** The last value emitted to the caller of a picker and not yet answered by a write of [value]. */
    internal val lastEmission: ColorValue? get() = pending

    // Records [value] as handed to the caller of a picker that reports edits instead of applying them.
    internal fun emit(value: ColorValue) {
        pending = value
    }

    internal fun edit(channel: ColorChannel, value: Double?) {
        submit(edited(channel, value))
    }

    internal fun editAlpha(alpha: Double?) {
        submit(editBase.withAlpha(alpha))
    }

    // Two channels of one space in one write, as a plane drags them: the color passes through no value
    // with only one of them changed.
    internal fun edit(x: ColorChannel, xValue: Double?, y: ColorChannel, yValue: Double?) {
        submit(edited(x, xValue).with(y, yValue))
    }

    private fun submit(edited: ColorValue) {
        val sink = onEdit
        if (sink != null) sink(edited) else value = edited
    }

    private fun edited(channel: ColorChannel, value: Double?): ColorValue {
        var color = editBase.to(channel.space)
        val hue = channel.space.hueChannel()
        if (hue != null && hue !== channel && color.isMissing(hue)) color = color.with(hue, memory.hue(hue) ?: 0.0)
        return color.with(channel, value)
    }

    internal val rememberedHues: Map<HueFamily, Double> get() = memory.remembered

    internal fun restoreHues(saved: Map<HueFamily, Double>, known: Collection<ColorSpace>) {
        memory.restore(saved, known)
    }

    public companion object {
        /**
         * Saves a state's value and remembered hues. A saved space is found by id among [knownSpaces]
         * and then the library's own, as `ColorValue.parseCss` finds one; a value whose space is not
         * found restores as null, so the state starts again from its initial value.
         *
         * @throws IllegalArgumentException if two different spaces in [knownSpaces] share an id.
         */
        public fun Saver(knownSpaces: Collection<ColorSpace> = ColorSpaces.all): Saver<ColorPickerState, Any> =
            colorPickerStateSaver(knownSpaces)
    }
}
