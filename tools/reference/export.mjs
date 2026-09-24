// Writes the color core's reference test data: the conversions in web-platform-tests' CSS color
// lists, and seeded colors converted by color.js. Both sources are pinned, so a rerun writes the
// same files.
//
//   cd tools/reference
//   npm ci
//   node generate.mjs
import Color from "colorjs.io";
import { writeFileSync } from "node:fs";

const WPT_COMMIT = "5a5b2b591b39c59d5bca77819db305474dcfd18a";
const WPT = `https://raw.githubusercontent.com/web-platform-tests/wpt/${WPT_COMMIT}/css/css-color/parsing`;
const OUT = new URL("../../color/src/commonTest/kotlin/codes/side/color/", import.meta.url);

// ---- web-platform-tests ----
//
// A relative color F(from X c1 c2 c3) whose channels are F's own, in order, is X converted into F's
// space: the rest of relative color syntax (calc(), reordered channels) is not conversion. X already
// in F's space tests how channel keywords resolve, not conversion, and is left out too.

const FUNCTION_SPACES = {
    rgb: "srgb", rgba: "srgb", hsl: "hsl", hsla: "hsl", hwb: "hwb",
    lab: "lab", lch: "lch", oklab: "oklab", oklch: "oklch",
};
const FUNCTION_CHANNELS = {
    rgb: "r g b", rgba: "r g b", hsl: "h s l", hsla: "h s l", hwb: "h w b",
    lab: "l a b", lch: "l c h", oklab: "l a b", oklch: "l c h",
};
const COLOR_SPACES = { "srgb": "srgb", "srgb-linear": "srgb-linear", "display-p3": "display-p3", "xyz": "xyz-d65", "xyz-d50": "xyz-d50", "xyz-d65": "xyz-d65" };
const UNSUPPORTED = ["calc(", "var(", "currentcolor", "color-mix", "light-dark", "contrast-color", "from ", "alpha(", "rec2020", "a98-rgb", "prophoto-rgb", "display-p3-linear"];

function spaceOf(color) {
    const fn = /^([\w-]+)\(/.exec(color);
    if (!fn) return "srgb"; // hex and named colors
    if (fn[1] === "color") return COLOR_SPACES[/^color\(\s*([\w-]+)/.exec(color)[1]];
    return FUNCTION_SPACES[fn[1]];
}

async function wptConversions() {
    const cases = [];
    for (const file of ["color-computed-relative-color.html", "color-computed-powerless.html", "color-computed-none.html"]) {
        const text = await (await fetch(`${WPT}/${file}`)).text();
        const calls = /(?:fuzzy_)?test_computed_(?:color|value)\((?:\s*`color`\s*,)?\s*`([^`]*)`\s*,\s*`([^`]*)`/g;
        for (const [, source, expected] of text.matchAll(calls)) {
            let target, origin;
            const fn = /^(\w+)\(from (.+) (\S+ \S+ \S+)( \/ alpha)?\)$/.exec(source.trim());
            const color = /^color\(from (.+) ([\w-]+) (\S+ \S+ \S+)( \/ alpha)?\)$/.exec(source.trim());
            if (color) {
                const [, inner, space, channels] = color;
                if (!(space in COLOR_SPACES) || channels !== (space.startsWith("xyz") ? "x y z" : "r g b")) continue;
                [origin, target] = [inner, COLOR_SPACES[space]];
            } else if (fn) {
                const [, name, inner, channels] = fn;
                if (FUNCTION_CHANNELS[name] !== channels) continue;
                [origin, target] = [inner, FUNCTION_SPACES[name]];
            } else {
                continue;
            }
            if ([origin, expected].some(t => UNSUPPORTED.some(u => t.includes(u)))) continue;
            if (spaceOf(origin) === target) continue;
            cases.push([origin, target, expected]);
        }
    }
    return cases;
}

// ---- color.js ----

// Our ids for the spaces color.js has too. It has no CMYK.
const COLORJS = {
    "srgb": "srgb", "srgb-linear": "srgb-linear", "display-p3": "p3", "xyz-d65": "xyz-d65", "xyz-d50": "xyz-d50",
    "lab": "lab", "lch": "lch", "oklab": "oklab", "oklch": "oklch", "hsl": "hsl", "hwb": "hwb", "hsv": "hsv",
};
// color.js reads HSV and HWB through HSL, whose hue turns half a turn for colors far outside sRGB
// (CSS issue 9222), so pairs with those spaces take colors inside sRGB only.
const EXTENDED = new Set(["srgb", "srgb-linear", "display-p3", "xyz-d65", "xyz-d50", "lab", "lch", "oklab", "oklch"]);

function mulberry32(seed) {
    return function () {
        seed |= 0; seed = seed + 0x6D2B79F5 | 0;
        let t = Math.imul(seed ^ seed >>> 15, 1 | seed);
        t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t;
        return ((t ^ t >>> 14) >>> 0) / 4294967296;
    };
}

const number = v => (v === null || Number.isNaN(v)) ? "none" : String(v);

// 1,000 colors over every ordered pair of distinct spaces, each pair's in turn: odd rounds of a pair
// of extended-range spaces anywhere in a box 30% past sRGB each way, the rest inside sRGB.
function crossCheck() {
    const random = mulberry32(20260924);
    const ids = Object.keys(COLORJS);
    const pairs = ids.flatMap(from => ids.filter(to => to !== from).map(to => [from, to]));
    const lines = [];
    for (let i = 0; i < 1000; i++) {
        const [from, to] = pairs[i % pairs.length];
        const wide = Math.floor(i / pairs.length) % 2 === 1 && EXTENDED.has(from) && EXTENDED.has(to);
        const rgb = [0, 1, 2].map(() => wide ? random() * 1.6 - 0.3 : random());
        const coords = new Color("srgb", rgb).to(COLORJS[from]).coords.map(c => Number.isNaN(c) || c === null ? 0 : c);
        const converted = new Color(COLORJS[from], coords).to(COLORJS[to]).coords;
        lines.push(`${from} ${coords.map(number).join(" ")} > ${to} ${converted.map(number).join(" ")}`);
    }
    return lines;
}

// Okhsl and Okhsv of sRGB colors whose OkLCh hue is outside 220°–280°, where color.js's fitted
// cusp drifts from sRGB's edge (by up to 4.4e-3 in Okhsv's s near pure blue).
function okhsx() {
    const random = mulberry32(20260925);
    const lines = [];
    while (lines.length < 400) {
        const color = new Color("srgb", [random(), random(), random()]);
        const hue = color.to("oklch").coords[2];
        if (Number.isNaN(hue) || (hue >= 220 && hue < 280)) continue;
        const okhsl = color.to("okhsl").coords;
        const okhsv = color.to("okhsv").coords;
        lines.push(`${color.coords.join(" ")} > ${okhsl.map(number).join(" ")} > ${okhsv.map(number).join(" ")}`);
    }
    return lines;
}

// ---- Kotlin ----

const kotlinString = s => `"${s.replaceAll("\\", "\\\\").replaceAll("\"", "\\\"").replaceAll("$", "\\$")}"`;

// A Kotlin string constant holds at most 65,535 bytes, so long data goes in several.
function chunked(lines) {
    const chunks = [[]];
    let size = 0;
    for (const line of lines) {
        if (size + line.length + 1 > 60000) { chunks.push([]); size = 0; }
        chunks.at(-1).push(line);
        size += line.length + 1;
    }
    return chunks.map(chunk => `    """\n${chunk.join("\n")}\n    """,\n`).join("");
}

const wpt = await wptConversions();
writeFileSync(new URL("WptConversions.kt", OUT),
    "package codes.side.color\n\n" +
    `// Generated by tools/reference/generate.mjs from web-platform-tests (BSD-3-Clause) at ${WPT_COMMIT.slice(0, 7)},\n` +
    "// css/css-color/parsing/color-computed-{relative-color,powerless,none}.html: the relative colors that\n" +
    "// only convert. Do not edit.\n\n" +
    "/** A color, the id of the space it is converted into, and how a browser serializes the result. */\n" +
    "internal val WPT_CONVERSIONS: List<Triple<String, String, String>> = listOf(\n" +
    wpt.map(([origin, target, expected]) => `    Triple(${kotlinString(origin)}, ${kotlinString(target)}, ${kotlinString(expected)}),\n`).join("") +
    ")\n");

const conversions = crossCheck();
const okhsxLines = okhsx();
writeFileSync(new URL("ColorJsReference.kt", OUT),
    "package codes.side.color\n\n" +
    "// Generated by tools/reference/generate.mjs from color.js 0.7.1 (MIT). Do not edit.\n\n" +
    "/** Seeded colors, one a line: `space components > space components`, `none` for a missing one. */\n" +
    "internal val COLORJS_CONVERSIONS: List<String> = listOf(\n" + chunked(conversions) + ")\n\n" +
    "/** sRGB colors away from blue's hues, one a line: `r g b > okhsl h s l > okhsv h s v`. */\n" +
    "internal val COLORJS_OKHSX: List<String> = listOf(\n" + chunked(okhsxLines) + ")\n");

console.log(`${wpt.length} WPT conversions, ${conversions.length} color.js conversions, ${okhsxLines.length} Okhsl/Okhsv colors`);
