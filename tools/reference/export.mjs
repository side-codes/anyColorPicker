// Exports the color module's reference test data as JSON: CSS Color 4's named-color table, its
// worked examples of equivalent colors and its worked conversions from its own Bikeshed source,
// web-platform-tests' CSS color parsing lists and the conversions in them, and seeded colors
// converted by color.js. Every source is pinned, so a rerun writes the same files. The tests decode
// them with kotlinx.serialization; nothing here writes Kotlin.
//
//   cd tools/reference
//   npm ci
//   node export.mjs
import Color from "colorjs.io";
import { JSDOM } from "jsdom";
import { mkdirSync, readFileSync, writeFileSync } from "node:fs";
import vm from "node:vm";

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

// ---- CSS Color 4 ----
//
// The Bikeshed source is HTML with shorthands in its text, so it is parsed as HTML and queried.
// Bikeshed writes an inline CSS value as ''value'' and a definition link as [=term=]; both stay in
// the text, which is where the colors and the spec's own words about them are read from.

const quotedCss = text => [...text.matchAll(/''([^']+)''/g)].map(m => m[1]);

// The named-color table: a row per color, its name in the header cell, then its hex and its decimal,
// which must agree.
function cssNamedColors(document) {
    return [...document.querySelectorAll("tr > th[scope=row] > dfn")].map(dfn => {
        const name = dfn.textContent.trim();
        const hex = dfn.parentElement.nextElementSibling?.textContent.trim();
        const rgb = dfn.parentElement.nextElementSibling?.nextElementSibling?.textContent.trim().split(/\s+/).map(Number);
        if (!/^#[0-9a-f]{6}$/.test(hex) || rgb?.length !== 3) throw new Error(`${name}: the row is not a name, a hex and a decimal`);
        if (`#${rgb.map(v => v.toString(16).padStart(2, "0")).join("")}` !== hex) throw new Error(`${name}: ${hex} is not ${rgb.join(" ")}`);
        return { name, rgb };
    });
}

// §12, Comparing <color> Values: each worked example compares the first two colors it quotes, and
// says the two "are not [=equivalent colors=]" when they are not; its notes add positive pairs.
function cssEquivalentColors(document) {
    const heading = document.getElementById("comparing-color-values");
    const next = [...document.querySelectorAll("h2")].find(h => heading.compareDocumentPosition(h) & h.DOCUMENT_POSITION_FOLLOWING);
    const section = document.createRange();
    section.setStartAfter(heading);
    section.setEndBefore(next);
    const pairs = [];
    for (const example of document.querySelectorAll('div.example[id^="ex-equivalent-"]')) {
        if (!section.intersectsNode(example)) continue;
        const text = example.textContent;
        const colors = quotedCss(text);
        if (colors.length < 2) throw new Error(`${example.id} quotes fewer than two colors`);
        const equivalent = !/are\s+not\s+\[=equivalent colors=\]/.test(text);
        pairs.push({ example: example.id, first: colors[0], second: colors[1], equivalent });
    }
    for (const [, first, second] of section.toString().matchAll(/''([^']+)''\s+and\s+''([^']+)''—are \[=equivalent colors=\]/g)) {
        pairs.push({ example: "note", first, second, equivalent: true });
    }
    return pairs;
}

// Worked conversions: a color and the same color printed in another space, both in one example
// block. css-worked-examples.json names each block, the two texts as printed there (`printed` where
// the source states the second in prose), and a tolerance per component: half a unit of the last
// digit printed, or 1e-9 where the value is exact. The second color is read by color.js, which does
// not clamp, so lab(100.1154% …) keeps its lightness past 100.
//
// Left out are examples written before the draft's current matrices: lch(51.2345% 21.2 130) as srgb
// and display-p3, #7654CD as xyz-d50 and xyz-d65, lch(85.9017% 166.116 138.207) as display-p3,
// rgb(76% 62% 3%) as lab and lch, and color(display-p3 0.84 0.19 0.72) as lab and lch, which color.js
// converts as the library does, up to 1.4e-2 from the printed values.
function cssWorkedExamples(document) {
    const { examples } = JSON.parse(readFileSync(new URL("css-worked-examples.json", import.meta.url), "utf8"));
    return examples.map(({ example, color, equals, printed = [color, equals], tolerance }) => {
        const block = document.getElementById(example);
        if (!block?.matches("div.example")) throw new Error(`${example}: no such example`);
        for (const text of printed) {
            if (!block.textContent.includes(text)) throw new Error(`${example}: "${text}" is not printed there`);
        }
        const parsed = new Color(equals);
        const space = Object.keys(COLORJS).find(id => COLORJS[id] === parsed.space.id);
        if (space === undefined) throw new Error(`${example}: ${equals} is in ${parsed.space.id}, which the library does not have`);
        if (tolerance.length !== parsed.coords.length) throw new Error(`${example}: ${tolerance.length} tolerances for ${parsed.coords.length} components`);
        return { example, color, equals: { space, components: components(parsed.coords) }, tolerance };
    });
}

// ---- web-platform-tests ----
//
// Each test file is loaded as a page and its inline scripts are run against a harness that records
// what it is asked to test, so the tests' own lists and loops expand themselves. testharness.js and
// the support scripts are not loaded; the four functions the files call stand in for them.

// Every value a file tests for the `color` property, as { kind, value, expected }: kind is "valid",
// "invalid" or "computed", and expected is the serialization the test wants, if it names one.
async function wptTests(name) {
    const dom = new JSDOM(await fetchText(`${WPT}/${name}`), { runScripts: "outside-only" });
    const tests = [];
    const record = kind => (property, value, expected) => {
        if (property === "color") tests.push({ kind, value, expected });
    };
    Object.assign(dom.window, {
        test_valid_value: record("valid"),
        test_invalid_value: record("invalid"),
        fuzzy_test_computed_color: (value, expected) => tests.push({ kind: "computed", value, expected }),
        // Tests of currentcolor and of other properties, which only convert where the color property does.
        fuzzy_test_computed_color_using_currentcolor: () => {},
        fuzzy_test_computed_color_property: () => {},
    });
    const context = dom.getInternalVMContext();
    for (const script of dom.window.document.querySelectorAll("script:not([src])")) {
        new vm.Script(script.textContent, { filename: `${WPT_PATH}/${name}` }).runInContext(context);
    }
    return tests;
}

// The values the named files test with the harness function [kind].
async function wptValues(names, kind) {
    const tests = [];
    for (const name of names) tests.push(...(await wptTests(name)).filter(test => test.kind === kind));
    if (tests.some(test => typeof test.expected !== "string" && test.expected !== undefined)) {
        throw new Error(`${names}: a test accepts more than one serialization`);
    }
    return tests;
}

// Cases that need calc(), sign(), currentcolor or light-dark() are left out: they need a cascade or
// math functions the parser does not have.
const skipped = value => ["calc(", "sign(", "currentcolor", "light-dark"].some(u => value.toLowerCase().includes(u));

// A color and how a browser serializes it; a test that names no serialization expects the color back.
const serialized = ({ value, expected }) => ({ color: value, serialized: expected ?? value });

async function wptParsing() {
    const valid = (await wptValues(["color-valid-rgb.html", "color-valid-hsl.html", "color-valid-hwb.html", "color-valid-lab.html", "color-valid.html"], "valid"))
        .map(serialized)
        .filter(test => !skipped(test.color) && !skipped(test.serialized));
    const validColorFunction = (await wptValues(["color-valid-color-function.html"], "valid"))
        .map(serialized)
        .filter(test => !skipped(test.color) && !skipped(test.serialized));
    const invalid = (await wptValues([
        "color-invalid.html", "color-invalid-rgb.html", "color-invalid-hsl.html", "color-invalid-hwb.html",
        "color-invalid-hex-color.html", "color-invalid-named-color.html", "color-invalid-color-function.html", "color-invalid-lab.html",
    ], "invalid")).map(test => test.value).filter(value => !skipped(value));
    return { valid, validColorFunction, invalid };
}

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
    const tests = await wptValues(["color-computed-relative-color.html", "color-computed-powerless.html", "color-computed-none.html"], "computed");
    for (const { value: source, expected: serialized } of tests) {
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

const colorJsVersion = JSON.parse(readFileSync(new URL("node_modules/colorjs.io/package.json", import.meta.url))).version;

const css = new JSDOM(await fetchText(`https://raw.githubusercontent.com/w3c/csswg-drafts/${CSS_COMMIT}/${CSS_PATH}`)).window.document;
const namedColors = cssNamedColors(css);
const equivalentColors = cssEquivalentColors(css);
const workedExamples = cssWorkedExamples(css);
writeJson("css-color-4.json", {
    source: {
        repository: "https://github.com/w3c/csswg-drafts",
        commit: CSS_COMMIT,
        path: CSS_PATH,
        license: "W3C-20150513",
        workedExamples: {
            chosenIn: "tools/reference/css-worked-examples.json",
            readBy: { package: "colorjs.io", version: colorJsVersion },
        },
    },
    namedColors,
    equivalentColors,
    workedExamples,
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

console.log(`CSS Color 4: ${namedColors.length} named colors, ${equivalentColors.length} equivalent-color examples, ${workedExamples.length} worked conversions`);
console.log(`WPT: ${parsing.valid.length} valid, ${parsing.validColorFunction.length} color() cases, ${parsing.invalid.length} invalid, ${conversions.length} conversions`);
console.log(`color.js ${colorJsVersion}: ${colorJs.length} conversions, ${okhsx.length} Okhsl/Okhsv colors`);
