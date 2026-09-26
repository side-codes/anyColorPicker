package codes.side.colorpicker.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.LocalSaveableStateRegistry
import androidx.compose.runtime.saveable.SaveableStateRegistry
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi

/**
 * Recreates its content from what the content saved, as a configuration change does: every `rememberSaveable`
 * below [Content] is saved, the content is disposed, and it is composed again from the saved values.
 *
 * Compose's own `StateRestorationTester` does this on Android only; on desktop it throws. Only strings,
 * numbers, booleans and lists of them are accepted, which is what every platform's saved state holds.
 */
@OptIn(ExperimentalTestApi::class)
internal class Recreation {

    private var registry by mutableStateOf(SaveableStateRegistry(restoredValues = null, canBeSaved = ::isSavable))
    private var shown by mutableStateOf(true)

    @Composable
    fun Content(content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalSaveableStateRegistry provides registry) {
            if (shown) content()
        }
    }

    fun recreate(test: ComposeUiTest) {
        lateinit var saved: Map<String, List<Any?>>
        test.runOnIdle {
            saved = registry.performSave()
            shown = false
        }
        test.runOnIdle {
            registry = SaveableStateRegistry(restoredValues = saved, canBeSaved = ::isSavable)
            shown = true
        }
        test.waitForIdle()
    }
}

private fun isSavable(value: Any?): Boolean = when (value) {
    null, is String, is Int, is Long, is Double, is Float, is Boolean -> true
    is List<*> -> value.all(::isSavable)
    else -> false
}
