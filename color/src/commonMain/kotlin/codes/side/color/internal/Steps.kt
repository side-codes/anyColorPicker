package codes.side.color.internal

/** The most components any space has: CMYK's four. */
internal const val MAX_COMPONENTS: Int = 4

/** One stage of a conversion, applied in place to a buffer of [MAX_COMPONENTS]. */
internal fun interface Step {
    fun apply(v: DoubleArray)
}

/** A 3×3 row-major matrix applied to the first three components. Adjacent ones fuse into one. */
internal class MatrixStep(val m: DoubleArray) : Step {
    override fun apply(v: DoubleArray) {
        val x = v[0]
        val y = v[1]
        val z = v[2]
        v[0] = m[0] * x + m[1] * y + m[2] * z
        v[1] = m[3] * x + m[4] * y + m[5] * z
        v[2] = m[6] * x + m[7] * y + m[8] * z
    }
}

/** Merges each run of adjacent [MatrixStep]s into one. */
internal fun fuse(steps: List<Step>): List<Step> {
    val fused = ArrayList<Step>(steps.size)
    for (step in steps) {
        val last = fused.lastOrNull()
        if (step is MatrixStep && last is MatrixStep) {
            fused[fused.size - 1] = MatrixStep(multiply(step.m, last.m))
        } else {
            fused += step
        }
    }
    return fused
}

/** Runs [steps] over [src]'s first [count] components into [dst], which may be [src]. */
internal fun runSteps(steps: List<Step>, src: DoubleArray, dst: DoubleArray, count: Int) {
    if (src !== dst) src.copyInto(dst, 0, 0, count)
    for (step in steps) step.apply(dst)
}
