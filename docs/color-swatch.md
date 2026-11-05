---
title: "Color swatch"
description: "ColorSwatch in anyColorPicker draws a Compose color over a transparency checkerboard, so a color's alpha reads correctly."
section: Components
nav_order: 110
---

`ColorSwatch` draws the color over a transparency checkerboard, so alpha reads correctly:

![A half-transparent red color swatch over a checkerboard](images/color-swatch.png)

```kotlin
ColorSwatch(
    color = state.color,
    modifier = Modifier.fillMaxWidth().height(48.dp),
    contentDescription = "Selected color",
)
```

It takes `colors` and a `shape`, as [Theming](theming.md) describes. Without Material, a clip,
`Modifier.checkerboard` and a background draw the same: see [Without Material](without-material.md).
