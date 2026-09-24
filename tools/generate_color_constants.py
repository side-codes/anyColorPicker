#!/usr/bin/env python3
"""Writes the color module's conversion constants from the definitions CSS Color 4 gives.

RGB matrices are derived from the primaries' chromaticities in exact fractions, as CSS derives
the rationals it publishes, and rounded once. Bradford and Oklab matrices are CSS's published
64-bit values, taken as they are. Each literal is the correctly rounded double of its fraction,
printed as the shortest decimal that parses back to it, so every platform reads the same bits.

Usage:
  python tools/generate_color_constants.py          rewrite the Kotlin file
  python tools/generate_color_constants.py --check  exit 1 if the committed file differs
"""

import sys
from fractions import Fraction as F
from pathlib import Path

OUTPUT = Path(__file__).resolve().parent.parent / "color/src/commonMain/kotlin/codes/side/color/internal/GeneratedConstants.kt"


def white_xyz(x, y):
    x, y = F(x), F(y)
    return [x / y, F(1), (1 - x - y) / y]


def inverse(m):
    a, b, c = m[0]
    d, e, f = m[1]
    g, h, i = m[2]
    det = a * (e * i - f * h) - b * (d * i - f * g) + c * (d * h - e * g)
    return [
        [(e * i - f * h) / det, (c * h - b * i) / det, (b * f - c * e) / det],
        [(f * g - d * i) / det, (a * i - c * g) / det, (c * d - a * f) / det],
        [(d * h - e * g) / det, (b * g - a * h) / det, (a * e - b * d) / det],
    ]


def multiply(m, n):
    return [[sum(m[r][k] * n[k][c] for k in range(3)) for c in range(3)] for r in range(3)]


def rgb_to_xyz(primaries, white):
    columns = [white_xyz(x, y) for x, y in primaries]
    p = [[columns[c][r] for c in range(3)] for r in range(3)]
    scale = [sum(row[k] * w for k, w in enumerate(white)) for row in inverse(p)]
    return [[p[r][c] * scale[c] for c in range(3)] for r in range(3)]


def decimals(rows):
    return [[F(v) for v in row] for row in rows]


D65 = white_xyz("0.3127", "0.3290")
D50 = white_xyz("0.3457", "0.3585")

SRGB_PRIMARIES = [("0.640", "0.330"), ("0.300", "0.600"), ("0.150", "0.060")]
DISPLAY_P3_PRIMARIES = [("0.680", "0.320"), ("0.265", "0.690"), ("0.150", "0.060")]

SRGB_LINEAR_TO_XYZ_D65 = rgb_to_xyz(SRGB_PRIMARIES, D65)
DISPLAY_P3_LINEAR_TO_XYZ_D65 = rgb_to_xyz(DISPLAY_P3_PRIMARIES, D65)

# CSS Color 4, Sample code for color conversions: D65_to_D50 and D50_to_D65.
XYZ_D65_TO_D50 = decimals([
    ["1.0479297925449969", "0.022946870601609652", "-0.05019226628920524"],
    ["0.02962780877005599", "0.9904344267538799", "-0.017073799063418826"],
    ["-0.009243040646204504", "0.015055191490298152", "0.7518742814281371"],
])
XYZ_D50_TO_D65 = decimals([
    ["0.955473421488075", "-0.02309845494876471", "0.06325924320057072"],
    ["-0.0283697093338637", "1.0099953980813041", "0.021041441191917323"],
    ["0.012314014864481998", "-0.020507649298898964", "1.330365926242124"],
])

# CSS Color 4, Sample code for color conversions: XYZ_to_OKLab and OKLab_to_XYZ.
XYZ_D65_TO_LMS = decimals([
    ["0.8190224379967030", "0.3619062600528904", "-0.1288737815209879"],
    ["0.0329836539323885", "0.9292868615863434", "0.0361446663506424"],
    ["0.0481771893596242", "0.2642395317527308", "0.6335478284694309"],
])
LMS_TO_XYZ_D65 = decimals([
    ["1.2268798758459243", "-0.5578149944602171", "0.2813910456659647"],
    ["-0.0405757452148008", "1.1122868032803170", "-0.0717110580655164"],
    ["-0.0763729366746601", "-0.4214933324022432", "1.5869240198367816"],
])
LMS_TO_OKLAB = decimals([
    ["0.2104542683093140", "0.7936177747023054", "-0.0040720430116193"],
    ["1.9779985324311684", "-2.4285922420485799", "0.4505937096174110"],
    ["0.0259040424655478", "0.7827717124575296", "-0.8086757549230774"],
])
OKLAB_TO_LMS = decimals([
    ["1.0000000000000000", "0.3963377773761749", "0.2158037573099136"],
    ["1.0000000000000000", "-0.1055613458156586", "-0.0638541728258133"],
    ["1.0000000000000000", "-0.0894841775298119", "-1.2914855480194092"],
])

MATRICES = [
    ("SRGB_LINEAR_TO_XYZ_D65", "Linear sRGB to XYZ-D65, derived from the sRGB primaries.", SRGB_LINEAR_TO_XYZ_D65),
    ("XYZ_D65_TO_SRGB_LINEAR", "The exact inverse of [SRGB_LINEAR_TO_XYZ_D65].", inverse(SRGB_LINEAR_TO_XYZ_D65)),
    ("DISPLAY_P3_LINEAR_TO_XYZ_D65", "Linear Display P3 to XYZ-D65, derived from the P3 primaries.", DISPLAY_P3_LINEAR_TO_XYZ_D65),
    ("XYZ_D65_TO_DISPLAY_P3_LINEAR", "The exact inverse of [DISPLAY_P3_LINEAR_TO_XYZ_D65].", inverse(DISPLAY_P3_LINEAR_TO_XYZ_D65)),
    ("XYZ_D65_TO_D50", "Bradford adaptation from D65 to D50, CSS's published matrix.", XYZ_D65_TO_D50),
    ("XYZ_D50_TO_D65", "Bradford adaptation from D50 to D65, CSS's published matrix.", XYZ_D50_TO_D65),
    ("XYZ_D65_TO_LMS", "XYZ-D65 to Oklab's LMS, CSS's published matrix.", XYZ_D65_TO_LMS),
    ("LMS_TO_XYZ_D65", "Oklab's LMS to XYZ-D65, CSS's published matrix.", LMS_TO_XYZ_D65),
    ("LMS_TO_OKLAB", "Cube-rooted LMS to Oklab, CSS's published matrix.", LMS_TO_OKLAB),
    ("OKLAB_TO_LMS", "Oklab to cube-rooted LMS, CSS's published matrix.", OKLAB_TO_LMS),
    ("LMS_TO_SRGB_LINEAR", "Oklab's LMS straight to linear sRGB: [XYZ_D65_TO_SRGB_LINEAR] × [LMS_TO_XYZ_D65], multiplied exactly. sRGB's gamut, and Okhsl and Okhsv with it, is found through it.", multiply(inverse(SRGB_LINEAR_TO_XYZ_D65), LMS_TO_XYZ_D65)),
    ("SRGB_LINEAR_TO_LMS", "Linear sRGB straight to Oklab's LMS: [XYZ_D65_TO_LMS] × [SRGB_LINEAR_TO_XYZ_D65], multiplied exactly.", multiply(XYZ_D65_TO_LMS, SRGB_LINEAR_TO_XYZ_D65)),
    ("LMS_TO_DISPLAY_P3_LINEAR", "Oklab's LMS straight to linear Display P3: [XYZ_D65_TO_DISPLAY_P3_LINEAR] × [LMS_TO_XYZ_D65], multiplied exactly.", multiply(inverse(DISPLAY_P3_LINEAR_TO_XYZ_D65), LMS_TO_XYZ_D65)),
    ("DISPLAY_P3_LINEAR_TO_LMS", "Linear Display P3 straight to Oklab's LMS: [XYZ_D65_TO_LMS] × [DISPLAY_P3_LINEAR_TO_XYZ_D65], multiplied exactly.", multiply(XYZ_D65_TO_LMS, DISPLAY_P3_LINEAR_TO_XYZ_D65)),
]

VECTORS = [
    ("D65_XYZ", "The D65 white as XYZ with Y = 1, from its chromaticity (0.3127, 0.3290).", D65),
    ("D50_XYZ", "The D50 white as XYZ with Y = 1, from its chromaticity (0.3457, 0.3585).", D50),
]


def literal(value):
    return repr(float(value))


def render():
    lines = [
        "// Generated by tools/generate_color_constants.py. Do not edit; change the script and rerun it.",
        "package codes.side.color.internal",
        "",
    ]
    for name, doc, values in VECTORS:
        lines += [f"/** {doc} */", f"internal val {name}: DoubleArray = doubleArrayOf(",
                  "    " + ", ".join(literal(v) for v in values) + ",", ")", ""]
    for name, doc, rows in MATRICES:
        lines += [f"/** {doc} Row-major. */", f"internal val {name}: DoubleArray = doubleArrayOf("]
        lines += ["    " + ", ".join(literal(v) for v in row) + "," for row in rows]
        lines += [")", ""]
    return "\n".join(lines).rstrip("\n") + "\n"


def main():
    text = render()
    if "--check" in sys.argv[1:]:
        current = OUTPUT.read_text(encoding="utf-8") if OUTPUT.exists() else ""
        if current != text:
            print(f"{OUTPUT} is out of date; run python tools/generate_color_constants.py", file=sys.stderr)
            sys.exit(1)
        print(f"{OUTPUT.name} is up to date.")
        return
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(text, encoding="utf-8", newline="\n")
    print(f"Wrote {OUTPUT}")


if __name__ == "__main__":
    main()
