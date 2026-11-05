---
title: "Architecture: a value stays in its space"
description: "Why anyColorPicker keeps a color in the space it was written in and converts forward only, as CSS Color 4, color.js and Sass do, so edits never lose precision."
section: State and architecture
nav_order: 170
---

Color space conversions lose precision: floats round, and a conversion cannot invent what a
color does not carry — every hue of a grey is the same sRGB color. Industry-standard tools (CSS
Color Level 4, color.js, Sass) solve this the same way we do:

**Store colors in their authored color space. Convert forward only. Never convert back.**

A `ColorValue` is always in one space, and the state holds whichever value was last written or
edited. A slider edits in its own channel's space: the color is converted there once, the channel
is set, and the result stays there. Reading another space converts forward, once, from that
value; nothing is re-derived from a conversion.

```
User drags the OkLCh chroma slider
  -> the value is converted into OkLCh once and its chroma set (the value is now OkLCh)
  -> the Okhsl sliders read it: one conversion, OkLCh -> Okhsl
  -> the OkLCh sliders read it: no conversion at all
```

This means:

- Editing in a space and reading that space back gives **exactly the value set**
- Cross-space reads involve a single forward conversion, never a round-trip
- No precision loss accumulates over time, regardless of how many edits are made
- A value outside sRGB and a `none` component are kept until something writes over them

For more details, see:

- [CSS Color Module Level 4](https://www.w3.org/TR/css-color-4/) -- the W3C spec mandates the same approach
- [Sass Color Spaces](https://sass-lang.com/documentation/values/colors/) -- stores colors in their original space
