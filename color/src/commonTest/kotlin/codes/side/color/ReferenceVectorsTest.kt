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
        for ((source, spaceId, serialized) in WPT_CONVERSIONS) {
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
        for (example in cssExamples) {
            val expected = example.second
            val actual = ColorValue.parseCss(example.first).to(expected.space)
            report.compare(actual, expected, "${example.first} as $expected") { example.tolerance[it.index] + 1e-12 }
        }
        assertTrue(report.isEmpty(), report.toString())
    }

    @Test
    fun conversionsMatchColorJs() {
        val report = StringBuilder()
        for (line in COLORJS_CONVERSIONS.flatMap { it.lines() }.filter { it.isNotBlank() }) {
            val (source, target) = line.split(" > ").map { reference(it.trim()) }
            report.compare(source.to(target.space), target, line) { 1e-9 * max(1.0, abs(it.referenceRange.endInclusive)) }
        }
        assertTrue(report.isEmpty(), report.toString())
    }

    @Test
    fun okhslAndOkhsvMatchColorJsAwayFromBlue() {
        // color.js's Okhsl and Okhsv rest on a fitted cusp and a one-step edge; here they are solved,
        // which is 3.9e-5 and 4.6e-7 apart at worst away from the hues around pure blue.
        val report = StringBuilder()
        for (line in COLORJS_OKHSX.flatMap { it.lines() }.filter { it.isNotBlank() }) {
            val (rgb, okhsl, okhsv) = line.split(" > ").map { part -> part.trim().split(" ").map { if (it == "none") null else it.toDouble() } }
            val color = Srgb(rgb[0], rgb[1], rgb[2])
            report.compare(color.to(Okhsl), Okhsl(okhsl[0], okhsl[1], okhsl[2]), line) { if (it.isHue) 1e-9 else 5e-5 }
            report.compare(color.to(Okhsv), Okhsv(okhsv[0], okhsv[1], okhsv[2]), line) { if (it.isHue) 1e-9 else 1e-6 }
        }
        assertTrue(report.isEmpty(), report.toString())
    }

    // `space c0 c1 c2`, with `none` for a missing component.
    private fun reference(text: String): ColorValue {
        val parts = text.split(" ")
        val space = ColorSpaces.all.first { it.id == parts[0] }
        var missing = 0
        val components = DoubleArray(space.channels.size) { i ->
            val value = parts[i + 1]
            if (value == "none") missing = missing or (1 shl i)
            if (value == "none") 0.0 else value.toDouble()
        }
        return space.color(components, missing = missing)
    }

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

    // CSS Color 4 (Editor's Draft 2026-09-13), its worked examples: a color, the same color written in
    // another space, and each of that one's components rounded to half a unit of the last digit
    // printed. Examples written before the draft's current matrices are left out:
    // lch(51.2345% 21.2 130) as srgb and display-p3, #7654CD as xyz-d50 and xyz-d65,
    // lch(85.9017% 166.116 138.207) as display-p3, rgb(76% 62% 3%) as lab and lch, and
    // color(display-p3 0.84 0.19 0.72) as lab and lch, which color.js 0.7.1 converts as this library
    // does, up to 1.4e-2 from the printed values.
    private class CssExample(val first: String, val second: ColorValue, val tolerance: DoubleArray)

    private val cssExamples = listOf(
        CssExample("lch(51.2345% 21.2 130)", ColorValue.parseCss("lab(51.2345% -13.6271 16.2401)"), doubleArrayOf(5e-5, 5e-5, 5e-5)),
        CssExample("hsl(240deg 100% 50%)", ColorValue.parseCss("oklch(0.452 0.313 264.1)"), doubleArrayOf(5e-4, 5e-4, 0.05)),
        CssExample("hsl(60deg 100% 50%)", ColorValue.parseCss("oklch(0.968 0.211 109.8)"), doubleArrayOf(5e-4, 5e-4, 0.05)),
        CssExample("hsl(220deg 100% 50%)", ColorValue.parseCss("oklch(0.533 0.26 262.6)"), doubleArrayOf(5e-4, 5e-3, 0.05)),
        CssExample("hsl(250deg 100% 50%)", ColorValue.parseCss("oklch(0.462 0.306 268.9)"), doubleArrayOf(5e-4, 5e-4, 0.05)),
        CssExample("hsl(50deg 100% 50%)", ColorValue.parseCss("oklch(0.882 0.181 94.24)"), doubleArrayOf(5e-4, 5e-4, 5e-3)),
        CssExample("hsl(80deg 100% 50%)", ColorValue.parseCss("oklch(0.91 0.245 129.9)"), doubleArrayOf(5e-3, 5e-4, 0.05)),
        CssExample("hwb(150 20% 10%)", ColorValue.parseCss("hsl(150 77.78% 55%)"), doubleArrayOf(1e-9, 5e-3, 1e-9)),
        CssExample("hwb(150 20% 10%)", ColorValue.parseCss("rgb(20% 90% 55%)"), doubleArrayOf(5e-3, 5e-3, 5e-3)),
        CssExample("hwb(45 40% 80%)", ColorValue.parseCss("rgb(33.33% 33.33% 33.33%)"), doubleArrayOf(5e-5, 5e-5, 5e-5)),
        CssExample("#7654CD", ColorValue.parseCss("rgb(46.27% 32.94% 80.39%)"), doubleArrayOf(5e-5, 5e-5, 5e-5)),
        CssExample("#7654CD", ColorValue.parseCss("lab(44.36% 36.05 -58.99)"), doubleArrayOf(5e-3, 5e-3, 5e-3)),
        CssExample("oklch(65% 0.15 270)", ColorValue.parseCss("lab(57.9% 11.4 -53.7)"), doubleArrayOf(0.05, 0.05, 0.05)),
        CssExample("oklch(65% 0.15 270)", ColorValue.parseCss("color(display-p3 0.445 0.529 0.891)"), doubleArrayOf(5e-4, 5e-4, 5e-4)),
        CssExample("oklch(65% 0.15 270)", ColorValue.parseCss("#6c88ea"), doubleArrayOf(0.5 / 255, 0.5 / 255, 0.5 / 255)),
        CssExample("oklch(65% 0.25 270)", ColorValue.parseCss("lab(56.03% 32.3 -88.58)"), doubleArrayOf(5e-3, 0.05, 5e-3)),
        CssExample("oklch(65% 0.25 270)", ColorValue.parseCss("color(display-p3 0.3731 0.4673 1.105)"), doubleArrayOf(5e-5, 5e-5, 5e-4)),
        CssExample("oklch(65% 0.25 270)", ColorValue.parseCss("color(srgb 0.3475 0.4707 1.146)"), doubleArrayOf(5e-5, 5e-5, 5e-4)),
        // Lightness past 100%, which lab() would clamp.
        CssExample("color(xyz-d65 1 1 1)", Lab(100.1154, 9.064489, 5.801761), doubleArrayOf(5e-5, 5e-7, 5e-7)),
        CssExample("color(display-p3 0.7 0.5 none)", ColorValue.parseCss("oklch(63.612% 0.1522 78.748)"), doubleArrayOf(5e-6, 5e-5, 5e-4)),
        CssExample("oklch(76% 0.27 60)", ColorValue.parseCss("color(display-p3 1.062 0.4954 -0.3115)"), doubleArrayOf(5e-4, 5e-5, 5e-5)),
        CssExample("hsl(38.824 100% 50%)", ColorValue.parseCss("rgb(255 165 0)"), doubleArrayOf(0.5 / 255, 0.5 / 255, 0.5 / 255)),
        CssExample("hwb(740deg 20% 30%)", ColorValue.parseCss("rgb(178.5 93.5 51)"), doubleArrayOf(0.05 / 255, 0.05 / 255, 0.5 / 255)),
        CssExample("lab(50% 0 0)", ColorValue.parseCss("oklab(0.56897 0 0)"), doubleArrayOf(5e-6, 1e-9, 1e-9)),
    )
}
