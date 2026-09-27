package codes.side.color

import java.io.DataInputStream
import kotlin.test.Test
import kotlin.test.assertTrue

// The newest class-file version Java 17 loads.
private const val JAVA_17 = 61

class BytecodeLevelTest {

    // A Compose Desktop app running on JDK 17 loads the -jvm jar. Left unpinned, Kotlin targets whatever JDK the
    // build runs on, and the jar resolves fine and then fails with UnsupportedClassVersionError.
    @Test
    fun theJvmClassesLoadOnJava17() {
        val stream = checkNotNull(ColorValue::class.java.getResourceAsStream("ColorValue.class"))
        val major = DataInputStream(stream).use { input ->
            input.readInt()
            input.readUnsignedShort()
            input.readUnsignedShort()
        }
        assertTrue(major <= JAVA_17, "ColorValue.class is class-file version $major, past Java 17's $JAVA_17")
    }
}
