---
title: "Choosing a color space"
description: "Which color space to pick in with anyColorPicker, and what each is for: Okhsl, Okhsv, Oklab, OkLCh, Lab, LCH, HSL, HWB, HSV, CMYK, sRGB, Display P3 and XYZ."
section: Colors
nav_order: 40
---

- **Okhsl** is Björn Ottosson's perceptual replacement for HSL, and the one to reach for if you
  are choosing between the two. Lightness is perceived lightness, so a blue and a yellow at `0.5`
  look about equally light; in HSL they differ by more than half the scale. Saturation is measured
  against the sRGB gamut, so `1` is as colorful as sRGB goes at that hue and lightness — every
  coordinate is a real color, bar a sliver just past pure blue, and no part of a slider is dead
  travel.
- **Okhsv** has Okhsl's perceptual hue and gamut-relative saturation in the HSV arrangement
  artists expect: full saturation at full value is the most vivid form of a hue, and pulling value
  down darkens toward black. Prefer Okhsl when the middle of the lightness track should be a mid
  tone.
- **Oklab** is the perceptual space the two above are built on, and the one to interpolate and
  compare colors in: equal steps are close to equal perceived steps, and moving `L` does not drag
  the perceived hue with it. **OkLCh** is its cylindrical form, CSS's `oklch()`, for changing one
  of lightness, chroma and hue while holding the others. Neither is bounded by the display, so most
  of their range lies outside sRGB and is gamut-mapped into it to be drawn.
- **Lab** and **LCH** are CIELAB with a D50 white, which is what CSS `lab()`, Photoshop and
  Compose's `ColorSpaces.CieLab` all quote, so a value copied from any of them means here what it
  meant there. About an eighth of the a–b square is inside sRGB.
- **HSL** and **HWB** follow CSS formulas over sRGB; **HSV** is the conventional sRGB hexcone model.
- **CMYK** is the naive conversion, with no color profile. It round-trips on screen and is not
  what a press will print — real CMYK is device dependent, its gamut is not sRGB's, and crossing
  between them needs an ICC profile and a rendering intent. Treat it as a screen-space
  parameterisation rather than ink.
- **sRGB**, **linear sRGB**, **Display P3** and **XYZ** are there for interchange, and
  `ColorPicker(state, space = DisplayP3)` puts sliders on any of them.

Each space's channels and units are listed in [The color model](color-model.md), and the
[Gallery](gallery.md) shows a picker for each.
