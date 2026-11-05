---
title: "CSS color strings and hex"
description: "Write and parse CSS Color 4 strings and hex colors in Kotlin with anyColorPicker, and choose which end of an eight-digit hex holds the alpha."
section: Colors
nav_order: 60
---

## CSS color strings

```kotlin
Oklch(0.7, 0.15, 140.0).toCssString()     // "oklch(0.7 0.15 140)"
Hsl(120.0, 50.0, 25.0).toCssString()      // "hsl(120 50% 25%)"
Okhsl(120.0, 0.5, 0.25).toCssString()     // "color(--okhsl 120 0.5 0.25)"
Hsl(120.0, 50.0, null).toCssString()      // "hsl(120 50% none)"

ColorValue.parseCss("oklch(70% 0.15 140 / 50%)")   // an OkLCh value at half alpha
ColorValue.parseCssOrNull("not a color")           // null for invalid color text
```

A value is written in its own space and never mapped into a gamut, so a Display P3 red stays
`color(display-p3 1 0 0)`. The library's HSV, Okhsl, Okhsv and CMYK and an
[app's spaces](color-model.md#defining-your-own-color-spaces) are written as `color(--name …)`, which `parseCss` reads back.

## Hex colors

Hex is always sRGB, eight bits a channel, mapped into the gamut first:

```kotlin
val blue = Srgb(0.2, 0.5, 0.8)

blue.toHexString(HexAlpha.First)    // "#FF3380CC", as android.graphics.Color writes it
blue.toHexString(HexAlpha.Last)     // "#3380CCFF", as CSS writes it
blue.toHexString(HexAlpha.None)     // "#3380CC"

ColorValue.parseHex("#3380CC", HexAlpha.None)          // throws on invalid input
ColorValue.parseHexOrNull("#ABC", HexAlpha.None)       // shorthand, expands to #AABBCC
```

## Hex alpha: first or last

Four and eight hex digits carry an alpha channel and cannot tell you at which end —
`#FF000080` is a half-transparent red to a stylesheet and an opaque navy to
`android.graphics.Color`. Neither end has a default, so a round trip names the same ordering
twice and the pair reads off one screen:

```kotlin
val stored = blue.toHexString(HexAlpha.First)
ColorValue.parseHexOrNull(stored, HexAlpha.First)          // the color that went in

ColorValue.parseHexOrNull("#FF000080", HexAlpha.First)     // opaque navy, as Android reads it
ColorValue.parseHexOrNull("#FF000080", HexAlpha.Last)      // half-transparent red, as CSS reads it
ColorValue.parseHexOrNull("#FF000080", HexAlpha.None)      // null: the opaque forms only
```

Three and six digits carry no alpha, so they mean the same thing whichever you name.
