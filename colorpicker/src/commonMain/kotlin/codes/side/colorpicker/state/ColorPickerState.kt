package codes.side.colorpicker.state

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import codes.side.colorpicker.conversion.toArgbInt
import codes.side.colorpicker.conversion.toCmyk
import codes.side.colorpicker.conversion.toHsl
import codes.side.colorpicker.conversion.toLab
import codes.side.colorpicker.conversion.toRgb
import codes.side.colorpicker.conversion.toRgbColor
import codes.side.colorpicker.model.CmykColor
import codes.side.colorpicker.model.HslColor
import codes.side.colorpicker.model.LabColor
import codes.side.colorpicker.model.PickerColor
import codes.side.colorpicker.model.RgbColor

/**
 * Single source of truth for the current color.
 *
 * Internally stores every color space natively. When the user edits via one
 * space (e.g. drags the Hue slider), only that space is authoritative — all
 * others are forward-converted from it. This guarantees zero round-trip drift:
 * the authoritative value is never re-derived from a conversion.
 */
@Stable
class ColorPickerState internal constructor(initialColor: PickerColor) {

    // The authoritative space — whichever was last written to.
    private var origin by mutableStateOf(
        when (initialColor) {
            is HslColor -> Origin.HSL
            is RgbColor -> Origin.RGB
            is CmykColor -> Origin.CMYK
            is LabColor -> Origin.LAB
        }
    )

    // Native storage per space. Only `origin` is authoritative;
    // the rest are lazily recomputed when read after the origin changes.
    private var hsl by mutableStateOf(initialColor.toHslSeed())
    private var rgb by mutableStateOf(initialColor.toRgbSeed())
    private var cmyk by mutableStateOf(initialColor.toCmykSeed())
    private var lab by mutableStateOf(initialColor.toLabSeed())
    private var dirty by mutableStateOf(0) // version counter to trigger recomposition

    var isInteracting: Boolean by mutableStateOf(false)
        internal set

    // ---- Public read access ----

    val hslColor: HslColor get() {
        ensureFresh()
        return hsl
    }

    val rgbColor: RgbColor get() {
        ensureFresh()
        return rgb
    }

    val cmykColor: CmykColor get() {
        ensureFresh()
        return cmyk
    }

    val labColor: LabColor get() {
        ensureFresh()
        return lab
    }

    val argbInt: Int get() = rgbColor.toArgbInt()

    // ---- HSL updates (no conversion — direct write) ----

    fun updateHue(hue: Float) {
        hsl = hsl.copy(hue = hue.coerceIn(0f, 360f))
        changeOrigin(Origin.HSL)
    }

    fun updateSaturation(saturation: Float) {
        hsl = hsl.copy(saturation = saturation.coerceIn(0f, 1f))
        changeOrigin(Origin.HSL)
    }

    fun updateLightness(lightness: Float) {
        hsl = hsl.copy(lightness = lightness.coerceIn(0f, 1f))
        changeOrigin(Origin.HSL)
    }

    fun updateAlpha(alpha: Float) {
        when (origin) {
            Origin.HSL -> hsl = hsl.copy(alpha = alpha.coerceIn(0f, 1f))
            Origin.RGB -> rgb = rgb.copy(alpha = alpha.coerceIn(0f, 1f))
            Origin.CMYK -> cmyk = cmyk.copy(alpha = alpha.coerceIn(0f, 1f))
            Origin.LAB -> lab = lab.copy(alpha = alpha.coerceIn(0f, 1f))
        }
        invalidate()
    }

    fun updateFromHsl(hsl: HslColor) {
        this@ColorPickerState.hsl = hsl
        changeOrigin(Origin.HSL)
    }

    // ---- RGB updates (no conversion — direct write) ----

    fun updateFromRgb(rgb: RgbColor) {
        this@ColorPickerState.rgb = rgb
        changeOrigin(Origin.RGB)
    }

    // ---- CMYK updates (no conversion — direct write) ----

    fun updateFromCmyk(cmyk: CmykColor) {
        this@ColorPickerState.cmyk = cmyk
        changeOrigin(Origin.CMYK)
    }

    // ---- LAB updates (no conversion — direct write) ----

    fun updateFromLab(lab: LabColor) {
        this@ColorPickerState.lab = lab
        changeOrigin(Origin.LAB)
    }

    // ---- ARGB Int update ----

    fun updateFromArgbInt(argb: Int) {
        rgb = argb.toRgbColor()
        changeOrigin(Origin.RGB)
    }

    /** Read the authoritative color in whichever space was last written to. */
    fun pickerColor(): PickerColor = when (origin) {
        Origin.HSL -> hslColor
        Origin.RGB -> rgbColor
        Origin.CMYK -> cmykColor
        Origin.LAB -> labColor
    }

    // ---- Internals ----

    private var lastSyncedVersion = -1

    private fun changeOrigin(newOrigin: Origin) {
        origin = newOrigin
        invalidate()
    }

    private fun invalidate() {
        dirty++
    }

    private fun ensureFresh() {
        val currentVersion = dirty
        if (lastSyncedVersion == currentVersion) return
        lastSyncedVersion = currentVersion
        syncFromOrigin()
    }

    private fun syncFromOrigin() {
        when (origin) {
            Origin.HSL -> {
                val rgb = hsl.toRgb()
                this@ColorPickerState.rgb = rgb
                cmyk = rgb.toCmyk()
                lab = rgb.toLab()
            }
            Origin.RGB -> {
                hsl = rgb.toHsl().copy(alpha = rgb.alpha)
                cmyk = rgb.toCmyk()
                lab = rgb.toLab()
            }
            Origin.CMYK -> {
                val rgb = cmyk.toRgb()
                this@ColorPickerState.rgb = rgb
                hsl = rgb.toHsl().copy(alpha = cmyk.alpha)
                lab = rgb.toLab()
            }
            Origin.LAB -> {
                val rgb = lab.toRgb()
                this@ColorPickerState.rgb = rgb
                hsl = rgb.toHsl().copy(alpha = lab.alpha)
                cmyk = rgb.toCmyk()
            }
        }
    }

    internal enum class Origin { HSL, RGB, CMYK, LAB }
}

// ---- Seed converters used at construction ----

private fun PickerColor.toHslSeed(): HslColor = when (this) {
    is HslColor -> this
    is RgbColor -> toHsl().copy(alpha = alpha)
    is CmykColor -> toRgb().toHsl().copy(alpha = alpha)
    is LabColor -> toRgb().toHsl().copy(alpha = alpha)
}

private fun PickerColor.toRgbSeed(): RgbColor = when (this) {
    is HslColor -> toRgb()
    is RgbColor -> this
    is CmykColor -> toRgb()
    is LabColor -> toRgb()
}

private fun PickerColor.toCmykSeed(): CmykColor = when (this) {
    is HslColor -> toRgb().toCmyk()
    is RgbColor -> toCmyk()
    is CmykColor -> this
    is LabColor -> toRgb().toCmyk()
}

private fun PickerColor.toLabSeed(): LabColor = when (this) {
    is HslColor -> toRgb().toLab()
    is RgbColor -> toLab()
    is CmykColor -> toRgb().toLab()
    is LabColor -> this
}
