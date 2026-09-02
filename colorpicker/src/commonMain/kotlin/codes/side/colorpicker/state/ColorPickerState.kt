package codes.side.colorpicker.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import codes.side.colorpicker.conversion.toArgbInt
import codes.side.colorpicker.conversion.toCmyk
import codes.side.colorpicker.conversion.toHsl
import codes.side.colorpicker.conversion.toLab
import codes.side.colorpicker.conversion.toOkhsl
import codes.side.colorpicker.conversion.toOkhsv
import codes.side.colorpicker.conversion.toOklab
import codes.side.colorpicker.conversion.toOklch
import codes.side.colorpicker.conversion.toRgbColor
import codes.side.colorpicker.conversion.withAlpha
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
 * Single source of truth for the current color.
 *
 * Holds one authoritative [PickerColor] — whichever space was last written to.
 * All other spaces are derived from it via [derivedStateOf] on demand (pure
 * conversions, no writes on read). This guarantees zero round-trip drift *within
 * the authoritative space*: repeatedly reading and re-writing channels of the
 * space last written to never re-derives the value from a conversion. Values
 * observed in the other spaces are conversions and carry ordinary conversion
 * rounding.
 *
 * Backed by Compose snapshot state: reads are safe from any thread, but writes
 * (the `update*` methods and [isInteracting]) are expected on the main thread,
 * like other Compose UI state. Updates are idempotent — writing a value equal to
 * the current one produces no observable state change.
 *
 * Create instances in composition via [rememberColorPickerState] or
 * [rememberSaveableColorPickerState], or hold one directly (e.g. in a view model)
 * using this constructor.
 *
 * @param initialColor the starting color; it becomes the initial authoritative
 * value, so its runtime type also selects the initial origin space.
 */
@Stable
public class ColorPickerState(initialColor: PickerColor = HslColor()) {

    // The single authoritative value. Its runtime type is the origin space —
    // whichever space was last written to.
    private var authoritative by mutableStateOf<PickerColor>(initialColor)

    /**
     * True while the user is actively dragging one of the library's sliders (set on the
     * first value change, cleared when the gesture finishes or the interacting slider
     * leaves composition mid-drag). Useful for deferring expensive work until the
     * interaction ends. Programmatic `update*` calls do not affect this flag.
     */
    public var isInteracting: Boolean by mutableStateOf(false)
        internal set

    // ---- Derived spaces (pure computation, no writes on read) ----
    //
    // Each space is itself when authoritative and a conversion otherwise. The
    // conversions route through RGB; see PickerColor.toRgbColor.

    private val hslDerived = derivedStateOf {
        val c = authoritative
        if (c is HslColor) c else c.toRgbColor().toHsl()
    }

    private val rgbDerived = derivedStateOf { authoritative.toRgbColor() }

    private val cmykDerived = derivedStateOf {
        val c = authoritative
        if (c is CmykColor) c else c.toRgbColor().toCmyk()
    }

    private val labDerived = derivedStateOf {
        val c = authoritative
        if (c is LabColor) c else c.toRgbColor().toLab()
    }

    private val oklabDerived = derivedStateOf {
        val c = authoritative
        if (c is OklabColor) c else c.toRgbColor().toOklab()
    }

    private val oklchDerived = derivedStateOf {
        val c = authoritative
        if (c is OklchColor) c else c.toRgbColor().toOklch()
    }

    private val okhslDerived = derivedStateOf {
        val c = authoritative
        if (c is OkhslColor) c else c.toRgbColor().toOkhsl()
    }

    private val okhsvDerived = derivedStateOf {
        val c = authoritative
        if (c is OkhsvColor) c else c.toRgbColor().toOkhsv()
    }

    // ---- Public read access ----

    /** The current color as HSL; a derived conversion unless HSL is the origin space. */
    public val hslColor: HslColor get() = hslDerived.value

    /** The current color as RGB; a derived conversion unless RGB is the origin space. */
    public val rgbColor: RgbColor get() = rgbDerived.value

    /** The current color as CMYK; a derived conversion unless CMYK is the origin space. */
    public val cmykColor: CmykColor get() = cmykDerived.value

    /** The current color as CIELAB; a derived conversion unless LAB is the origin space. */
    public val labColor: LabColor get() = labDerived.value

    /** The current color as Oklab; a derived conversion unless Oklab is the origin space. */
    public val oklabColor: OklabColor get() = oklabDerived.value

    /** The current color as OkLCh; a derived conversion unless OkLCh is the origin space. */
    public val oklchColor: OklchColor get() = oklchDerived.value

    /** The current color as Okhsl; a derived conversion unless Okhsl is the origin space. */
    public val okhslColor: OkhslColor get() = okhslDerived.value

    /** The current color as Okhsv; a derived conversion unless Okhsv is the origin space. */
    public val okhsvColor: OkhsvColor get() = okhsvDerived.value

    /** The current color as a packed ARGB [Int] (`0xAARRGGBB`). */
    public val argbInt: Int get() = rgbColor.toArgbInt()

    /** The authoritative color in whichever space was last written to. */
    public val pickerColor: PickerColor get() = authoritative

    // ---- HSL updates ----

    /**
     * Updates the hue channel. NaN is ignored; values are clamped to 0..360, and 360
     * is stored as the equivalent 0 (see [HslColor]), so the observable range is
     * 0..360 (exclusive).
     */
    public fun updateHue(hue: Float) {
        if (hue.isNaN()) return
        authoritative = hslColor.copy(hue = hue.coerceIn(0f, 360f))
    }

    /** Updates the saturation channel. NaN is ignored; values are clamped to 0..1. */
    public fun updateSaturation(saturation: Float) {
        if (saturation.isNaN()) return
        authoritative = hslColor.copy(saturation = saturation.coerceIn(0f, 1f))
    }

    /** Updates the lightness channel. NaN is ignored; values are clamped to 0..1. */
    public fun updateLightness(lightness: Float) {
        if (lightness.isNaN()) return
        authoritative = hslColor.copy(lightness = lightness.coerceIn(0f, 1f))
    }

    /** Sets [hsl] as the authoritative color; HSL becomes the origin space. */
    public fun updateFromHsl(hsl: HslColor) {
        authoritative = hsl
    }

    // ---- RGB updates ----

    /** Updates the red channel. NaN is ignored; values are clamped to 0..1. */
    public fun updateRed(red: Float) {
        if (red.isNaN()) return
        authoritative = rgbColor.copy(red = red.coerceIn(0f, 1f))
    }

    /** Updates the green channel. NaN is ignored; values are clamped to 0..1. */
    public fun updateGreen(green: Float) {
        if (green.isNaN()) return
        authoritative = rgbColor.copy(green = green.coerceIn(0f, 1f))
    }

    /** Updates the blue channel. NaN is ignored; values are clamped to 0..1. */
    public fun updateBlue(blue: Float) {
        if (blue.isNaN()) return
        authoritative = rgbColor.copy(blue = blue.coerceIn(0f, 1f))
    }

    /** Sets [rgb] as the authoritative color; RGB becomes the origin space. */
    public fun updateFromRgb(rgb: RgbColor) {
        authoritative = rgb
    }

    // ---- CMYK updates ----

    /** Updates the cyan channel. NaN is ignored; values are clamped to 0..1. */
    public fun updateCyan(cyan: Float) {
        if (cyan.isNaN()) return
        authoritative = cmykColor.copy(cyan = cyan.coerceIn(0f, 1f))
    }

    /** Updates the magenta channel. NaN is ignored; values are clamped to 0..1. */
    public fun updateMagenta(magenta: Float) {
        if (magenta.isNaN()) return
        authoritative = cmykColor.copy(magenta = magenta.coerceIn(0f, 1f))
    }

    /** Updates the yellow channel. NaN is ignored; values are clamped to 0..1. */
    public fun updateYellow(yellow: Float) {
        if (yellow.isNaN()) return
        authoritative = cmykColor.copy(yellow = yellow.coerceIn(0f, 1f))
    }

    /** Updates the key (black) channel. NaN is ignored; values are clamped to 0..1. */
    public fun updateKey(key: Float) {
        if (key.isNaN()) return
        authoritative = cmykColor.copy(key = key.coerceIn(0f, 1f))
    }

    /** Sets [cmyk] as the authoritative color; CMYK becomes the origin space. */
    public fun updateFromCmyk(cmyk: CmykColor) {
        authoritative = cmyk
    }

    // ---- LAB updates ----

    /** Updates the L (lightness) channel. NaN is ignored; values are clamped to 0..100. */
    public fun updateLabLightness(l: Float) {
        if (l.isNaN()) return
        authoritative = labColor.copy(l = l.coerceIn(0f, 100f))
    }

    /** Updates the a axis. NaN is ignored; values are clamped to -128..127. */
    public fun updateLabA(a: Float) {
        if (a.isNaN()) return
        authoritative = labColor.copy(a = a.coerceIn(-128f, 127f))
    }

    /** Updates the b axis. NaN is ignored; values are clamped to -128..127. */
    public fun updateLabB(b: Float) {
        if (b.isNaN()) return
        authoritative = labColor.copy(b = b.coerceIn(-128f, 127f))
    }

    /** Sets [lab] as the authoritative color; LAB becomes the origin space. */
    public fun updateFromLab(lab: LabColor) {
        authoritative = lab
    }

    // ---- Oklab updates ----

    /** Updates the lightness channel. NaN is ignored; values are clamped to 0..1. */
    public fun updateOklabLightness(l: Float) {
        if (l.isNaN()) return
        authoritative = oklabColor.copy(l = l.coerceIn(0f, 1f))
    }

    /** Updates the a axis. NaN is ignored; values are clamped to -0.4..0.4. */
    public fun updateOklabA(a: Float) {
        if (a.isNaN()) return
        authoritative = oklabColor.copy(a = a.coerceIn(-OKLAB_AB_RANGE, OKLAB_AB_RANGE))
    }

    /** Updates the b axis. NaN is ignored; values are clamped to -0.4..0.4. */
    public fun updateOklabB(b: Float) {
        if (b.isNaN()) return
        authoritative = oklabColor.copy(b = b.coerceIn(-OKLAB_AB_RANGE, OKLAB_AB_RANGE))
    }

    /** Sets [oklab] as the authoritative color; Oklab becomes the origin space. */
    public fun updateFromOklab(oklab: OklabColor) {
        authoritative = oklab
    }

    // ---- OkLCh updates ----

    /** Updates the lightness channel. NaN is ignored; values are clamped to 0..1. */
    public fun updateOklchLightness(l: Float) {
        if (l.isNaN()) return
        authoritative = oklchColor.copy(l = l.coerceIn(0f, 1f))
    }

    /** Updates the chroma channel. NaN is ignored; values are clamped to 0..0.4. */
    public fun updateOklchChroma(chroma: Float) {
        if (chroma.isNaN()) return
        authoritative = oklchColor.copy(chroma = chroma.coerceIn(0f, OKLAB_AB_RANGE))
    }

    /**
     * Updates the hue channel. NaN is ignored; values are clamped to 0..360, and 360 is
     * stored as the equivalent 0 (see [OklchColor]), so the observable range is 0..360
     * (exclusive).
     */
    public fun updateOklchHue(hue: Float) {
        if (hue.isNaN()) return
        authoritative = oklchColor.copy(hue = hue.coerceIn(0f, 360f))
    }

    /** Sets [oklch] as the authoritative color; OkLCh becomes the origin space. */
    public fun updateFromOklch(oklch: OklchColor) {
        authoritative = oklch
    }

    // ---- Okhsl updates ----

    /**
     * Updates the hue channel. NaN is ignored; values are clamped to 0..360, and 360 is
     * stored as the equivalent 0 (see [OkhslColor]), so the observable range is 0..360
     * (exclusive).
     */
    public fun updateOkhslHue(hue: Float) {
        if (hue.isNaN()) return
        authoritative = okhslColor.copy(hue = hue.coerceIn(0f, 360f))
    }

    /** Updates the saturation channel. NaN is ignored; values are clamped to 0..1. */
    public fun updateOkhslSaturation(saturation: Float) {
        if (saturation.isNaN()) return
        authoritative = okhslColor.copy(saturation = saturation.coerceIn(0f, 1f))
    }

    /** Updates the lightness channel. NaN is ignored; values are clamped to 0..1. */
    public fun updateOkhslLightness(lightness: Float) {
        if (lightness.isNaN()) return
        authoritative = okhslColor.copy(lightness = lightness.coerceIn(0f, 1f))
    }

    /** Sets [okhsl] as the authoritative color; Okhsl becomes the origin space. */
    public fun updateFromOkhsl(okhsl: OkhslColor) {
        authoritative = okhsl
    }

    // ---- Okhsv updates ----

    /**
     * Updates the hue channel. NaN is ignored; values are clamped to 0..360, and 360 is
     * stored as the equivalent 0 (see [OkhsvColor]), so the observable range is 0..360
     * (exclusive).
     */
    public fun updateOkhsvHue(hue: Float) {
        if (hue.isNaN()) return
        authoritative = okhsvColor.copy(hue = hue.coerceIn(0f, 360f))
    }

    /** Updates the saturation channel. NaN is ignored; values are clamped to 0..1. */
    public fun updateOkhsvSaturation(saturation: Float) {
        if (saturation.isNaN()) return
        authoritative = okhsvColor.copy(saturation = saturation.coerceIn(0f, 1f))
    }

    /** Updates the value channel. NaN is ignored; values are clamped to 0..1. */
    public fun updateOkhsvValue(value: Float) {
        if (value.isNaN()) return
        authoritative = okhsvColor.copy(value = value.coerceIn(0f, 1f))
    }

    /** Sets [okhsv] as the authoritative color; Okhsv becomes the origin space. */
    public fun updateFromOkhsv(okhsv: OkhsvColor) {
        authoritative = okhsv
    }

    // ---- Alpha update (origin space unchanged) ----

    /** Updates the alpha channel of the authoritative color. NaN is ignored; values are clamped to 0..1. */
    public fun updateAlpha(alpha: Float) {
        if (alpha.isNaN()) return
        authoritative = authoritative.withAlpha(alpha.coerceIn(0f, 1f))
    }

    // ---- ARGB Int update ----

    /** Sets the color from a packed ARGB Int; RGB becomes the origin space. */
    public fun updateFromArgbInt(argb: Int) {
        authoritative = argb.toRgbColor()
    }
}
