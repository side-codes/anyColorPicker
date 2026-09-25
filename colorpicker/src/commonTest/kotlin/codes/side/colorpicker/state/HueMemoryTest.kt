package codes.side.colorpicker.state

import codes.side.color.ColorSpace
import codes.side.color.ColorValue
import codes.side.color.DisplayP3
import codes.side.color.Hsl
import codes.side.color.Hsv
import codes.side.color.HueFamily
import codes.side.color.Hwb
import codes.side.color.Lch
import codes.side.color.OkLch
import codes.side.color.Okhsl
import codes.side.color.Okhsv
import codes.side.color.Srgb
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HueMemoryTest {

    private val grey = Srgb(0.5, 0.5, 0.5)

    private fun memoryOf(vararg colors: ColorValue) = HueMemory().apply { colors.forEach(::learn) }

    @Test
    fun aFamilyNeverTaughtHasNoHue() {
        assertNull(HueMemory().hue(Hsl.H))
    }

    @Test
    fun aPresentHueIsRememberedForItsWholeFamily() {
        val memory = memoryOf(Hsl(200.0, 80.0, 50.0))
        assertEquals(200.0, memory.hue(Hsl.H))
        assertEquals(200.0, memory.hue(Hsv.H), "HSV shares HSL's hexcone angle")
        assertEquals(200.0, memory.hue(Hwb.H), "and so does HWB")
    }

    @Test
    fun aHueWrittenOnAGreyIsRemembered() {
        assertEquals(120.0, memoryOf(Hsl(200.0, 80.0, 50.0), Hsl(120.0, 0.0, 50.0)).hue(Hsl.H))
    }

    @Test
    fun aGreyFromAnotherSpaceLeavesEveryFamilyAlone() {
        val chosen = Hsl(200.0, 80.0, 50.0)
        val memory = memoryOf(chosen, grey)
        assertEquals(200.0, memory.hue(Hsl.H))
        assertNear(chosen.to(OkLch)[OkLch.H]!!, memory.hue(Okhsl.H), message = "Oklab's family")
        assertNear(chosen.to(Lch)[Lch.H]!!, memory.hue(Lch.H), message = "CIELab's family")
    }

    @Test
    fun hueSurvivesBlackAndWhite() {
        val memory = memoryOf(Hsl(200.0, 80.0, 50.0), Srgb(0.0, 0.0, 0.0), Srgb(1.0, 1.0, 1.0))
        assertEquals(200.0, memory.hue(Hsl.H))
    }

    @Test
    fun theOkSpacesShareTheirHue() {
        val memory = memoryOf(Okhsl(140.0, 0.9, 0.5), grey)
        assertEquals(140.0, memory.hue(Okhsl.H))
        assertEquals(140.0, memory.hue(Okhsv.H))
        assertEquals(140.0, memory.hue(OkLch.H))
    }

    @Test
    fun aColoredValueTeachesEveryFamily() {
        val blue = Srgb(0.2, 0.4, 0.8)
        val memory = memoryOf(blue)
        assertNear(blue.to(Hsl)[Hsl.H]!!, memory.hue(Hsl.H))
        assertNear(blue.to(OkLch)[OkLch.H]!!, memory.hue(OkLch.H))
        assertNear(blue.to(Lch)[Lch.H]!!, memory.hue(Lch.H))
    }

    @Test
    fun theLastHueChosenAnywhereIsTheOneRemembered() {
        val red = Srgb(1.0, 0.0, 0.0)
        val memory = memoryOf(Hsl(200.0, 80.0, 50.0), red, grey)
        assertNear(0.0, memory.hue(Hsl.H))
        assertNear(red.to(OkLch)[OkLch.H]!!, memory.hue(Okhsl.H))
    }

    @Test
    fun aColoredConversionIsNotOverridden() {
        assertNear(0.0, memoryOf(Hsl(200.0, 80.0, 50.0), Srgb(1.0, 0.0, 0.0)).hue(Hsl.H))
    }

    @Test
    fun anHslHueTakenToWhiteKeepsTheOkAngleItHad() {
        // The same hue again carries nothing across: on the way to white the Ok family took the
        // exact angle from the colored value, where the reference color is a few degrees off.
        val chosen = Hsl(200.0, 80.0, 50.0)
        val memory = memoryOf(chosen, Hsl(200.0, 80.0, 100.0))
        assertNear(chosen.to(OkLch)[OkLch.H]!!, memory.hue(Okhsl.H))
    }

    @Test
    fun aHueGivenAtWhiteCarriesAcrossFromTheStart() {
        val hsl = memoryOf(Hsl(200.0, 100.0, 100.0))
        assertNear(Hsl(200.0, 100.0, 50.0).to(OkLch)[OkLch.H]!!, hsl.hue(OkLch.H), message = "HSL into OkLCh")
        val ok = memoryOf(Okhsl(140.0, 1.0, 1.0))
        assertNear(OkLch(0.75, 0.12, 140.0).to(Hsl)[Hsl.H]!!, ok.hue(Hsl.H), message = "Okhsl into HSL")
        assertNear(OkLch(0.75, 0.12, 140.0).to(Lch)[Lch.H]!!, ok.hue(Lch.H), message = "Okhsl into LCH")
    }

    @Test
    fun aNewHueWrittenAtWhiteCarriesAcross() {
        val memory = memoryOf(Hsl(200.0, 80.0, 50.0), Hsl(120.0, 100.0, 100.0))
        assertNear(Hsl(120.0, 100.0, 50.0).to(OkLch)[OkLch.H]!!, memory.hue(OkLch.H))
    }

    @Test
    fun anAppSpaceJoinsWithItsOwnFamily() {
        val p3Hsl = ColorSpace.hsl("--hsl-p3", DisplayP3)
        val p3Hue = p3Hsl.channels[0]
        val orange = p3Hsl.color(doubleArrayOf(30.0, 80.0, 50.0))
        val memory = memoryOf(orange, grey)
        assertEquals(30.0, memory.hue(p3Hue))
        assertNear(orange.to(Hsl)[Hsl.H]!!, memory.hue(Hsl.H), message = "sRGB's hexcone is another family, taught by the conversion")
        val red = Srgb(1.0, 0.0, 0.0)
        memory.learn(red)
        assertNear(red.to(p3Hsl)[p3Hue]!!, memory.hue(p3Hue), message = "a colored value teaches the app's family too")
    }

    @Test
    fun aFamilyNeverSeenTakesItsHueFromOneThatWas() {
        val p3Hsl = ColorSpace.hsl("--hsl-p3", DisplayP3)
        val memory = memoryOf(Hsl(200.0, 80.0, 50.0), grey)
        assertNear(Hsl(200.0, 100.0, 50.0).to(p3Hsl)[p3Hsl.H]!!, memory.hue(p3Hsl.H))
    }

    @Test
    fun restoredHuesReplaceLearnedOnes() {
        val memory = memoryOf(Hsl(200.0, 80.0, 50.0))
        memory.restore(mapOf(HueFamily.rgbHexcone(Srgb) to 90.0, HueFamily("--elsewhere") to 10.0), emptyList())
        assertEquals(90.0, memory.hue(Hsl.H))
        assertEquals(mapOf(HueFamily.rgbHexcone(Srgb) to 90.0, HueFamily("--elsewhere") to 10.0), memory.remembered - HueFamily.Oklab - HueFamily.CieLab)
    }
}
