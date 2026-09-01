package codes.side.colorpicker.sample

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport

/**
 * Runs the shared sample in a browser on Kotlin/Wasm:
 * `./gradlew :sample:webApp:wasmJsBrowserDevelopmentRun`.
 *
 * The same [SampleApp] the Android, iOS and desktop samples use.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport {
        SampleApp()
    }
}
