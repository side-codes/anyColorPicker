---
title: "State management"
description: "ColorPickerState holds the color a Compose picker edits, remembers a grey's hue, reads any channel in any space, and survives process death."
section: State and architecture
nav_order: 160
---

`ColorPickerState` holds the color a picker edits, as one `ColorValue`, and the hues it
remembers beside it.

```kotlin
val state = rememberColorPickerState(Okhsl(250.0, 0.8, 0.6))

state.value                       // the ColorValue, in the space it was last written in
state.color                       // as a Compose Color, mapped into sRGB
state[Hsl.H]                      // one channel, converted; null for a grey's hue
state.displayValue(Hsl.H)         // what a slider shows: for a grey, the hue last chosen
state.hsl.l                       // a typed view; there is one for each of the fifteen spaces

state.value = Oklch(0.7, 0.15, 140.0)
state[Hsl.L] = 40.0               // leaves the color in HSL
state.value = state.value.withAlpha(0.5)

state.isInteracting               // true while a slider or plane is being dragged
```

## A grey keeps its hue

A grey has no hue, so a hue slider has nothing to show for one. The state remembers the last
hue chosen and shows that, and an edit that makes a grey colorful writes it back, so a color the
user darkened or desaturated comes back in the hue they had rather than red. A grey arriving
without a hue — from hex, a Compose `Color` or an sRGB value — leaves what is remembered alone.
A hue chosen in one space is carried into the others, so an Okhsl slider shows the hue last
picked on an HSL one; that is this library's own rule, as CSS carries no hue across spaces
([csswg-drafts#8484](https://github.com/w3c/csswg-drafts/issues/8484)).

Writing a channel that is out of its limit, or not a number, throws.

## Holding and saving the state

`ColorPickerState` has a public constructor, so it can also be created and held outside of
composition (e.g. in a ViewModel). `rememberSaveableColorPickerState` keeps the value, its space
and the remembered hues across configuration changes and process death, on platforms that
restore saved state. A value in an app's own space restores when that space is among the
`knownSpaces` it is given.

A picker can also sit over a value you hold yourself instead of a state: see
[Color pickers](pickers.md#over-a-value-you-hold).
