package codes.side.colorpicker.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import codes.side.color.ColorChannel
import codes.side.color.Hsl
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ColorPickerStringsTest {

    // Overrides one channel and defers to the library for the rest.
    private object German : ColorPickerStrings {
        @Composable
        override fun channelName(channel: ColorChannel): String = if (channel === Hsl.S) "Sättigung" else super.channelName(channel)

        @Composable
        override fun dialogTitle(): String = "Farbe wählen"
    }

    private object French : ColorPickerStrings {
        @Composable
        override fun channelName(channel: ColorChannel): String = "Saturation (fr)"
    }

    @Test
    fun withoutAProviderTheWordsAreTheLibrarys() = runComposeUiTest {
        var read: List<String>? = null
        setContent {
            val strings = ColorPickerStrings.current
            read = listOf(strings.channelName(Hsl.S), strings.channelValue(Hsl.S, 40.0, false), strings.dialogTitle())
        }
        waitForIdle()
        assertEquals(listOf("Saturation", "40%", "Select color"), read)
    }

    @Test
    fun aProvidedImplementationReplacesOnlyWhatItOverrides() = runComposeUiTest {
        var read: List<String>? = null
        setContent {
            ProvideColorPickerStrings(German) {
                val strings = ColorPickerStrings.current
                read = listOf(strings.channelName(Hsl.S), strings.channelName(Hsl.L), strings.dialogTitle(), strings.confirm())
            }
        }
        waitForIdle()
        assertEquals(listOf("Sättigung", "Lightness", "Farbe wählen", "OK"), read)
    }

    @Test
    fun theInnerProviderWins() = runComposeUiTest {
        var read: String? = null
        setContent {
            ProvideColorPickerStrings(German) {
                ProvideColorPickerStrings(French) { read = ColorPickerStrings.current.channelName(Hsl.S) }
            }
        }
        waitForIdle()
        assertEquals("Saturation (fr)", read)
    }

    @Test
    fun replacingTheProvidedStringsUpdatesTheirReaders() = runComposeUiTest {
        var provided by mutableStateOf<ColorPickerStrings>(German)
        var read: String? = null
        setContent {
            ProvideColorPickerStrings(provided) { read = ColorPickerStrings.current.channelName(Hsl.S) }
        }
        waitForIdle()
        assertEquals("Sättigung", read)
        provided = French
        waitForIdle()
        assertEquals("Saturation (fr)", read)
    }

    @Test
    fun numbersFollowTheLocaleTheCompositionStartsIn() = withDefaultLocale("fr-FR") {
        runComposeUiTest {
            var read: String? = null
            setContent { read = ColorPickerStrings.current.channelValue(Hsl.S, 40.0, false) }
            waitForIdle()
            val text = checkNotNull(read)
            assertTrue(Regex("40[\u00A0\u202F]%").matches(text), text)
        }
    }
}
