// Exports the color module's reference test data as JSON: CSS Color 4's named-color table and its
// worked examples of equivalent colors from its own Bikeshed source, web-platform-tests' CSS color
// parsing lists and the conversions in them, and seeded colors converted by color.js. Every source is pinned, so a rerun writes the same files. The
// tests decode them with kotlinx.serialization; nothing here writes Kotlin.
//
//   cd tools/reference
//   npm ci
//   node export.mjs
import Color from "colorjs.io";
import { mkdirSync, readFileSync, writeFileSync } from "node:fs";

// The last commit to CSS Color 4's source on 2026-09-13, the Editor's Draft the library follows.
const CSS_COMMIT = "11ab50923a2a289647bd869fcea346bc2543a0fb";
const CSS_PATH = "css-color-4/Overview.bs";
const WPT_COMMIT = "5a5b2b591b39c59d5bca77819db305474dcfd18a";
const WPT_PATH = "css/css-color/parsing";
const WPT = `https://raw.githubusercontent.com/web-platform-tests/wpt/${WPT_COMMIT}/${WPT_PATH}`;
const OUT = new URL("../../color/src/commonTest/resources/reference/", import.meta.url);

async function fetchText(url) {
    const response = await fetch(url);
    if (!response.ok) throw new Error(`${url}: HTTP ${response.status}`);
    return response.text();
}

const wptFile = name => fetchText(`${WPT}/${name}`);

// ---- CSS Color 4 ----

// The named-color table: each row gives a name, its hex and its decimal, and the two must agree.
function cssNamedColors(source) {
    const rows = /<th scope=row><dfn>([a-z]+)<\/dfn><td>#([0-9a-f]{6})<td>(\d+) (\d+) (\d+)/g;
    const colors = [];
    for (const [, name, hex, ...decimal] of source.matchAll(rows)) {
        const rgb = decimal.map(Number);
        if (rgb.map(v => v.toString(16).padStart(2, "0")).join("") !== hex) throw new Error(`${name}: #${hex} is not ${decimal.join(" ")}`);
        colors.push({ name, rgb });
    }
    return colors;
}

// §12, Comparing <color> Values: each worked example compares the first two colors it quotes, and
// says "are <em>not</em>" when they are not equivalent; its notes add positive pairs of their own.
function cssEquivalentColors(source) {
    const start = source.indexOf('<h2 id="comparing-color-values">');
    const section = source.slice(start, source.indexOf("<h2", start + 1));
    const pairs = [];
    for (const [, example, body] of section.matchAll(/<div class="example" id="(ex-equivalent-[\w-]+)">([\s\S]*?)<\/div>/g)) {
        const colors = [...body.matchAll(/''([^']+)''/g)].map(m => m[1]);
        if (colors.length < 2) throw new Error(`${example} quotes fewer than two colors`);
        const equivalent = !/are\s+<em>not<\/em>\s+\[=equivalent colors=\]/.test(body);
        pairs.push({ example, first: colors[0], second: colors[1], equivalent });
    }
    for (const [, first, second] of section.matchAll(/''([^']+)''\s+and\s+''([^']+)''—are \[=equivalent colors=\]/g)) {
        pairs.push({ example: "note", first, second, equivalent: true });
    }
    return pairs;
}

// ---- web-platform-tests: parsing ----
//
// Cases that need calc(), sign(), currentcolor or light-dark() are left out: they need a cascade or
// math functions the parser does not have.

const skipped = value => ["calc(", "sign(", "currentcolor", "light-dark"].some(u => value.toLowerCase().includes(u));

async function wptParsing() {
    const valid = [];
    for (const name of ["color-valid-rgb.html", "color-valid-hsl.html", "color-valid-hwb.html", "color-valid-lab.html"]) {
        for (const [, color, serialized] of (await wptFile(name)).matchAll(/^\s*\["([^"]*)", "([^"]*)"/gm)) {
            if (!skipped(color) && !skipped(serialized)) valid.push({ color, serialized });
        }
    }
    const general = await wptFile("color-valid.html");
    for (const [, color, serialized] of general.matchAll(/test_valid_value\("color", "([^"]*)", "([^"]*)"\)/g)) {
        if (!skipped(color)) valid.push({ color, serialized });
    }
    for (const [, color] of general.matchAll(/test_valid_value\("color", "([^"]*)"\);/g)) {
        if (!skipped(color)) valid.push({ color, serialized: color });
    }

    // color(): the file loops one template over every predefined space; {space} stands for it.
    const validColorFunction = [];
    const colorFunction = await wptFile("color-valid-color-function.html");
    for (const [, color, serialized] of colorFunction.matchAll(/test_valid_value\("color", `([^`]*)`, `([^`]*)`\)/g)) {
        if (!skipped(color)) {
            validColorFunction.push({
                color: color.replaceAll("${colorSpace}", "{space}"),
                serialized: serialized.replaceAll("${resultColorSpace}", "{space}"),
            });
        }
    }

    const invalid = [];
    for (const name of ["color-invalid.html", "color-invalid-rgb.html", "color-invalid-hsl.html", "color-invalid-hwb.html"]) {
        const text = await wptFile(name);
        invalid.push(...[...text.matchAll(/test_invalid_value\("color", "([^"]*)"\)/g)].map(m => m[1]));
        invalid.push(...[...text.matchAll(/^\s*\["([^"]*)", "[^"]*"\]/gm)].map(m => m[1]));
    }
    for (const name of ["color-invalid-hex-color.html", "color-invalid-named-color.html"]) {
        invalid.push(...[...(await wptFile(name)).matchAll(/^\s*\["([^"]*)", "[^"]*"\]/gm)].map(m => m[1]));
    }
    invalid.push(...[...(await wptFile("color-invalid-color-function.html")).matchAll(/test_invalid_value\("color", "([^"]*)"\);/g)].map(m => m[1]));
    const kept = invalid.filter(value => !skipped(value));

    // Templates looped over lists of spaces, written out once per space.
    const RGB_SPACES = ["srgb", "srgb-linear", "a98-rgb", "rec2020", "prophoto-rgb"];
    const XYZ_SPACES = ["xyz", "xyz-d50", "xyz-d65"];
    const LOOPED = { "RGB_SPACES": RGB_SPACES, "XYZ_SPACES": XYZ_SPACES, "[...RGB_SPACES, ...XYZ_SPACES]": [...RGB_SPACES, ...XYZ_SPACES] };
    for (const name of ["color-invalid-lab.html", "color-invalid-color-function.html"]) {
        const loops = /for \(const colorSpace of (\[[^\]]*\]|\[\.\.\.RGB_SPACES, \.\.\.XYZ_SPACES\]|RGB_SPACES|XYZ_SPACES)\) \{([\s\S]*?)\n\}/g;
        for (const [, list, body] of (await wptFile(name)).matchAll(loops)) {
            const spaces = LOOPED[list] ?? [...list.matchAll(/"([^"]+)"/g)].map(m => m[1]);
            for (const [, template] of body.matchAll(/test_invalid_value\("color", `([^`]*)`\)/g)) {
                for (const space of spaces) {
                    const value = template.replaceAll("${colorSpace}", space);
                    if (!skipped(value)) kept.push(value);
                }
            }
        }
    }
    return { valid, validColorFunction, invalid: kept };
}

// ---- web-platform-tests: conversions ----
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
        const text = await wptFile(file);
        const calls = /(?:fuzzy_)?test_computed_(?:color|value)\((?:\s*`color`\s*,)?\s*`([^`]*)`\s*,\s*`([^`]*)`/g;
        for (const [, source, serialized] of text.matchAll(calls)) {
            let space, color;
            const fn = /^(\w+)\(from (.+) (\S+ \S+ \S+)( \/ alpha)?\)$/.exec(source.trim());
            const colorFunction = /^color\(from (.+) ([\w-]+) (\S+ \S+ \S+)( \/ alpha)?\)$/.exec(source.trim());
            if (colorFunction) {
                const [, inner, name, channels] = colorFunction;
                if (!(name in COLOR_SPACES) || channels !== (name.startsWith("xyz") ? "x y z" : "r g b")) continue;
                [color, space] = [inner, COLOR_SPACES[name]];
            } else if (fn) {
                const [, name, inner, channels] = fn;
                if (FUNCTION_CHANNELS[name] !== channels) continue;
                [color, space] = [inner, FUNCTION_SPACES[name]];
            } else {
                continue;
            }
            if ([color, serialized].some(t => UNSUPPORTED.some(u => t.includes(u)))) continue;
            if (spaceOf(color) === space) continue;
            cases.push({ color, space, serialized });
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
// HSL, HWB and HSV are defined over sRGB's cube, and outside it their hues are still unsettled in CSS
// (issue 9222), so pairs with them take colors inside sRGB only.
const EXTENDED = new Set(["srgb", "srgb-linear", "display-p3", "xyz-d65", "xyz-d50", "lab", "lch", "oklab", "oklch"]);
const CONVERSION_SEED = 20260924;
const OKHSX_SEED = 20260925;

function mulberry32(seed) {
    return function () {
        seed |= 0; seed = seed + 0x6D2B79F5 | 0;
        let t = Math.imul(seed ^ seed >>> 15, 1 | seed);
        t = t + Math.imul(t ^ t >>> 7, 61 | t) ^ t;
        return ((t ^ t >>> 14) >>> 0) / 4294967296;
    };
}

// A missing component, NaN or null in color.js, is null here, as JSON has no NaN.
const components = coords => coords.map(c => (c === null || Number.isNaN(c)) ? null : c);

// 1,000 colors over every ordered pair of distinct spaces, each pair's in turn: odd rounds of a pair
// of extended-range spaces anywhere in a box 30% past sRGB each way, the rest inside sRGB.
function colorJsConversions() {
    const random = mulberry32(CONVERSION_SEED);
    const ids = Object.keys(COLORJS);
    const pairs = ids.flatMap(from => ids.filter(to => to !== from).map(to => [from, to]));
    const cases = [];
    for (let i = 0; i < 1000; i++) {
        const [from, to] = pairs[i % pairs.length];
        const wide = Math.floor(i / pairs.length) % 2 === 1 && EXTENDED.has(from) && EXTENDED.has(to);
        const rgb = [0, 1, 2].map(() => wide ? random() * 1.6 - 0.3 : random());
        const coords = new Color("srgb", rgb).to(COLORJS[from]).coords.map(c => Number.isNaN(c) || c === null ? 0 : c);
        const converted = new Color(COLORJS[from], coords).to(COLORJS[to]).coords;
        cases.push({ from: { space: from, components: coords }, to: { space: to, components: components(converted) } });
    }
    return cases;
}

// Okhsl and Okhsv of sRGB colors whose OkLCh hue is outside 220°–280°, where color.js's fitted
// cusp drifts from sRGB's edge (by up to 4.4e-3 in Okhsv's s near pure blue).
function colorJsOkhsx() {
    const random = mulberry32(OKHSX_SEED);
    const cases = [];
    while (cases.length < 400) {
        const color = new Color("srgb", [random(), random(), random()]);
        const hue = color.to("oklch").coords[2];
        if (Number.isNaN(hue) || (hue >= 220 && hue < 280)) continue;
        cases.push({ srgb: color.coords, okhsl: components(color.to("okhsl").coords), okhsv: components(color.to("okhsv").coords) });
    }
    return cases;
}

// ---- JSON ----

// A JSON object whose lists hold one element a line, so a changed case is a one-line diff.
function writeJson(name, data) {
    const members = Object.entries(data).map(([key, value]) => {
        if (Array.isArray(value)) {
            if (value.length === 0) throw new Error(`${name}: ${key} is empty`);
            return `  ${JSON.stringify(key)}: [\n${value.map(item => `    ${JSON.stringify(item)}`).join(",\n")}\n  ]`;
        }
        return `  ${JSON.stringify(key)}: ${JSON.stringify(value, null, 2).replaceAll("\n", "\n  ")}`;
    });
    const text = `{\n${members.join(",\n")}\n}\n`;
    JSON.parse(text);
    writeFileSync(new URL(name, OUT), text);
}

mkdirSync(OUT, { recursive: true });

const cssSource = await fetchText(`https://raw.githubusercontent.com/w3c/csswg-drafts/${CSS_COMMIT}/${CSS_PATH}`);
const namedColors = cssNamedColors(cssSource);
const equivalentColors = cssEquivalentColors(cssSource);
writeJson("css-color-4.json", {
    source: {
        repository: "https://github.com/w3c/csswg-drafts",
        commit: CSS_COMMIT,
        path: CSS_PATH,
        license: "W3C-20150513",
    },
    namedColors,
    equivalentColors,
});

const parsing = await wptParsing();
const conversions = await wptConversions();
writeJson("wpt.json", {
    source: {
        repository: "https://github.com/web-platform-tests/wpt",
        commit: WPT_COMMIT,
        path: WPT_PATH,
        license: "BSD-3-Clause",
    },
    ...parsing,
    conversions,
});

const colorJsVersion = JSON.parse(readFileSync(new URL("node_modules/colorjs.io/package.json", import.meta.url))).version;
const colorJs = colorJsConversions();
const okhsx = colorJsOkhsx();
writeJson("colorjs.json", {
    source: {
        package: "colorjs.io",
        version: colorJsVersion,
        license: "MIT",
        seeds: { conversions: CONVERSION_SEED, okhsx: OKHSX_SEED },
    },
    conversions: colorJs,
    okhsx,
});

console.log(`CSS Color 4: ${namedColors.length} named colors, ${equivalentColors.length} equivalent-color examples`);
console.log(`WPT: ${parsing.valid.length} valid, ${parsing.validColorFunction.length} color() templates, ${parsing.invalid.length} invalid, ${conversions.length} conversions`);
console.log(`color.js ${colorJsVersion}: ${colorJs.length} conversions, ${okhsx.length} Okhsl/Okhsv colors`);
