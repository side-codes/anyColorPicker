package codes.side.colorpicker.preview

import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.fail

/**
 * Renders every entry in [componentPreviews] and checks it against the committed golden in
 * `docs/images/`, which the README embeds.
 *
 * Regenerate after an intentional visual change:
 * ```
 * ./gradlew :colorpicker:jvmTest --tests "*PreviewGoldenTest*" -PupdateGoldens=true
 * ```
 *
 * The comparison is deliberately tolerant. Goldens are produced on one machine and
 * verified on another, and text antialiasing differs between platforms and JDK versions,
 * so an exact match would fail for reasons that have nothing to do with the library. The
 * threshold is wide enough to absorb that and narrow enough to catch what actually
 * matters: a slider that stopped drawing, a gradient that changed, a thumb that vanished.
 */
@OptIn(ExperimentalTestApi::class)
class PreviewGoldenTest {

    private val goldenDir = File("../docs/images")
    private val updating = System.getProperty("updateGoldens") == "true"

    /** Per-channel difference above which a pixel counts as changed. */
    private val channelTolerance = 12

    /** Share of differing pixels tolerated before a preview is considered changed. */
    private val maxChangedFraction = 0.02

    @Test
    fun previewsMatchTheirGoldens() {
        val failures = mutableListOf<String>()
        var written = 0

        for (preview in componentPreviews) {
            val rendered = render(preview)
            val golden = File(goldenDir, "${preview.name}.png")

            if (updating) {
                golden.parentFile.mkdirs()
                ImageIO.write(rendered, "png", golden)
                written++
                continue
            }

            if (!golden.exists()) {
                failures += "${preview.name}: no golden at ${golden.path}"
                continue
            }
            val expected = ImageIO.read(golden)
            when (val verdict = compare(expected, rendered)) {
                is Verdict.Match -> Unit
                is Verdict.Differs -> failures += "${preview.name}: ${verdict.reason}"
            }
        }

        if (updating) {
            println("Wrote $written golden(s) to ${goldenDir.canonicalPath}")
            return
        }
        if (failures.isNotEmpty()) {
            fail(
                "Preview goldens differ:\n" + failures.joinToString("\n") { "  - $it" } +
                    "\nIf the change is intended, regenerate with -PupdateGoldens=true",
            )
        }
    }

    private fun render(preview: ComponentPreview): BufferedImage {
        lateinit var image: BufferedImage
        runComposeUiTest {
            setContent {
                androidx.compose.foundation.layout.Box(
                    Modifier.testTag(preview.name).size(preview.size.width, preview.size.height),
                ) { preview.content() }
            }
            image = onNodeWithTag(preview.name).captureToImage().toAwtImage()
        }
        return image
    }

    private sealed interface Verdict {
        object Match : Verdict
        data class Differs(val reason: String) : Verdict
    }

    private fun compare(expected: BufferedImage, actual: BufferedImage): Verdict {
        if (expected.width != actual.width || expected.height != actual.height) {
            return Verdict.Differs(
                "size ${actual.width}x${actual.height}, golden ${expected.width}x${expected.height}",
            )
        }
        var changed = 0
        for (y in 0 until expected.height) {
            for (x in 0 until expected.width) {
                val a = expected.getRGB(x, y)
                val b = actual.getRGB(x, y)
                if (a == b) continue
                val dr = abs((a shr 16 and 0xFF) - (b shr 16 and 0xFF))
                val dg = abs((a shr 8 and 0xFF) - (b shr 8 and 0xFF))
                val db = abs((a and 0xFF) - (b and 0xFF))
                if (maxOf(dr, dg, db) > channelTolerance) changed++
            }
        }
        val fraction = changed.toDouble() / (expected.width * expected.height)
        return if (fraction > maxChangedFraction) {
            Verdict.Differs("%.2f%% of pixels changed (tolerance %.0f%%)".format(fraction * 100, maxChangedFraction * 100))
        } else {
            Verdict.Match
        }
    }
}
