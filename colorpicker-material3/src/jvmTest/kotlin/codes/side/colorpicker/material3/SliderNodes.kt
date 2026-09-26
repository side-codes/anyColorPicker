package codes.side.colorpicker.material3

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag

/**
 * The slider inside the component tagged [tag]. The tag lands on the column that holds the
 * labels and the slider, and the slider's own node is the one with a progress range.
 */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.sliderIn(tag: String): SemanticsNodeInteraction =
    onNode(hasAnyAncestor(hasTestTag(tag)) and SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))

/** The slider a screen reader calls [name], such as "Hue" or "Alpha". */
@OptIn(ExperimentalTestApi::class)
internal fun ComposeUiTest.sliderNamed(name: String): SemanticsNodeInteraction =
    onNode(hasContentDescription(name) and SemanticsMatcher.keyIsDefined(SemanticsProperties.ProgressBarRangeInfo))
