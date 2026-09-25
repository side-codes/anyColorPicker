package codes.side.colorpicker.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import codes.side.color.Cmyk
import codes.side.color.ColorChannel
import codes.side.color.ColorSpace
import codes.side.color.ColorSpaces
import codes.side.color.ColorValue
import codes.side.color.Hsl
import codes.side.color.HueFamily
import codes.side.color.Lab
import codes.side.color.OkLch
import codes.side.color.Okhsl
import codes.side.color.Okhsv
import codes.side.color.Oklab
import codes.side.color.Srgb
import codes.side.color.compose.toColorValue
import codes.side.color.compose.toComposeColor
import codes.side.color.toGamut
import codes.side.colorpicker.model.CmykColor
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.LabColor
import codes.side.colorpicker.model.OKLAB_AB_RANGE
import codes.side.colorpicker.model.OkhslColor
import codes.side.colorpicker.model.OkhsvColor
import codes.side.colorpicker.model.OklabColor
import codes.side.colorpicker.model.OklchColor
import codes.side.colorpicker.model.PickerColor
import codes.side.colorpicker.model.RgbColor

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
 * leaves what is remembered alone.
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

    init {
        memory.learn(initialValue)
    }

    /** The color. Writing one equal to it changes nothing. */
    public var value: ColorValue
        get() = current
        set(value) {
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
    public fun displayValue(channel: ColorChannel): Double =
        get(channel) ?: if (channel.isHue) memory.hue(channel) ?: 0.0 else 0.0

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

    internal fun edit(channel: ColorChannel, value: Double?) {
        submit(edited(channel, value))
    }

    internal fun editAlpha(alpha: Double?) {
        submit(current.withAlpha(alpha))
    }

    private fun submit(edited: ColorValue) {
        val sink = onEdit
        if (sink != null) sink(edited) else value = edited
    }

    private fun edited(channel: ColorChannel, value: Double?): ColorValue {
        var color = current.to(channel.space)
        val hue = channel.space.hueChannel()
        if (hue != null && hue !== channel && color.isMissing(hue)) color = color.with(hue, displayValue(hue))
        return color.with(channel, value)
    }

    internal val rememberedHues: Map<HueFamily, Double> get() = memory.remembered

    internal fun restoreHues(saved: Map<HueFamily, Double>) {
        memory.restore(saved)
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

    // ---- In the model package's types, which the per-channel sliders, planes and pickers use ----

    /** A state starting from [initialColor]. */
    public constructor(initialColor: PickerColor = HslColor()) : this(initialColor.toColorValue())

    // The color in [space] as the model package reads it: itself in its own space, and otherwise
    // converted from its mapping into sRGB.
    private fun legacy(space: ColorSpace): ColorValue =
        if (current.space == space) current else current.toGamut(Srgb.gamut).to(space)

    private fun legacyHue(color: ColorValue, channel: ColorChannel): Double = color[channel] ?: memory.hue(channel) ?: 0.0

    /** The color as HSL, saturation and lightness 0–1. */
    public val hslColor: HslColor get() = legacy(Hsl).let { hslColorOf(it, legacyHue(it, Hsl.H)) }

    /** The color as sRGB, mapped into its gamut. */
    public val rgbColor: RgbColor get() = rgbColorOf(legacy(Srgb))

    /** The color as CMYK. */
    public val cmykColor: CmykColor get() = cmykColorOf(legacy(Cmyk))

    /** The color as CIELAB. */
    public val labColor: LabColor get() = labColorOf(legacy(Lab))

    /** The color as Oklab. */
    public val oklabColor: OklabColor get() = oklabColorOf(legacy(Oklab))

    /** The color as OkLCh. */
    public val oklchColor: OklchColor get() = legacy(OkLch).let { oklchColorOf(it, legacyHue(it, OkLch.H)) }

    /** The color as Okhsl. */
    public val okhslColor: OkhslColor get() = legacy(Okhsl).let { okhslColorOf(it, legacyHue(it, Okhsl.H)) }

    /** The color as Okhsv. */
    public val okhsvColor: OkhsvColor get() = legacy(Okhsv).let { okhsvColorOf(it, legacyHue(it, Okhsv.H)) }

    /** The color as packed ARGB (`0xAARRGGBB`), mapped into sRGB. */
    public val argbInt: Int get() = color.toArgb()

    /** The color in its own space, as the model package's class for that space, or as sRGB. */
    public val pickerColor: PickerColor
        get() = when (current.space) {
            Hsl -> hslColor
            Cmyk -> cmykColor
            Lab -> labColor
            Oklab -> oklabColor
            OkLch -> oklchColor
            Okhsl -> okhslColor
            Okhsv -> okhsvColor
            else -> rgbColor
        }

    // A model-package channel write: NaN is ignored, and the value is held to [range], then scaled.
    private fun setLegacy(channel: ColorChannel, value: Float, range: ClosedFloatingPointRange<Float>, scale: Double = 1.0) {
        if (!value.isNaN()) set(channel, value.coerceIn(range) * scale)
    }

    public fun updateHue(hue: Float): Unit = setLegacy(Hsl.H, hue, 0f..360f)
    public fun updateSaturation(saturation: Float): Unit = setLegacy(Hsl.S, saturation, 0f..1f, 100.0)
    public fun updateLightness(lightness: Float): Unit = setLegacy(Hsl.L, lightness, 0f..1f, 100.0)
    public fun updateFromHsl(hsl: HslColor) {
        value = hsl.toColorValue()
    }

    public fun updateRed(red: Float): Unit = setLegacy(Srgb.R, red, 0f..1f)
    public fun updateGreen(green: Float): Unit = setLegacy(Srgb.G, green, 0f..1f)
    public fun updateBlue(blue: Float): Unit = setLegacy(Srgb.B, blue, 0f..1f)
    public fun updateFromRgb(rgb: RgbColor) {
        value = rgb.toColorValue()
    }

    public fun updateCyan(cyan: Float): Unit = setLegacy(Cmyk.C, cyan, 0f..1f)
    public fun updateMagenta(magenta: Float): Unit = setLegacy(Cmyk.M, magenta, 0f..1f)
    public fun updateYellow(yellow: Float): Unit = setLegacy(Cmyk.Y, yellow, 0f..1f)
    public fun updateKey(key: Float): Unit = setLegacy(Cmyk.K, key, 0f..1f)
    public fun updateFromCmyk(cmyk: CmykColor) {
        value = cmyk.toColorValue()
    }

    public fun updateLabLightness(l: Float): Unit = setLegacy(Lab.L, l, 0f..100f)
    public fun updateLabA(a: Float): Unit = setLegacy(Lab.A, a, -128f..127f)
    public fun updateLabB(b: Float): Unit = setLegacy(Lab.B, b, -128f..127f)
    public fun updateFromLab(lab: LabColor) {
        value = lab.toColorValue()
    }

    public fun updateOklabLightness(l: Float): Unit = setLegacy(Oklab.L, l, 0f..1f)
    public fun updateOklabA(a: Float): Unit = setLegacy(Oklab.A, a, -OKLAB_AB_RANGE..OKLAB_AB_RANGE)
    public fun updateOklabB(b: Float): Unit = setLegacy(Oklab.B, b, -OKLAB_AB_RANGE..OKLAB_AB_RANGE)
    public fun updateFromOklab(oklab: OklabColor) {
        value = oklab.toColorValue()
    }

    public fun updateOklchLightness(l: Float): Unit = setLegacy(OkLch.L, l, 0f..1f)
    public fun updateOklchChroma(chroma: Float): Unit = setLegacy(OkLch.C, chroma, 0f..OKLAB_AB_RANGE)
    public fun updateOklchHue(hue: Float): Unit = setLegacy(OkLch.H, hue, 0f..360f)
    public fun updateFromOklch(oklch: OklchColor) {
        value = oklch.toColorValue()
    }

    public fun updateOkhslHue(hue: Float): Unit = setLegacy(Okhsl.H, hue, 0f..360f)
    public fun updateOkhslSaturation(saturation: Float): Unit = setLegacy(Okhsl.S, saturation, 0f..1f)
    public fun updateOkhslLightness(lightness: Float): Unit = setLegacy(Okhsl.L, lightness, 0f..1f)
    public fun updateFromOkhsl(okhsl: OkhslColor) {
        value = okhsl.toColorValue()
    }

    public fun updateOkhsvHue(hue: Float): Unit = setLegacy(Okhsv.H, hue, 0f..360f)
    public fun updateOkhsvSaturation(saturation: Float): Unit = setLegacy(Okhsv.S, saturation, 0f..1f)
    public fun updateOkhsvValue(value: Float): Unit = setLegacy(Okhsv.V, value, 0f..1f)
    public fun updateFromOkhsv(okhsv: OkhsvColor) {
        value = okhsv.toColorValue()
    }

    public fun updateAlpha(alpha: Float) {
        if (!alpha.isNaN()) value = current.withAlpha(alpha.coerceIn(0f, 1f).toDouble())
    }

    public fun updateFromArgbInt(argb: Int) {
        value = Color(argb).toColorValue()
    }
}
