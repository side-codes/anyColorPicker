package codes.side.color

import codes.side.color.internal.chromaWithin
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertTrue

// Every 0.05° and every hundredth of lightness, too slow for the browser: the walk against the closed forms.
class EdgeWalkSweepTest {

    @Test
    fun theWalkLandsWhereTheClosedFormsDoEverywhere() {
        val rec2020 = ColorSpace.rgb("--rec2020-sweep", RgbPrimaries(0.708, 0.292, 0.170, 0.797, 0.131, 0.046), WhitePoint.D65, TransferFunction.Srgb)
        val pureBlue = Srgb(0.0, 0.0, 1.0).to(OkLch)[OkLch.H]!!
        val hues = List(7200) { it * 0.05 } + listOf(245.1, 245.2, 264.05, 264.1, 264.2, pureBlue)
        val failures = ArrayList<String>()
        for (gamut in listOf(Srgb.gamut, DisplayP3.gamut, rec2020.gamut)) {
            val t = gamut.lmsToLinear
            for (hue in hues) {
                val a = cos(hue * PI / 180.0)
                val b = sin(hue * PI / 180.0)
                for (step in 1..99) {
                    val l = step / 100.0
                    val edge = chromaWithin(t, l, a, b, 10.0)
                    for (chroma in listOf(edge * (1.0 + 1e-7), edge * 1.3 + 1e-3, 0.6, 0.27)) {
                        val apart = EdgeWalkTest.disagreement(t, l, a, b, chromaWithin(t, l, a, b, chroma), chromaWithin(t, l, a, b, chroma, iterative = true))
                        if (apart != null && failures.size < 10) failures += "$gamut at L $l, $hue°, chroma $chroma: $apart"
                    }
                }
            }
        }
        assertTrue(failures.isEmpty(), failures.joinToString("\n"))
    }
}
