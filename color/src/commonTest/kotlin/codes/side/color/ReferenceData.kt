package codes.side.color

import com.goncalossilva.resources.Resource
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

// The JSON files under src/commonTest/resources/reference, exported by tools/reference/export.mjs from
// pinned sources that each file's `source` names. Decoding is strict: a field this schema does not
// know fails the tests rather than being skipped.

/** CSS Color 4's own source: its named-color table, in 8-bit sRGB. */
@Serializable
internal data class CssReference(val source: JsonObject, val namedColors: List<NamedColor>) {
    @Serializable
    data class NamedColor(val name: String, val rgb: List<Int>)
}

/** web-platform-tests' css/css-color/parsing: its parsing lists, and the relative colors that only convert. */
@Serializable
internal data class WptReference(
    val source: JsonObject,
    /** Colors and how a browser serializes them. */
    val valid: List<Serialized>,
    /** The same for `color()`, with `{space}` standing for each predefined space. */
    val validColorFunction: List<Serialized>,
    /** Text that is not a color. */
    val invalid: List<String>,
    /** A color, the id of the space it is converted into, and how a browser serializes the result. */
    val conversions: List<Conversion>,
) {
    @Serializable
    data class Serialized(val color: String, val serialized: String)

    @Serializable
    data class Conversion(val color: String, val space: String, val serialized: String)
}

/** Seeded colors converted by color.js; a missing component is null. */
@Serializable
internal data class ColorJsReference(
    val source: JsonObject,
    val conversions: List<Conversion>,
    /** sRGB colors away from blue's hues, with their Okhsl and Okhsv. */
    val okhsx: List<Okhsx>,
) {
    @Serializable
    data class Components(val space: String, val components: List<Double?>)

    @Serializable
    data class Conversion(val from: Components, val to: Components)

    @Serializable
    data class Okhsx(val srgb: List<Double>, val okhsl: List<Double?>, val okhsv: List<Double?>)
}

internal val CSS: CssReference by lazy { reference("css-color-4.json") }

internal val WPT: WptReference by lazy { reference("wpt.json") }

internal val COLORJS: ColorJsReference by lazy { reference("colorjs.json") }

private inline fun <reified T> reference(name: String): T = Json.decodeFromString(Resource("reference/$name").readText())
