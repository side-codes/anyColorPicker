package codes.side.color

import kotlin.math.abs
import kotlin.math.max
import kotlin.test.Test
import kotlin.test.assertTrue

class ReferenceVectorsTest {

    @Test
    fun wptConversionsMatchBrowsers() {
        // WPT prints 6 to 8 significant digits, so each channel is compared within 2e-6 of its range.
        val report = StringBuilder()
        for ((source, spaceId, serialized) in WPT.conversions) {
            val target = ColorSpaces.all.first { it.id == spaceId }
            var converted = ColorValue.parseCss(source).to(target)
            // The result is a CSS function's value, whose lightness clamps as parsing clamps it.
            if (target in listOf(Lab, Lch, Oklab, OkLch)) {
                val lightness = target.channels[0]
                converted[lightness]?.let { converted = converted.with(lightness, it.coerceIn(lightness.referenceRange)) }
            }
            val expected = ColorValue.parseCss(serialized)
            val at = "$source in $spaceId, browsers $serialized"
            report.compare(if (expected.space == target) converted else converted.to(expected.space), expected, at) {
                2e-6 * max(1.0, abs(it.referenceRange.endInclusive))
            }
        }
        assertTrue(report.isEmpty(), report.toString())
    }

    @Test
    fun cssWorkedExamplesHoldToTheDigitsTheyPrint() {
        val report = StringBuilder()
        for (case in CSS.workedExamples) {
            val space = ColorSpaces.all.first { it.id == case.equals.space }
            val expected = space.colorOf(case.equals.components.toTypedArray(), 1.0)
            val actual = ColorValue.parseCss(case.color).to(space)
            report.compare(actual, expected, "${case.example}: ${case.color} as $expected") { case.tolerance[it.index] + 1e-12 }
        }
        assertTrue(report.isEmpty(), report.toString())
    }

    @Test
    fun conversionsMatchColorJs() {
        val report = StringBuilder()
        for (case in COLORJS.conversions) {
            val source = reference(case.from)
            val target = reference(case.to)
            report.compare(source.to(target.space), target, "$case") { 1e-9 * max(1.0, abs(it.referenceRange.endInclusive)) }
        }
        assertTrue(report.isEmpty(), report.toString())
    }

    @Test
    fun okhslAndOkhsvMatchColorJsAwayFromBlue() {
        // color.js's Okhsl and Okhsv rest on a fitted cusp and a one-step edge; here they are solved,
        // which is 3.9e-5 and 4.6e-7 apart at worst away from the hues around pure blue.
        val report = StringBuilder()
        for (case in COLORJS.okhsx) {
            val (rgb, okhsl, okhsv) = case
            val color = Srgb(rgb[0], rgb[1], rgb[2])
            report.compare(color.to(Okhsl), Okhsl(okhsl[0], okhsl[1], okhsl[2]), "$case") { if (it.isHue) 1e-9 else 5e-5 }
            report.compare(color.to(Okhsv), Okhsv(okhsv[0], okhsv[1], okhsv[2]), "$case") { if (it.isHue) 1e-9 else 1e-6 }
        }
        assertTrue(report.isEmpty(), report.toString())
    }

    private fun reference(value: ColorJsReference.Components): ColorValue =
        ColorSpaces.all.first { it.id == value.space }.colorOf(value.components.toTypedArray(), 1.0)

    private fun StringBuilder.compare(actual: ColorValue, expected: ColorValue, at: String, tolerance: (ColorChannel) -> Double) {
        for (channel in expected.space.channels) {
            val want = expected[channel]
            val have = actual.components()[channel.index]
            if (want == null) {
                if (!actual.isMissing(channel)) appendLine("$at: ${channel.id} is $have, not missing")
                continue
            }
            if (actual.isMissing(channel)) {
                appendLine("$at: ${channel.id} is missing, not $want")
                continue
            }
            var difference = abs(have - want)
            if (channel.isHue) difference = minOf(difference, 360.0 - difference)
            if (difference > tolerance(channel)) appendLine("$at: ${channel.id} is $have, off by $difference")
        }
    }
}
