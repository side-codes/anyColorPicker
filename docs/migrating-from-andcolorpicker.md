---
title: "Migrating from andcolorpicker"
description: "Move from the discontinued View-based andcolorpicker 0.6.x Android library to anyColorPicker's Jetpack Compose components, concept by concept."
section: Guides
nav_order: 200
---

The View-based `codes.side:andcolorpicker` artifact (XML `HSLColorPickerSeekBar` and friends) is discontinued. This library is a full Compose Multiplatform rewrite published under new coordinates:

```diff
- implementation("codes.side:andcolorpicker:0.6.2")
+ implementation("codes.side:colorpicker-material3:2.0.0")
```

There is no 1:1 API mapping — migrate by concept:

| andcolorpicker (View-based)                                    | colorpicker-material3 (Compose)                                                                                                    |
|----------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|
| `HSLColorPickerSeekBar` (`hslMode` = hue/saturation/lightness) | `ChannelSlider(state, Hsl.H)`, `Hsl.S` or `Hsl.L`, or `HslColorPicker` for all three                                               |
| `RGBColorPickerSeekBar`                                        | `ChannelSlider(state, Srgb.R)`, `Srgb.G` or `Srgb.B`, or `RgbColorPicker`                                                          |
| `CMYKColorPickerSeekBar`                                       | `ChannelSlider(state, Cmyk.C)`, `Cmyk.M`, `Cmyk.Y` or `Cmyk.K`, or `CmykColorPicker`                                               |
| `LABColorPickerSeekBar`                                        | `ChannelSlider(state, Lab.L)`, `Lab.A` or `Lab.B`, or `LabColorPicker`                                                             |
| `HSLAlphaColorPickerSeekBar`                                   | `AlphaSlider`                                                                                                                      |
| `PickerGroup` + `registerPickers`                              | Pass one `ColorPickerState` to every component — they stay in sync automatically                                                   |
| `SwatchView`                                                   | `ColorSwatch`                                                                                                                      |
| `OnColorPickListener` / `addListener`                          | Read `state.value` — it is Compose snapshot state, so composition recomposes automatically; use `snapshotFlow` outside composition |
| `IntegerHSLColor` and friends                                  | `ColorValue`, built by its space: `Hsl(200.0, 80.0, 50.0)`                                                                         |
| `hslColoringMode` = `pure` / `output`                          | `ColoringMode.Independent` / `ColoringMode.Contextual`                                                                             |
