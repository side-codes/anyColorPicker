---
title: "The color model"
description: "How anyColorPicker holds a color in Kotlin: a ColorValue in one color space, in CSS Color 4 units, with none components, your own spaces and Compose colors."
section: Colors
nav_order: 30
---

A `ColorValue` is a color in one space: that space's components, in its own units, and an alpha.
Each space builds one:

```kotlin
val teal = Okhsl(200.0, 0.8, 0.5)            // hue, saturation, lightness
val halfRed = Srgb(1.0, 0.0, 0.0, 0.5)       // alpha comes last
val vivid = Oklch(0.7, 0.3, 150.0)           // more chroma than sRGB can show

teal.space                  // Okhsl
teal[Okhsl.L]               // 0.5
teal.to(Oklch)              // the same color, in OkLCh
teal.with(Okhsl.L, 0.7)     // lighter, and still Okhsl
teal.withAlpha(0.5)
```

A value stays in its space until you convert it, so an OkLCh chroma of 0.3 or a Display P3 red
is kept as it is, and only drawing it maps it into what the screen can show. A component can be
`null`, CSS's `none`: a grey's hue is missing rather than 0, because it has none.
[Architecture](architecture.md) explains why a value is never converted back.

## Channels and units

The units are CSS's, so a number copied from a stylesheet or a design tool means the same here:

| Space                             | Channels                                    | Units                                          |
|-----------------------------------|---------------------------------------------|------------------------------------------------|
| `Srgb`, `SrgbLinear`, `DisplayP3` | `R`, `G`, `B`                               | 0–1, and past it for a color outside the gamut |
| `XyzD65`, `XyzD50`                | `X`, `Y`, `Z`                               | the white's `Y` is 1                           |
| `Lab`                             | `L`, `A`, `B`                               | L 0–100; a and b about ±125                    |
| `Lch`                             | `L`, `C`, `H`                               | L 0–100, C 0–150, H 0–360                      |
| `Oklab`                           | `L`, `A`, `B`                               | L 0–1; a and b about ±0.4                      |
| `Oklch`                           | `L`, `C`, `H`                               | L 0–1, C 0–0.4, H 0–360                        |
| `Hsl`, `Hsv`, `Hwb`               | `H`, `S`, `L`; `H`, `S`, `V`; `H`, `W`, `B` | H 0–360, the rest 0–100                        |
| `Okhsl`, `Okhsv`                  | `H`, `S`, `L`; `H`, `S`, `V`                | H 0–360, the rest 0–1                          |
| `Cmyk`                            | `C`, `M`, `Y`, `K`                          | 0–1                                            |

[Choosing a color space](color-spaces.md) says what each space is for.

## Defining your own color spaces

```kotlin
val brandHsl = ColorSpace.hsl("--brand-hsl", over = DisplayP3)

ColorPicker(state, space = brandHsl)   // a plane and H, S and L sliders, over Display P3
```

`ColorSpace.rgb`, `hsl`, `hsv`, `hwb` and `polar` build one. Its id is a CSS custom name, so
`toCssString` writes it as `color(--brand-hsl …)`, and `parseCss` reads that back when the space is
among those it is given.

## Compose colors

```kotlin
val color: Color = teal.toComposeColor()     // mapped into sRGB, as toGamut maps it
val value: ColorValue = color.toColorValue() // sRGB, or the Compose color space it is in
```

A Compose `Color` in Display P3 or another of Compose's RGB spaces arrives in that space, not
clipped into sRGB. [Gamut mapping](gamut-mapping.md) describes how a color outside sRGB is brought into it.
