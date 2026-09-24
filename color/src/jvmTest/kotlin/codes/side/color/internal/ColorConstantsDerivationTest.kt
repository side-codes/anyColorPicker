package codes.side.color.internal

import java.math.BigDecimal
import java.math.BigInteger
import java.math.MathContext
import kotlin.math.abs
import kotlin.math.cbrt
import kotlin.math.nextDown
import kotlin.math.nextUp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Recomputes ColorConstants.kt from CSS Color 4's definitions. A derived literal must be the double
 * nearest its exact value and a published one CSS's printed text, so a constant edited by hand
 * cannot drift by a single ulp. On failure the expected list is the literal to paste.
 */
class ColorConstantsDerivationTest {

    @Test
    fun whitesAreTheirChromaticities() {
        assertNearest("D65_XYZ", d65, D65_XYZ)
        assertNearest("D50_XYZ", d50, D50_XYZ)
    }

    @Test
    fun rgbMatricesAreDerivedFromTheirPrimaries() {
        assertNearest("SRGB_LINEAR_TO_XYZ_D65", srgbToXyz, SRGB_LINEAR_TO_XYZ_D65)
        assertNearest("XYZ_D65_TO_SRGB_LINEAR", inverse(srgbToXyz), XYZ_D65_TO_SRGB_LINEAR)
        assertNearest("DISPLAY_P3_LINEAR_TO_XYZ_D65", displayP3ToXyz, DISPLAY_P3_LINEAR_TO_XYZ_D65)
        assertNearest("XYZ_D65_TO_DISPLAY_P3_LINEAR", inverse(displayP3ToXyz), XYZ_D65_TO_DISPLAY_P3_LINEAR)
    }

    @Test
    fun publishedMatricesAreCssText() {
        for ((name, text, literal) in published) assertEquals(text.map { it.toDouble() }, literal.toList(), name)
    }

    @Test
    fun lmsShortcutsAreExactProducts() {
        // Multiplied as CSS's printed decimals, not as the doubles they round to.
        val lmsToXyz = cssLmsToXyz.map { Rational.of(it) }
        val xyzToLms = cssXyzToLms.map { Rational.of(it) }
        assertNearest("LMS_TO_SRGB_LINEAR", product(inverse(srgbToXyz), lmsToXyz), LMS_TO_SRGB_LINEAR)
        assertNearest("SRGB_LINEAR_TO_LMS", product(xyzToLms, srgbToXyz), SRGB_LINEAR_TO_LMS)
        assertNearest("LMS_TO_DISPLAY_P3_LINEAR", product(inverse(displayP3ToXyz), lmsToXyz), LMS_TO_DISPLAY_P3_LINEAR)
        assertNearest("DISPLAY_P3_LINEAR_TO_LMS", product(xyzToLms, displayP3ToXyz), DISPLAY_P3_LINEAR_TO_LMS)
    }

    @Test
    fun publishedMatricesCarryTheWhites() {
        // Bradford takes D65 to D50 and back, and Oklab puts D65 at L = 1, a = b = 0: CSS's values
        // do so within 6e-16, exactly and in doubles alike.
        assertClose("XYZ_D65_TO_D50 · D65", transform(XYZ_D65_TO_D50, D65_XYZ), D50_XYZ)
        assertClose("XYZ_D50_TO_D65 · D50", transform(XYZ_D50_TO_D65, D50_XYZ), D65_XYZ)
        val lms = transform(XYZ_D65_TO_LMS, D65_XYZ)
        for (k in 0..2) lms[k] = cbrt(lms[k])
        assertClose("Oklab of D65", transform(LMS_TO_OKLAB, lms), doubleArrayOf(1.0, 0.0, 0.0))
    }

    private val d65 = xyzOf("0.3127", "0.3290")
    private val d50 = xyzOf("0.3457", "0.3585")
    private val srgbToXyz = rgbToXyz(listOf("0.640" to "0.330", "0.300" to "0.600", "0.150" to "0.060"))
    private val displayP3ToXyz = rgbToXyz(listOf("0.680" to "0.320", "0.265" to "0.690", "0.150" to "0.060"))

    // CSS Color 4, "Sample code for color conversions", as printed there.
    private val cssD65ToD50 = listOf(
        "1.0479297925449969", "0.022946870601609652", "-0.05019226628920524",
        "0.02962780877005599", "0.9904344267538799", "-0.017073799063418826",
        "-0.009243040646204504", "0.015055191490298152", "0.7518742814281371",
    )
    private val cssD50ToD65 = listOf(
        "0.955473421488075", "-0.02309845494876471", "0.06325924320057072",
        "-0.0283697093338637", "1.0099953980813041", "0.021041441191917323",
        "0.012314014864481998", "-0.020507649298898964", "1.330365926242124",
    )
    private val cssXyzToLms = listOf(
        "0.8190224379967030", "0.3619062600528904", "-0.1288737815209879",
        "0.0329836539323885", "0.9292868615863434", "0.0361446663506424",
        "0.0481771893596242", "0.2642395317527308", "0.6335478284694309",
    )
    private val cssLmsToXyz = listOf(
        "1.2268798758459243", "-0.5578149944602171", "0.2813910456659647",
        "-0.0405757452148008", "1.1122868032803170", "-0.0717110580655164",
        "-0.0763729366746601", "-0.4214933324022432", "1.5869240198367816",
    )
    private val cssLmsToOklab = listOf(
        "0.2104542683093140", "0.7936177747023054", "-0.0040720430116193",
        "1.9779985324311684", "-2.4285922420485799", "0.4505937096174110",
        "0.0259040424655478", "0.7827717124575296", "-0.8086757549230774",
    )
    private val cssOklabToLms = listOf(
        "1.0000000000000000", "0.3963377773761749", "0.2158037573099136",
        "1.0000000000000000", "-0.1055613458156586", "-0.0638541728258133",
        "1.0000000000000000", "-0.0894841775298119", "-1.2914855480194092",
    )
    private val published = listOf(
        Triple("XYZ_D65_TO_D50", cssD65ToD50, XYZ_D65_TO_D50),
        Triple("XYZ_D50_TO_D65", cssD50ToD65, XYZ_D50_TO_D65),
        Triple("XYZ_D65_TO_LMS", cssXyzToLms, XYZ_D65_TO_LMS),
        Triple("LMS_TO_XYZ_D65", cssLmsToXyz, LMS_TO_XYZ_D65),
        Triple("LMS_TO_OKLAB", cssLmsToOklab, LMS_TO_OKLAB),
        Triple("OKLAB_TO_LMS", cssOklabToLms, OKLAB_TO_LMS),
    )

    private fun assertNearest(name: String, exact: List<Rational>, literal: DoubleArray) {
        assertEquals(exact.map { it.toDouble() }, literal.toList(), name)
    }

    private fun assertClose(name: String, actual: DoubleArray, expected: DoubleArray) {
        for (k in expected.indices) {
            assertTrue(abs(actual[k] - expected[k]) <= 1e-15, "$name[$k] is ${actual[k]}, not ${expected[k]}")
        }
    }

    private fun transform(m: DoubleArray, v: DoubleArray): DoubleArray =
        DoubleArray(3) { r -> m[r * 3] * v[0] + m[r * 3 + 1] * v[1] + m[r * 3 + 2] * v[2] }

    // XYZ with Y = 1 at chromaticity (x, y).
    private fun xyzOf(x: String, y: String): List<Rational> {
        val cx = Rational.of(x)
        val cy = Rational.of(y)
        return listOf(cx / cy, Rational.ONE, (Rational.ONE - cx - cy) / cy)
    }

    // CSS's derivation: the primaries' XYZ as columns, each scaled so that RGB white lands on D65.
    private fun rgbToXyz(primaries: List<Pair<String, String>>): List<Rational> {
        val columns = primaries.map { (x, y) -> xyzOf(x, y) }
        val unscaled = List(9) { columns[it % 3][it / 3] }
        val scale = product(inverse(unscaled), d65)
        return List(9) { unscaled[it] * scale[it % 3] }
    }

    // A row-major 3 × 3 matrix times another, or times a 3-vector.
    private fun product(m: List<Rational>, n: List<Rational>): List<Rational> {
        val columns = n.size / 3
        return List(n.size) { index ->
            val row = index / columns
            val column = index % columns
            (0..2).map { k -> m[row * 3 + k] * n[k * columns + column] }.reduce { sum, term -> sum + term }
        }
    }

    private fun inverse(m: List<Rational>): List<Rational> {
        val (a, b, c) = m.subList(0, 3)
        val (d, e, f) = m.subList(3, 6)
        val (g, h, i) = m.subList(6, 9)
        val det = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g)
        return listOf(
            (e * i - f * h) / det, (c * h - b * i) / det, (b * f - c * e) / det,
            (f * g - d * i) / det, (a * i - c * g) / det, (c * d - a * f) / det,
            (d * h - e * g) / det, (b * g - a * h) / det, (a * e - b * d) / det,
        )
    }

    private class Rational private constructor(val numerator: BigInteger, val denominator: BigInteger) {
        operator fun plus(other: Rational): Rational =
            of(numerator * other.denominator + other.numerator * denominator, denominator * other.denominator)

        operator fun minus(other: Rational): Rational =
            of(numerator * other.denominator - other.numerator * denominator, denominator * other.denominator)

        operator fun times(other: Rational): Rational = of(numerator * other.numerator, denominator * other.denominator)

        operator fun div(other: Rational): Rational = of(numerator * other.denominator, denominator * other.numerator)

        operator fun compareTo(other: Rational): Int = (numerator * other.denominator).compareTo(other.numerator * denominator)

        fun abs(): Rational = of(numerator.abs(), denominator)

        // Sixty digits settle the double unless the value lies within 1e-60 of the midpoint between
        // two; comparing against both neighbours rules that out.
        fun toDouble(): Double {
            val candidate = BigDecimal(numerator).divide(BigDecimal(denominator), MathContext(60)).toDouble()
            val error = (exact(candidate) - this).abs()
            check(error <= (exact(candidate.nextUp()) - this).abs() && error <= (exact(candidate.nextDown()) - this).abs()) {
                "$candidate is not the double nearest $this"
            }
            return candidate
        }

        override fun toString(): String = "$numerator/$denominator"

        companion object {
            val ONE: Rational = of(BigInteger.ONE, BigInteger.ONE)

            fun of(numerator: BigInteger, denominator: BigInteger): Rational {
                require(denominator.signum() != 0) { "Division by zero" }
                val gcd = numerator.gcd(denominator).let { if (denominator.signum() < 0) it.negate() else it }
                return Rational(numerator / gcd, denominator / gcd)
            }

            fun of(decimal: String): Rational = of(BigDecimal(decimal))

            // A double's exact binary value.
            fun exact(value: Double): Rational = of(BigDecimal(value))

            private fun of(value: BigDecimal): Rational = if (value.scale() >= 0) {
                of(value.unscaledValue(), BigInteger.TEN.pow(value.scale()))
            } else {
                of(value.unscaledValue() * BigInteger.TEN.pow(-value.scale()), BigInteger.ONE)
            }
        }
    }
}
