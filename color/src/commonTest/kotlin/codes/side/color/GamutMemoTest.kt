package codes.side.color

import codes.side.color.internal.GamutMemo
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GamutMemoTest {

    @Test
    fun aRememberedHueIsItsOwnCosineAndSine() {
        // −0.0 and +0.0 are equal, but their sines are not the same bits.
        val memo = GamutMemo()
        for (hue in listOf(200.0, 264.1, 200.0, -0.0, 0.0, -0.0, 360.0, 1e-300, 200.0)) {
            memo.hue(hue)
            assertEquals(cos(hue * PI / 180.0).toRawBits(), memo.hueCos.toRawBits(), "cos $hue")
            assertEquals(sin(hue * PI / 180.0).toRawBits(), memo.hueSin.toRawBits(), "sin $hue")
        }
    }

    @Test
    fun anOkhslRowIsKnownByItsExactLightnessAndHue() {
        val memo = GamutMemo()
        assertFalse(memo.hasOkhslRow(Double.NaN, Double.NaN, Double.NaN), "nothing is remembered at first")
        assertFalse(memo.hasOkhslRow(0.0, 0.0, 0.0), "nothing is remembered at first")
        memo.rememberOkhslRow(0.5, 1.0, 0.0, l = 0.4, c0 = 0.1, cMid = 0.2, cMax = 0.3)
        assertTrue(memo.hasOkhslRow(0.5, 1.0, 0.0))
        assertFalse(memo.hasOkhslRow(0.5, 1.0, -0.0))
        assertFalse(memo.hasOkhslRow(0.5000000000000001, 1.0, 0.0))
        assertEquals(listOf(0.4, 0.1, 0.2, 0.3), listOf(memo.okhslL, memo.okhslC0, memo.okhslCMid, memo.okhslCMax))
    }
}
