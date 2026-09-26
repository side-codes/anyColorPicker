package codes.side.colorpicker.state

import androidx.compose.runtime.mutableStateMapOf
import codes.side.color.ChannelKind
import codes.side.color.ColorChannel
import codes.side.color.ColorSpace
import codes.side.color.ColorValue
import codes.side.color.Hsl
import codes.side.color.HueFamily
import codes.side.color.Lch
import codes.side.color.OkLch
import codes.side.color.Srgb

// The hue a picker shows for a color that has none, one per HueFamily. A grey has no hue to
// convert, so reading one off it reports red; the last hue actually chosen in that family is shown
// instead, as a painting tool does. Every value written teaches it, in order:
//
// 1. A hue present in the value is remembered for its family, grey or not: a conversion never gives
//    a grey a hue, so a present one was authored.
// 2. A missing hue teaches nothing, so a grey from hex or a Compose Color leaves every family alone.
// 3. Every other family takes the hue of the value converted into its space, when that has one.
// 4. A family the conversion gave no hue, after rule 1 remembered a new one, takes the hue of the
//    first family's reference color at it: that family's space at the hue, its other channels at
//    their anchors. The same hue again carries nothing, since on the way to grey every family took
//    the exact angle from the colored value, where the reference color is a few degrees off.
//
// Carrying a hue across families is this library's own; CSS keeps none (csswg-drafts #8484).
internal class HueMemory {

    // The space each family's conversions go through: the library's for its own families, and for
    // any other the first space seen with it.
    private val spaces = linkedMapOf<HueFamily, ColorSpace>(
        HueFamily.rgbHexcone(Srgb) to Hsl,
        HueFamily.Oklab to OkLch,
        HueFamily.CieLab to Lch,
    )

    private val hues = mutableStateMapOf<HueFamily, Double>()

    /**
     * The hue remembered for [channel]'s family. A family no value has taught yet, such as one of an
     * app's spaces before a color in it is written, takes the hue a family with one would carry into
     * it by rule 4; null when no family has a hue.
     */
    fun hue(channel: ColorChannel): Double? {
        val family = channel.family ?: return null
        return hues[family] ?: carried(channel)
    }

    // Rule 4 worked on reading: the reference color of the first family with a hue, converted into
    // [channel]'s space. Nothing is written, so reading stays free to happen in any snapshot.
    private fun carried(channel: ColorChannel): Double? {
        for ((family, space) in spaces) {
            val hue = hues[family] ?: continue
            referenceColor(space, hue).to(channel.space)[channel]?.let { return it }
        }
        return null
    }

    /** Every remembered hue, by family. */
    val remembered: Map<HueFamily, Double> get() = hues.toMap()

    /**
     * Puts back hues saved earlier, over the ones [learn] remembered. A saved family whose space is
     * among [known] joins again, so later values keep teaching it; one whose space is not keeps its
     * hue until a color in that space is written.
     */
    fun restore(saved: Map<HueFamily, Double>, known: Collection<ColorSpace>) {
        hues.putAll(saved)
        for (space in known) {
            val family = space.hueChannel()?.family ?: continue
            if (family in saved) spaces.getOrPut(family) { space }
        }
    }

    fun learn(color: ColorValue) {
        val own = color.space.hueChannel()
        val ownFamily = own?.family
        var reference: ColorValue? = null
        if (own != null && ownFamily != null) {
            val space = spaces.getOrPut(ownFamily) { color.space }
            val hue = color[own]
            if (hue != null && hues[ownFamily] != hue) {
                hues[ownFamily] = hue
                reference = referenceColor(space, hue)
            }
        }
        for ((family, space) in spaces) {
            if (family == ownFamily) continue
            val channel = space.hueChannel() ?: continue
            val hue = color.to(space)[channel] ?: reference?.to(space)?.get(channel) ?: continue
            if (hues[family] != hue) hues[family] = hue
        }
    }
}

internal fun ColorSpace.hueChannel(): ColorChannel? = channels.firstOrNull { it.isHue }

internal val ColorChannel.family: HueFamily? get() = (kind as? ChannelKind.Hue)?.family
