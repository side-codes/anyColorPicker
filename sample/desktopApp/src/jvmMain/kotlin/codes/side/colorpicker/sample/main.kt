package codes.side.colorpicker.sample

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

/**
 * Runs the shared sample on the desktop: `./gradlew :sample:desktopApp:run`.
 *
 * The same [SampleApp] the Android and iOS samples use, so the pickers can be looked at
 * without an emulator or a device.
 */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "anyColorPicker",
        state = rememberWindowState(size = DpSize(720.dp, 960.dp)),
    ) {
        SampleApp()
    }
}
