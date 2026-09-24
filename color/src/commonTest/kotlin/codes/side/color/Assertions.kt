package codes.side.color

import kotlin.math.abs
import kotlin.test.assertTrue

internal fun assertNear(expected: Double, actual: Double, tolerance: Double, message: String = "") {
    assertTrue(abs(expected - actual) <= tolerance, "$message expected $expected, was $actual (diff ${abs(expected - actual)})")
}

internal fun assertComponents(expected: DoubleArray, actual: ColorValue, tolerance: Double) {
    val components = actual.components()
    for (i in expected.indices) {
        assertNear(expected[i], components[i], tolerance, "${actual.space.id}.${actual.space.channels[i].id}")
    }
}
