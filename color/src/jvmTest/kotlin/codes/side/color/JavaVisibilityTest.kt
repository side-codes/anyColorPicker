package codes.side.color

import java.lang.reflect.Modifier
import kotlin.test.Test
import kotlin.test.assertEquals

class JavaVisibilityTest {

    @Test
    fun theMissingAlphaFlagIsNoPublicField() {
        // Kotlin's internal is public to Java: an internal const in a companion is a public static
        // field on the class, where Java code can read and depend on it.
        val classes = listOfNotNull(
            ColorValue::class.java,
            ColorValue.Companion::class.java,
            runCatching { Class.forName("codes.side.color.ColorValueKt") }.getOrNull(),
        )
        val exposed = classes.flatMap { type ->
            type.declaredFields
                .filter { Modifier.isPublic(it.modifiers) && "ALPHA" in it.name }
                .map { "${type.name}.${it.name}" }
        }
        assertEquals(emptyList(), exposed)
    }
}
