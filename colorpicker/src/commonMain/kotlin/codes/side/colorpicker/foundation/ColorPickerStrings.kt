package codes.side.colorpicker.foundation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalLocale
import codes.side.color.ColorChannel
import codes.side.color.ColorSpace
import codes.side.colorpicker.ui.PlaneActionLabels

/**
 * Every word and number the picker components show or speak.
 *
 * The library's own are English words, with numbers in the format of the current locale: `40%`, `40 %` in French,
 * `%40` in Turkish, `٤٠٪` in Egyptian Arabic. To reword or translate them, implement this interface, overriding only
 * the members you need, and provide the implementation with [ProvideColorPickerStrings]. Components read it in their
 * parameter defaults, so an argument passed to a component wins over the provided strings, and the provided strings
 * over the library's.
 *
 * Every member has a body in the library's own words, so a member added in a later version reaches an existing
 * implementation in those words rather than breaking it. Members are composable: an implementation can read the app's
 * own resources with `stringResource`.
 */
@Stable
public interface ColorPickerStrings {

    /** The label above [channel]'s slider: "Saturation", "L"; an app channel's id. */
    @Composable
    public fun channelName(channel: ColorChannel): String = EnglishText.channelName(channel)

    /** What a screen reader calls [channel]: its name, except Lab's, which are read as L*, a* and b*. */
    @Composable
    public fun channelSpokenName(channel: ColorChannel): String = EnglishText.channelSpokenName(channel)

    /**
     * [channel]'s [value] as its slider shows it, or, when [forAccessibility], as a screen reader announces it. RGB reads
     * in 0 to 255, a hue in degrees, lightness and saturation in percent, and an app channel with the decimals its step
     * needs.
     */
    @Composable
    public fun channelValue(channel: ColorChannel, value: Double, forAccessibility: Boolean): String =
        EnglishText.channelValue(channel, value, rememberNumberFormatter())

    /** The alpha slider's label and what a screen reader calls it: "Alpha". */
    @Composable
    public fun alphaName(): String = EnglishText.alphaName()

    /** [alpha] as its slider shows it, 0 to 255, or, when [forAccessibility], as a screen reader announces it. */
    @Composable
    public fun alphaValue(alpha: Double, forAccessibility: Boolean): String =
        EnglishText.alphaValue(alpha, rememberNumberFormatter())

    /** Where the thumb of a slider over no channel sits, as a screen reader announces it: "37%". */
    @Composable
    public fun sliderPosition(fraction: Float): String = EnglishText.sliderPosition(fraction, rememberNumberFormatter())

    /** [space]'s name, as a switcher between spaces shows it: "Okhsl", "OkLCh", "HSV", "RGB"; an app space's id. */
    @Composable
    public fun spaceName(space: ColorSpace): String = EnglishText.spaceName(space)

    /** What a screen reader calls a plane over [x] and [y]: "Saturation and lightness". */
    @Composable
    public fun planeDescription(x: ColorChannel, y: ColorChannel): String = EnglishText.planeDescription(x, y)

    /** A plane's two values as a screen reader announces them: "40% saturation, 60% lightness". */
    @Composable
    public fun planeValue(x: ColorChannel, xValue: Double, y: ColorChannel, yValue: Double): String =
        EnglishText.planeValue(x, xValue, y, yValue, rememberNumberFormatter())

    /** The four accessibility actions of a plane over [x] and [y]: "Increase saturation", and so on. */
    @Composable
    public fun planeActions(x: ColorChannel, y: ColorChannel): PlaneActionLabels = EnglishText.planeActions(x, y)

    /** The four accessibility actions of a plane that does not know its channels: "Increase horizontally", and so on. */
    @Composable
    public fun planeAxisActions(): PlaneActionLabels = EnglishText.planeAxisActions()

    /** The color dialog's title: "Select color". */
    @Composable
    public fun dialogTitle(): String = EnglishText.dialogTitle()

    /** The color dialog's confirm button: "OK". */
    @Composable
    public fun confirm(): String = EnglishText.confirm()

    /** The color dialog's dismiss button: "Cancel". */
    @Composable
    public fun dismiss(): String = EnglishText.dismiss()

    /** What a screen reader calls the color a comparison started from: "Original color". */
    @Composable
    public fun originalColor(): String = EnglishText.originalColor()

    /** What a screen reader calls the color a comparison shows beside the original: "New color". */
    @Composable
    public fun newColor(): String = EnglishText.newColor()

    /** The action that puts the original color back: "Restore original color". */
    @Composable
    public fun restoreOriginal(): String = EnglishText.restoreOriginal()

    public companion object {
        /** The strings the nearest [ProvideColorPickerStrings] provided, or the library's own. */
        public val current: ColorPickerStrings
            @Composable get() = LocalColorPickerStrings.current ?: LibraryStrings
    }
}

/** Provides [strings] to every picker component in [content]; a nested call replaces them for its own content. */
@Composable
public fun ProvideColorPickerStrings(strings: ColorPickerStrings, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalColorPickerStrings provides strings, content = content)
}

// Private, so its type stays out of the API: apps provide strings through ProvideColorPickerStrings and read them
// through ColorPickerStrings.current. Static, because strings rarely change and replacing them recomposes everything
// below anyway.
private val LocalColorPickerStrings = staticCompositionLocalOf<ColorPickerStrings?> { null }

// The library's own words, for a composition nothing was provided to.
private object LibraryStrings : ColorPickerStrings

/** A formatter for the current locale, built again only when the locale changes. */
@Composable
internal fun rememberNumberFormatter(): NumberFormatter {
    val tag = LocalLocale.current.toLanguageTag()
    return remember(tag) { NumberFormatter(tag) }
}
