# Changelog

## 1.2.0

### Breaking changes

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
