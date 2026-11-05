---
title: "Gallery of color pickers"
description: "Screenshots of every anyColorPicker color picker: RGB, HSL, HSV, HWB, Lab, LCH, Oklab, OkLCh, Okhsl, Okhsv and CMYK, in both coloring modes."
icon: material/view-gallery-outline
---

Every picker takes a `ColoringMode`. `Independent` shows each channel's full range;
`Contextual` previews the resulting color at every slider position. A picker over a space
with a hue defaults to `Independent`; the RGB, Lab, Oklab and CMYK pickers default to
`Contextual`.

| Space                           | Independent                                                               | Contextual                                                              |
|---------------------------------|---------------------------------------------------------------------------|-------------------------------------------------------------------------|
| **RGB**<br>`RgbColorPicker`     | ![RGB color picker, independent coloring](images/rgb-independent.png)     | ![RGB color picker, contextual coloring](images/rgb-contextual.png)     |
| **HSL**<br>`HslColorPicker`     | ![HSL color picker, independent coloring](images/hsl-independent.png)     | ![HSL color picker, contextual coloring](images/hsl-contextual.png)     |
| **HSV**<br>`HsvColorPicker`     | ![HSV color picker, independent coloring](images/hsv-independent.png)     | ![HSV color picker, contextual coloring](images/hsv-contextual.png)     |
| **HWB**<br>`HwbColorPicker`     | ![HWB color picker, independent coloring](images/hwb-independent.png)     | ![HWB color picker, contextual coloring](images/hwb-contextual.png)     |
| **Lab**<br>`LabColorPicker`     | ![Lab color picker, independent coloring](images/lab-independent.png)     | ![Lab color picker, contextual coloring](images/lab-contextual.png)     |
| **LCH**<br>`LchColorPicker`     | ![LCH color picker, independent coloring](images/lch-independent.png)     | ![LCH color picker, contextual coloring](images/lch-contextual.png)     |
| **Oklab**<br>`OklabColorPicker` | ![Oklab color picker, independent coloring](images/oklab-independent.png) | ![Oklab color picker, contextual coloring](images/oklab-contextual.png) |
| **OkLCh**<br>`OklchColorPicker` | ![OkLCh color picker, independent coloring](images/oklch-independent.png) | ![OkLCh color picker, contextual coloring](images/oklch-contextual.png) |
| **Okhsl**<br>`OkhslColorPicker` | ![Okhsl color picker, independent coloring](images/okhsl-independent.png) | ![Okhsl color picker, contextual coloring](images/okhsl-contextual.png) |
| **Okhsv**<br>`OkhsvColorPicker` | ![Okhsv color picker, independent coloring](images/okhsv-independent.png) | ![Okhsv color picker, contextual coloring](images/okhsv-contextual.png) |
| **CMYK**<br>`CmykColorPicker`   | ![CMYK color picker, independent coloring](images/cmyk-independent.png)   | ![CMYK color picker, contextual coloring](images/cmyk-contextual.png)   |

[Color swatch](color-swatch.md), [Color planes](planes.md) and [Color picker dialog](dialog.md) show the other
components.

These images are copies of the Compose Preview Screenshot Testing references, rendered from the
library's own components. CI checks the references, not these copies:
`./gradlew :screenshot-tests:updateDebugScreenshotTest` rewrites the references and copies them to `docs/images`.
