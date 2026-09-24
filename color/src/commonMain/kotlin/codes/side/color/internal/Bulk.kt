package codes.side.color.internal

/**
 * Checks a bulk call: [count] colors of [sourceSize] components each, read from [srcOffset] in an
 * array of [srcSize], written as [targetSize] components each from [dstOffset] in an array of
 * [dstSize]. The two may be one array ([sameArray]) as long as no color is written over one not yet
 * read.
 */
internal fun checkBulk(
    sourceSize: Int,
    targetSize: Int,
    srcSize: Int,
    srcOffset: Int,
    dstSize: Int,
    dstOffset: Int,
    count: Int,
    sameArray: Boolean,
    sourceName: String,
    targetName: String,
) {
    require(count >= 0 && srcOffset >= 0 && dstOffset >= 0) { "Negative count or offset" }
    // Long, so an absurd count cannot wrap around and pass.
    val srcEnd = srcOffset + count.toLong() * sourceSize
    val dstEnd = dstOffset + count.toLong() * targetSize
    require(srcEnd <= srcSize) { "Source holds fewer than $count $sourceName colors" }
    require(dstEnd <= dstSize) { "Destination has room for fewer than $count $targetName colors" }
    if (sameArray && count > 1 && dstEnd > srcOffset && srcEnd > dstOffset) {
        // Each color is read whole before it is written, so a color's write may reach no further
        // than the next color's read: dstOffset − srcOffset ≤ (k + 1)(sourceSize − targetSize) for
        // every k up to count − 2, tightest at the first k or the last.
        val step = (sourceSize - targetSize).toLong()
        val bound = if (step >= 0) step else (count - 1) * step
        require(dstOffset - srcOffset <= bound) {
            "Converting in place from $srcOffset to $dstOffset would overwrite $sourceName colors not yet read"
        }
    }
}
