package codes.side.colorpicker.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.LayoutDirection
import codes.side.color.ColorChannel
import codes.side.color.Srgb
import codes.side.colorpicker.state.ColoringMode
import codes.side.colorpicker.state.anchorOf
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A track's gradient: [colors] at [positions] along it, from 0 at the start to 1 at the end, or
 * evenly spaced when [positions] is null.
 */
@Immutable
internal class TrackStops(val colors: List<Color>, val positions: FloatArray?) {

    init {
        require(positions == null || positions.size == colors.size) { "Each stop needs a position" }
    }

    /** The gradient, mirrored in right-to-left layouts as the slider is. */
    fun brush(direction: LayoutDirection): Brush {
        val mirrored = direction == LayoutDirection.Rtl
        if (positions == null) return Brush.horizontalGradient(if (mirrored) colors.reversed() else colors)
        val last = colors.size - 1
        val stops = Array(colors.size) { i ->
            if (mirrored) 1f - positions[last - i] to colors[last - i] else positions[i] to colors[i]
        }
        return Brush.horizontalGradient(*stops)
    }
}

/** The largest hue below 360. A hue of 360 is stored as 0, which would throw a thumb at the right end back to the left. */
internal val LAST_HUE: Double = Double.fromBits(360.0.toRawBits() - 1)

/** [value] held to [range] for a slider or plane on [channel], where a hue stops just below 360. */
internal fun clampToRange(channel: ColorChannel, range: ClosedFloatingPointRange<Double>, value: Double): Double {
    val held = value.coerceIn(range.start, range.endInclusive)
    return if (channel.isHue && held >= 360.0) LAST_HUE else held
}

/** The value [fraction] of the way along [range]. */
internal fun channelValueAt(channel: ColorChannel, range: ClosedFloatingPointRange<Double>, fraction: Double): Double =
    clampToRange(channel, range, range.start + (range.endInclusive - range.start) * fraction)

/** How far along [range] [value] sits, held to 0..1, so a value outside the range pins to its end. */
internal fun fractionOf(value: Double, range: ClosedFloatingPointRange<Double>): Float =
    ((value - range.start) / (range.endInclusive - range.start)).coerceIn(0.0, 1.0).toFloat()

/**
 * @throws IllegalArgumentException unless [range] is a finite span of increasing values within
 * [channel]'s limit.
 */
internal fun requireSliderRange(channel: ColorChannel, range: ClosedFloatingPointRange<Double>) {
    val span = range.endInclusive - range.start
    require(span > 0.0 && span.isFinite()) { "A slider on $channel needs a finite range of increasing values, was $range" }
    val limit = channel.limit
    require(limit == null || (range.start in limit && range.endInclusive in limit)) { "A slider on $channel takes a range within $limit, was $range" }
}

/**
 * The components a track on [channel] holds still, from [displayed], every channel of its space as it
 * is shown: all of them for a contextual track, and for an independent one each at its anchor except a
 * hue, which stays as displayed. [channel]'s own slot is 0; the track fills it.
 */
internal fun heldComponents(channel: ColorChannel, displayed: DoubleArray, mode: ColoringMode): DoubleArray {
    val channels = channel.space.channels
    return DoubleArray(channels.size) { i ->
        val other = channels[i]
        when {
            other === channel -> 0.0
            mode == ColoringMode.Contextual || other.isHue -> displayed[i]
            else -> anchorOf(other)
        }
    }
}

/** A track starts from this many even segments, then splits where its color bends. */
internal const val TRACK_SEED_SEGMENTS: Int = 32

/**
 * How far the straight run between two stops may stray from the true color at a quarter, half or three
 * quarters of the way, per sRGB channel. Testing the midpoint alone misses a kink between it and a stop,
 * where chroma reduction starts: tracks drawn that way strayed by up to 21/255.
 */
internal const val TRACK_TOLERANCE: Double = 1.0 / 255.0

/**
 * The narrowest segment a track splits, well under a pixel on any screen. A jump in the true colors,
 * such as Okhsv's hue crossing sRGB blue, is drawn as an edge rather than split forever.
 */
internal const val TRACK_MIN_SEGMENT: Double = 1.0 / 4096.0

/** The most stops a track takes. */
internal const val TRACK_MAX_STOPS: Int = 256

// Where along a segment a split is tested; the middle one becomes the new stop.
private val PROBES = doubleArrayOf(0.25, 0.5, 0.75)
private const val MIDDLE_PROBE = 1

/**
 * The stops that draw [channel]'s track over [range], every other channel at [held]. The colors are
 * computed in [channel]'s space and brought into sRGB by chroma reduction, one bulk call per round of
 * splitting: a segment is split while the straight run across it strays from its true color by more
 * than [TRACK_TOLERANCE], down to [TRACK_MIN_SEGMENT] and up to [TRACK_MAX_STOPS] stops.
 */
internal fun trackStops(channel: ColorChannel, held: DoubleArray, range: ClosedFloatingPointRange<Double>): TrackStops {
    val sample = trackSampler(channel, held, range)
    var fractions = DoubleArray(TRACK_SEED_SEGMENTS + 1) { it.toDouble() / TRACK_SEED_SEGMENTS }
    var rgb = sample(fractions)
    // Whether the segment after each stop is still to be tested; the last stop has none.
    var open = BooleanArray(fractions.size) { it < TRACK_SEED_SEGMENTS }
    while (fractions.size < TRACK_MAX_STOPS) {
        val tested = open.indices.filter { open[it] }
        if (tested.isEmpty()) break
        val probed = sample(
            DoubleArray(tested.size * PROBES.size) { k ->
                val i = tested[k / PROBES.size]
                fractions[i] + (fractions[i + 1] - fractions[i]) * PROBES[k % PROBES.size]
            },
        )
        val nextFractions = ArrayList<Double>(fractions.size * 2)
        val nextRgb = ArrayList<Double>(rgb.size * 2)
        val nextOpen = ArrayList<Boolean>(fractions.size * 2)
        var stops = fractions.size
        var segment = 0
        for (i in fractions.indices) {
            nextFractions += fractions[i]
            for (c in 0..2) nextRgb += rgb[3 * i + c]
            if (!open[i]) {
                nextOpen += false
                continue
            }
            val first = segment++ * PROBES.size
            if (stops >= TRACK_MAX_STOPS || !strays(rgb, i, probed, first)) {
                nextOpen += false
                continue
            }
            stops++
            // The halves split again only while they could make segments no narrower than the floor.
            val splitAgain = (fractions[i + 1] - fractions[i]) / 2.0 >= 2.0 * TRACK_MIN_SEGMENT
            nextOpen += splitAgain
            nextFractions += (fractions[i] + fractions[i + 1]) / 2.0
            for (c in 0..2) nextRgb += probed[3 * (first + MIDDLE_PROBE) + c]
            nextOpen += splitAgain
        }
        fractions = nextFractions.toDoubleArray()
        rgb = nextRgb.toDoubleArray()
        open = nextOpen.toBooleanArray()
    }
    return TrackStops(
        colors = List(fractions.size) { Color(srgbArgb(rgb, 3 * it)) },
        positions = FloatArray(fractions.size) { fractions[it].toFloat() },
    )
}

/** The color [channel]'s track shows at [value], the rest at [held]: what its thumb is painted with. */
internal fun trackColorAt(channel: ColorChannel, held: DoubleArray, value: Double): Color {
    val color = held.copyOf()
    color[channel.index] = value
    val rgb = DoubleArray(3)
    Srgb.gamut.mapper(channel.space).convert(color, rgb)
    return Color(srgbArgb(rgb, 0))
}

/** The opaque sRGB color at [at] in [rgb], each channel rounded to a byte in Double as the color bridge rounds. */
internal fun srgbArgb(rgb: DoubleArray, at: Int): Int =
    (0xFF shl 24) or (byte(rgb[at]) shl 16) or (byte(rgb[at + 1]) shl 8) or byte(rgb[at + 2])

private fun byte(value: Double): Int = (value.coerceIn(0.0, 1.0) * 255.0).roundToInt()

// Maps fractions along [range] to sRGB, three channels each: [channel] at each fraction, the rest at [held].
private fun trackSampler(
    channel: ColorChannel,
    held: DoubleArray,
    range: ClosedFloatingPointRange<Double>,
): (DoubleArray) -> DoubleArray {
    val size = channel.space.channels.size
    val mapper = Srgb.gamut.mapper(channel.space)
    return { fractions ->
        val colors = DoubleArray(fractions.size * size)
        for (i in fractions.indices) {
            held.copyInto(colors, i * size)
            colors[i * size + channel.index] = channelValueAt(channel, range, fractions[i])
        }
        val rgb = DoubleArray(fractions.size * 3)
        mapper.convert(colors, 0, rgb, 0, fractions.size)
        rgb
    }
}

// Whether the straight run from stop [i] to the next strays from that segment's probed colors, which
// start at probe [first] of [probed], by more than TRACK_TOLERANCE in any channel.
private fun strays(rgb: DoubleArray, i: Int, probed: DoubleArray, first: Int): Boolean {
    for (p in PROBES.indices) {
        for (c in 0..2) {
            val start = rgb[3 * i + c]
            val straight = start + (rgb[3 * (i + 1) + c] - start) * PROBES[p]
            if (abs(probed[3 * (first + p) + c] - straight) > TRACK_TOLERANCE) return true
        }
    }
    return false
}
