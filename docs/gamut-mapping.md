---
title: "Gamut mapping"
description: "How anyColorPicker draws a color outside sRGB with CSS Color 4 gamut mapping, holding lightness and hue within a just-noticeable difference."
section: Colors
nav_order: 50
---

Drawing a color outside sRGB does not clamp each channel independently, which would shift
lightness and hue as a side effect. It runs CSS Color 4's
[binary search with local MINDE](https://www.w3.org/TR/css-color-4/#GMA-Binary-local-MINDE), one of
the three algorithms it allows: binary search down the chroma axis at constant lightness and
hue, comparing each candidate against its clipped form, and return the clipped form once the two are
within a just-noticeable difference. Chroma pays; lightness and hue move by less than that
difference.

```kotlin
val vivid = Oklch(0.7, 0.3, 150.0)

vivid.isInGamut(Srgb.gamut)                     // false
vivid.toGamut(Srgb.gamut)                       // less chroma, the same lightness and hue
vivid.toGamut(Srgb.gamut, GamutMapping.Clip)    // each channel clipped, when that is what you want
vivid.toComposeColor()                          // mapped as toGamut maps it
```

The search runs in Oklab whatever space the color came from, as CSS specifies, so what survives is
Oklab's lightness and hue, not CIELAB's. `GamutMapping.ChromaReduction()` solves for the gamut's
edge instead, and keeps lightness and hue to rounding. Okhsl and Okhsv rarely
need either: their saturation is measured against sRGB, so they are inside it by construction,
except in a sliver just past pure blue (264.05–264.21°), where they stray by under 0.001 of a
linear channel.

## Limits

Two limits of the arithmetic. Below an OkLCh lightness of 0.001, `ChromaReduction`'s default solver
finds the edge less precisely than the color itself, so lightness and hue come back changed, though
the color stays far darker than one 8-bit step; `ChromaReduction(EdgeSolver.Iterative)` keeps them.
And a component beyond about 1e102 overflows the conversion: it maps to a color of the gamut, but not
one related to it, and from about 1e150 to black.
