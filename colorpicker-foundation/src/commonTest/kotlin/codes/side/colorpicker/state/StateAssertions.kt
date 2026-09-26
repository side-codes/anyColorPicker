package codes.side.colorpicker.state

import kotlin.math.abs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal fun assertNear(expected: Double, actual: Double?, tolerance: Double = 1e-9, message: String = "") {
    assertNotNull(actual, "$message expected $expected, was null")
    assertTrue(abs(expected - actual) <= tolerance, "$message expected $expected, was $actual (diff ${abs(expected - actual)})")
}
