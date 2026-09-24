package codes.side.color.internal

import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ColorConstantsTest {

    @Test
    fun theSrgbMatrixIsCssRationals() {
        // CSS Color 4 publishes lin_sRGB_to_XYZ as these fractions, so each literal must be the
        // correctly rounded fraction, bit for bit.
        val css = doubleArrayOf(
            506752.0 / 1228815.0, 87881.0 / 245763.0, 12673.0 / 70218.0,
            87098.0 / 409605.0, 175762.0 / 245763.0, 12673.0 / 175545.0,
            7918.0 / 409605.0, 87881.0 / 737289.0, 1001167.0 / 1053270.0,
        )
        for (i in css.indices) assertEquals(css[i], SRGB_LINEAR_TO_XYZ_D65[i], "element $i")
    }

    @Test
    fun whitesAreCssChromaticities() {
        assertEquals(0.9504559270516717, D65_XYZ[0])
        assertEquals(1.0890577507598784, D65_XYZ[2])
        assertEquals(0.9642956764295676, D50_XYZ[0])
        assertEquals(0.8251046025104602, D50_XYZ[2])
    }

    @Test
    fun forwardAndInverseMatricesArePairs() {
        val pairs = listOf(
            SRGB_LINEAR_TO_XYZ_D65 to XYZ_D65_TO_SRGB_LINEAR,
            DISPLAY_P3_LINEAR_TO_XYZ_D65 to XYZ_D65_TO_DISPLAY_P3_LINEAR,
            XYZ_D65_TO_D50 to XYZ_D50_TO_D65,
            XYZ_D65_TO_LMS to LMS_TO_XYZ_D65,
            LMS_TO_OKLAB to OKLAB_TO_LMS,
        )
        for ((forward, inverse) in pairs) assertIdentity(multiply(inverse, forward))
    }

    @Test
    fun theShortcutsAreTheProductsOfTheirParts() {
        val shortcuts = listOf(
            LMS_TO_SRGB_LINEAR to multiply(XYZ_D65_TO_SRGB_LINEAR, LMS_TO_XYZ_D65),
            SRGB_LINEAR_TO_LMS to multiply(XYZ_D65_TO_LMS, SRGB_LINEAR_TO_XYZ_D65),
            LMS_TO_DISPLAY_P3_LINEAR to multiply(XYZ_D65_TO_DISPLAY_P3_LINEAR, LMS_TO_XYZ_D65),
            DISPLAY_P3_LINEAR_TO_LMS to multiply(XYZ_D65_TO_LMS, DISPLAY_P3_LINEAR_TO_XYZ_D65),
        )
        for ((shortcut, product) in shortcuts) {
            for (i in 0 until 9) assertTrue(abs(product[i] - shortcut[i]) <= 1e-15, "element $i")
        }
    }

    @Test
    fun srgbWhiteLandsOnD65() {
        for (row in 0..2) {
            val sum = SRGB_LINEAR_TO_XYZ_D65[row * 3] + SRGB_LINEAR_TO_XYZ_D65[row * 3 + 1] + SRGB_LINEAR_TO_XYZ_D65[row * 3 + 2]
            assertTrue(abs(sum - D65_XYZ[row]) <= 1e-15, "row $row sums to $sum")
        }
    }

    private fun assertIdentity(m: DoubleArray) {
        for (i in 0 until 9) {
            val expected = if (i % 4 == 0) 1.0 else 0.0
            assertTrue(abs(m[i] - expected) <= 1e-12, "element $i was ${m[i]}")
        }
    }
}
