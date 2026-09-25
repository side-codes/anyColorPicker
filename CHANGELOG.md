# Changelog

## 2.0.0 (unreleased)

2.0 builds the pickers on a color model of its own, following CSS Color 4, and removes 1.x's color classes, sliders and planes. [Migrating from 1.x](README.md#-migrating-from-1x) maps each removed declaration to its replacement.

### Breaking changes

- **A color is a `ColorValue`.** The `model`, `conversion` and `util` packages are gone: `HslColor`, `RgbColor`, `CmykColor`, `LabColor`, `OklabColor`, `OklchColor`, `OkhslColor`, `OkhsvColor`, `PickerColor`, their conversions and hex functions, and `randomHslColor`. A `ColorValue` is a color in one of fifteen spaces, in CSS's units, so HSL's saturation and lightness run 0–100 rather than 0–1.
- **`ColorPickerState` holds a `ColorValue`.** It is built from a `ColorValue` or a Compose `Color`, with no default. `state.value`, `state[channel]` and fifteen typed views (`state.hsl`, `state.okLch`, …) replace the eight typed getters, `pickerColor` and `argbInt`, and `state[channel] = x` and `state.set(view)` replace the thirty `update…` functions. Writing NaN or a value outside a channel's limit throws where 1.x ignored or clamped it.
- **The 21 channel sliders and the three planes are gone.** `ChannelSlider(state, channel)` and `ChannelPlane(state, x, y)` take any channel of any space.
- **The pickers' value forms are fully controlled.** They take a `ColorValue` or a Compose `Color` in place of 1.x's color classes. A change reaches the callback in the same event, and the picker draws only what the caller passes back, where 1.x applied the caller's value when the gesture ended.
- **A picker's slots are `plane`, `channelSlider` and `alphaSlider`,** in place of one slot per slider.
- **`ColorPickerDialog` takes a `ColorValue` or a Compose `Color`, and a `space`,** Okhsl by default, where 1.x's was fixed to HSL.
- **State saved by 1.x is not restored.** It starts again from its initial value.

### Added

- **`codes.side:color`,** the color model without Compose: `ColorValue`; sRGB, linear sRGB, Display P3, XYZ D65 and D50, Lab, LCH, Oklab, OkLCh, HSL, HWB, HSV, Okhsl, Okhsv and CMYK; spaces an app defines; conversion between them; CSS Color 4 gamut mapping; CSS color strings and hex, both ways.
- **`codes.side:color-compose`,** `ColorValue.toComposeColor()` and `Color.toColorValue()`. A Compose color in Display P3 or another of Compose's RGB spaces keeps its space.
- **`ColorPicker(state, space)`,** a picker for any space: a plane when the space has one hue and two other channels, a slider for each channel, and alpha.
- **Five more named pickers,** `HsvColorPicker`, `HwbColorPicker`, `LchColorPicker`, `OklabColorPicker` and `OkLchColorPicker`, beside the six 1.x had. Each comes over a `ColorPickerState`, a `ColorValue` and a Compose `Color`.
- **`ChannelSlider` and `ChannelPlane`.** Arrow keys and screen readers step by each channel's own unit: a degree on a hue, 1/255 on an RGB channel, 0.001 on OkLCh chroma. A value past the end of a track, such as OkLCh chroma 0.5, pins the thumb while the label keeps its true number.
- **`AlphaSlider` takes `onValueChangeFinished`.**
- **`ColorPickerState.Saver(knownSpaces)`,** for saving a state wherever Compose takes a `Saver`. It and `rememberSaveableColorPickerState(…, knownSpaces)` restore a value in a space the app defines.
- **`ColorPickerState.displayValue(channel)`,** what a slider on that channel shows: a grey's remembered hue, and 0 for any other missing component.

## 1.2.1

### Fixed

- **A grey reports the last hue chosen, whichever space it was chosen in.** A neutral color has no hue to convert, so `ColorPickerState` hands back the last one chosen, but only a write in the same family counted. HSL at 200° dragged to white read hue 0 in Okhsl, and HSL at 200° followed by red from an RGB field still read 200 once the color went grey. Every write records the hue for both families.
- **`rememberSaveableColorPickerState` keeps the remembered hues.** A grey remembering hue 200 came back from a configuration change or process death reading 0. State saved by 1.2.0 still restores.
- **A wide-gamut Compose `Color` is gamut-mapped, not clipped.** `Color.toRgbColor()`, and every `Color.to*Color()` built on it, clipped each channel into sRGB, so Display P3 red arrived as `#FF0000`, 0.28° of Oklab hue away. It takes the CSS Color 4 mapping LAB, Oklab and OkLCh already used and arrives as `#FF0B0B`, hue intact. sRGB colors read exactly as before.
- **The Kotlin default hierarchy template applies.** `iosMain`, `appleMain` and `nativeMain` exist again, and the published metadata carries them. No API changes.

### Dependencies

Built with Kotlin 2.4.20 and Compose Multiplatform 1.12.1, up from 2.4.10 and 1.12.0; both reach consumers through the published dependencies.

### Compatibility

1.2.0 is not binary-compatible with 1.1.x. Thirty declarations changed their JVM signature: the hex functions, the `ColorPickerColors` and `ColorPickerShapes` constructors and `copy`, `ColorPickerDefaults.colors()` and `shapes()`, `ColorSlider`, `ColorPickerDialog`, and every 1.1.1 slider and picker. A library compiled against 1.1.x has to be rebuilt against 1.2.x; an app compiling its own code needs only the source changes listed under 1.2.0.

From 1.2.1 on, CI fails a release that removes anything an earlier release published, unless it is a new major version.

## 1.2.0

### Breaking changes

1.2.0 is also binary-incompatible with 1.1.x; see [Compatibility under 1.2.1](#compatibility).

**Hex strings name their alpha ordering.** `#FF000080` is a half-transparent red to a
stylesheet and an opaque navy to `android.graphics.Color`, and nothing in the string says
which. All four functions take a required `HexAlpha` instead of `includeAlpha: Boolean`:

```kotlin
// 1.1.1
color.toHexString()                        // "#FF3380CC"
color.toHexString(includeAlpha = false)    // "#3380CC"
"#FF3380CC".toRgbColorOrNull()

// 1.2.0
color.toHexString(HexAlpha.First)          // "#FF3380CC", as android.graphics.Color writes it
color.toHexString(HexAlpha.Last)           // "#3380CCFF", as CSS writes it
color.toHexString(HexAlpha.None)           // "#3380CC"
"#FF3380CC".toRgbColorOrNull(HexAlpha.First)
```

`HexAlpha.First` reproduces 1.1.1's behaviour at both ends. `HexAlpha.None` parses the three-
and six-digit forms and returns `null` for the rest, for input you do not control.

**`LabColor` uses the D50 white point.** It was D65 with a D65-referenced matrix, which is not
what an ICC profile, Photoshop or CSS `lab()` means by those numbers. Every stored `LabColor`
now names a slightly different colour — median ΔE76 2.7 over a 17³ sweep of sRGB and 7.1 at
the 95th percentile, against the 2.3 where a difference starts to show at all. sRGB red read
53.24, 80.09, 67.20 where CSS, Photoshop and `ColorSpaces.CieLab` all say 54.29, 80.81, 69.89. Colours outside sRGB are gamut-mapped by the CSS
Color 4 algorithm rather than clipped per channel, which holds lightness and hue and gives up
chroma.

**`ColorPickerColors` and `ColorPickerShapes` gained properties.** `disabledAlpha` and
`disabledSaturation` on the first, `planeShape` on the second. Positional construction and
`copy` with positional arguments no longer compile; build them through
`ColorPickerDefaults.colors()` and `ColorPickerDefaults.shapes()`, or name the arguments.

**`fromInt` wraps hue instead of clamping it.** An angle has no ends, so `370` is `10` and
`-10` is `350` on `HslColor`, `OkhslColor`, `OkhsvColor` and `OklchColor`.

### Added

- **The Oklab family** — `OklabColor`, `OklchColor`, `OkhslColor` and `OkhsvColor`, with
  conversions, channel sliders, and `OkhslColorPicker` and `OkhsvColorPicker`. Okhsl and Okhsv
  keep HSL's and HSV's shape while spacing their channels perceptually, so a hue sweep holds
  its lightness instead of dipping through blue.
- **Two-dimensional planes** — `ColorPlane`, plus `HslPlane`, `OkhslPlane` and `OkhsvPlane`.
  Dragging writes both channels at once, so a plane composes with a `HueSlider` into a full
  picker. They take keyboard focus and move on the arrow keys, a percent at a time and ten
  with shift; a screen reader is offered one named action per direction, since a surface with
  two degrees of freedom has no single adjustable value to expose.
- **Configurable pickers** — every slider in a ready-made picker is a slot, which is how one is
  localized; `enabled` dims a picker and refuses its input; `thumb` reaches every slider at
  once; and `ColorPickerTheme` carries colors, shapes and the new `ColorPickerDimensions` down
  the tree so a replacement inherits them without being handed anything.
- **Value-and-callback overloads** on all six pickers, for a caller holding the colour in a
  view model rather than a `ColorPickerState`.
- `ColorPickerState.isInteracting`, true while any slider or plane is being dragged.

### Fixed

- A hue survives a neutral. Dragging RGB to grey, or lightness to black, used to answer red on
  the hue slider, because a grey has no hue to convert.
- OkLCh reported a hue for every grey but black. The conversion matrices leave a and b around
  `1e-8` rather than at zero, which was enough for an angle to be computed from.
- The saturation slider's desaturated end was `Color.Gray` (`#888888`) while its own thumb
  painted `#808080` there.
- Two sliders dragged at once no longer clear `isInteracting` when the first of them finishes.
- Rastering a plane runs over scalars through a table rather than allocating per pixel and
  calling `pow`. A rebuild on a Pixel 6 Pro went from 36.71 ms to 3.94 ms, and a hue drag above
  a plane rebuilds on every frame.

## 1.1.1 and earlier

See the [GitHub releases](https://github.com/side-codes/anyColorPicker/releases).
