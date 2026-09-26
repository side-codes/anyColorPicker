package codes.side.colorpicker.ui

import codes.side.color.ColorChannel
import codes.side.color.ColorSpaces
import codes.side.color.Hsl
import codes.side.color.Lab
import codes.side.color.OkLch
import codes.side.color.Okhsl
import codes.side.color.Okhsv
import codes.side.color.Srgb
import codes.side.colorpicker.state.ColoringMode
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class TrackStopsTest {

    // Budget the drawn track is held to, and how close to a jump in the true colors it may blur.
    private val budget = 2.0 / 255.0
    private val nearJump = 1.0 / 2048.0
    private val jump = 8.0 / 255.0

    // What a gradient through [stops] draws at [fraction]: a straight run between the stops around it.
    private fun drawn(stops: TrackStops, fraction: Double): DoubleArray {
        val positions = checkNotNull(stops.positions)
        var s = 0
        while (s < positions.size - 2 && positions[s + 1] < fraction) s++
        val t = ((fraction - positions[s]) / (positions[s + 1] - positions[s])).coerceIn(0.0, 1.0)
        val a = stops.colors[s]
        val b = stops.colors[s + 1]
        return doubleArrayOf(
            a.red + (b.red - a.red) * t,
            a.green + (b.green - a.green) * t,
            a.blue + (b.blue - a.blue) * t,
        )
    }

    // The true sRGB color of [channel] at [fraction] of its reference range, the rest at [held].
    private fun truth(channel: ColorChannel, held: DoubleArray, fraction: Double): DoubleArray {
        val color = held.copyOf()
        color[channel.index] = channelValueAt(channel, channel.referenceRange, fraction.coerceIn(0.0, 1.0))
        val rgb = DoubleArray(3)
        Srgb.gamut.mapper(channel.space).convert(color, rgb)
        return rgb
    }

    @Test
    fun everyLibraryTrackStaysWithinTwoStepsOfItsColors() {
        val random = Random(2026)
        for (space in ColorSpaces.all) for (channel in space.channels) for (mode in ColoringMode.entries) repeat(8) {
            val displayed = DoubleArray(space.channels.size) { i ->
                val range = space.channels[i].referenceRange
                range.start + random.nextDouble() * (range.endInclusive - range.start)
            }
            val held = heldComponents(channel, displayed, mode)
            val stops = trackStops(channel, held, channel.referenceRange)
            assertTrue(stops.colors.size <= TRACK_MAX_STOPS, "$channel $mode took ${stops.colors.size} stops")
            for (k in 0 until 1024) {
                val fraction = k / 1023.0
                val shown = drawn(stops, fraction)
                val at = truth(channel, held, fraction)
                val before = truth(channel, held, fraction - nearJump)
                val after = truth(channel, held, fraction + nearJump)
                // Within a hair of a jump in the true colors, any color between its two sides is right.
                val jumps = (0..2).any { abs(after[it] - before[it]) > jump }
                for (c in 0..2) {
                    val error = if (jumps) {
                        val low = min(at[c], min(before[c], after[c]))
                        val high = max(at[c], max(before[c], after[c]))
                        max(0.0, max(low - shown[c], shown[c] - high))
                    } else {
                        abs(shown[c] - at[c])
                    }
                    assertTrue(
                        error <= budget,
                        "$channel $mode at ${decimals(fraction, 4)}, held ${held.toList()}: channel $c off by ${decimals(error * 255.0, 2)}/255",
                    )
                }
            }
        }
    }

    @Test
    fun aJumpIsDrawnNarrowerThanAPixel() {
        // Okhsv's hue jumps by about 15/255 at sRGB blue, where its cusp moves to the outer stretch of
        // the gamut.
        val held = heldComponents(Okhsv.H, doubleArrayOf(0.0, 0.85, 1.0), ColoringMode.Independent)
        val stops = trackStops(Okhsv.H, held, Okhsv.H.referenceRange)
        val positions = checkNotNull(stops.positions)
        val bracketed = (0 until positions.size - 1).any { i ->
            val a = stops.colors[i]
            val b = stops.colors[i + 1]
            val step = maxOf(abs(b.red - a.red), abs(b.green - a.green), abs(b.blue - a.blue))
            positions[i + 1] - positions[i] <= TRACK_MIN_SEGMENT.toFloat() && step > 10f / 255f
        }
        assertTrue(bracketed, "no pair of stops 1/4096 of the track apart brackets the jump")
    }

    @Test
    fun anIndependentTrackHoldsTheAnchorsAndTheDisplayedHue() {
        val displayed = doubleArrayOf(200.0, 30.0, 20.0)
        assertEquals(listOf(200.0, 0.0, 50.0), heldComponents(Hsl.S, displayed, ColoringMode.Independent).toList())
        assertEquals(listOf(0.0, 100.0, 50.0), heldComponents(Hsl.H, displayed, ColoringMode.Independent).toList())
        assertEquals(listOf(200.0, 0.0, 20.0), heldComponents(Hsl.S, displayed, ColoringMode.Contextual).toList())
        assertEquals(listOf(0.0, 0.0, 0.0), heldComponents(Lab.L, doubleArrayOf(40.0, 20.0, -30.0), ColoringMode.Independent).toList())
    }

    @Test
    fun theRightEndOfAHueStopsBelow360() {
        assertEquals(LAST_HUE, channelValueAt(Hsl.H, Hsl.H.referenceRange, 1.0))
        assertTrue(LAST_HUE < 360.0 && Hsl(LAST_HUE, 50.0, 50.0)[Hsl.H] == LAST_HUE, "the last hue must not wrap to 0")
        assertEquals(LAST_HUE, clampToRange(Hsl.H, Hsl.H.referenceRange, 361.0))
        assertEquals(1.0, clampToRange(Okhsl.S, Okhsl.S.referenceRange, 1.2))
    }

    @Test
    fun aValueOutsideTheRangePinsToItsEnd() {
        assertEquals(1f, fractionOf(0.5, OkLch.C.referenceRange))
        assertEquals(0f, fractionOf(-0.2, Srgb.R.referenceRange))
        assertEquals(0.25f, fractionOf(90.0, Hsl.H.referenceRange))
    }

    @Test
    fun aRangeMustBeASpanWithinTheLimit() {
        requireSliderRange(Okhsl.S, 0.2..0.8)
        requireSliderRange(OkLch.C, 0.0..0.5)
        assertFailsWith<IllegalArgumentException> { requireSliderRange(Okhsl.S, 0.0..2.0) }
        assertFailsWith<IllegalArgumentException> { requireSliderRange(Hsl.S, -10.0..100.0) }
        assertFailsWith<IllegalArgumentException> { requireSliderRange(Hsl.H, 90.0..90.0) }
        assertFailsWith<IllegalArgumentException> { requireSliderRange(Lab.A, 0.0..Double.POSITIVE_INFINITY) }
    }
}
