# andColorPicker

Multiplatform color picker library for Android & iOS, built with Compose Multiplatform and Material 3.

## Features

- Compose Multiplatform (Android + iOS)
- Material 3 theming
- HSL, RGB, CMYK, and LAB color models
- Alpha channel support
- Lossless color space conversions (float-based, zero drift)
- Unidirectional data flow with `ColorPickerState`
- Color picker dialog

## Setup

```kotlin
// build.gradle.kts
implementation("codes.side:colorpicker:1.0.0")
```

## Quick Start

```kotlin
@Composable
fun MyScreen() {
    val state = rememberColorPickerState(
        initialColor = HslColor(hue = 200f, saturation = 0.8f, lightness = 0.5f)
    )

    Column {
        HslColorPicker(state = state)
        ColorSwatch(
            color = state.hslColor.toComposeColor(),
            modifier = Modifier.size(48.dp)
        )
    }
}
```

## Color Models

All color models use **Float** for full precision. Integer accessors and factories are provided for convenience.

### HSL

```kotlin
val color = HslColor(hue = 210f, saturation = 0.8f, lightness = 0.5f, alpha = 1f)
// hue: [0, 360], saturation/lightness/alpha: [0, 1]

// Integer accessors
color.intHue        // 210
color.intSaturation // 80
color.intLightness  // 50
color.intAlpha      // 255

// From integers
HslColor.fromInt(hue = 210, saturation = 80, lightness = 50, alpha = 255)
```

### RGB

```kotlin
val color = RgbColor(red = 0.2f, green = 0.5f, blue = 0.8f, alpha = 1f)
// All components: [0, 1]

color.intRed   // 51
color.intGreen // 128
color.intBlue  // 204

RgbColor.fromInt(red = 51, green = 128, blue = 204)
```

### CMYK

```kotlin
val color = CmykColor(cyan = 0.3f, magenta = 0.6f, yellow = 0.1f, key = 0.2f)
// All components: [0, 1]

CmykColor.fromInt(cyan = 30, magenta = 60, yellow = 10, key = 20)
```

### LAB

```kotlin
val color = LabColor(l = 53.23f, a = 80.11f, b = 67.22f)
// l: [0, 100], a: [-128, 127], b: [-128, 127], alpha: [0, 1]

LabColor.fromInt(l = 53, a = 80, b = 67)
```

## Conversions

Conversions are extension functions. All operate on floats without rounding.

```kotlin
val hsl = HslColor(hue = 0f, saturation = 1f, lightness = 0.5f)
val rgb = hsl.toRgb()
val cmyk = rgb.toCmyk()
val lab = rgb.toLab()
val argb = rgb.toArgbInt()

// Compose interop
val composeColor: Color = hsl.toComposeColor()
```

## Color Picker Components

### HSL Picker

```kotlin
val state = rememberColorPickerState()

HslColorPicker(
    state = state,
    showAlpha = true,
    coloringMode = ColoringMode.Independent, // or Contextual
)
```

### RGB Picker

```kotlin
RgbColorPicker(state = state, showAlpha = true)
```

### Individual Sliders

```kotlin
HueSlider(state = state)
SaturationSlider(state = state)
LightnessSlider(state = state)
AlphaSlider(state = state)
RedSlider(state = state)
GreenSlider(state = state)
BlueSlider(state = state)
```

### Color Swatch

```kotlin
ColorSwatch(
    color = state.hslColor.toComposeColor(),
    modifier = Modifier.fillMaxWidth().height(48.dp),
)
```

### Dialog

```kotlin
ColorPickerDialog(
    initialColor = HslColor(hue = 200f, saturation = 0.8f, lightness = 0.5f),
    onColorSelected = { hsl -> /* use hsl */ },
    onDismiss = { /* close */ },
)
```

## State Management

`ColorPickerState` is the single source of truth. It reads and writes each color space natively, with no round-trip conversions.

```kotlin
val state = rememberColorPickerState()

// Read any color space
state.hslColor
state.rgbColor
state.cmykColor
state.labColor
state.argbInt

// Write from any color space
state.updateHue(180f)
state.updateSaturation(0.5f)
state.updateFromRgb(RgbColor(1f, 0f, 0f))
state.updateFromCmyk(cmykColor)
state.updateFromLab(labColor)
state.updateFromArgbInt(0xFFFF0000.toInt())
```

On Android, use `rememberSaveableColorPickerState()` to survive process death.

## Architecture: Zero-Drift Color Conversions

Color space conversions are inherently lossy when values are quantized to integers, and even with floats, transcendental functions (used in LAB) introduce IEEE 754 rounding errors. Industry-standard tools (Photoshop, CSS Color Level 4, Sass) solve this the same way we do:

**Store colors in their authored color space. Convert forward only. Never convert back.**

`ColorPickerState` tracks which color space was last written to (the *origin*). When you read a different space, it converts forward once from the origin. The origin value is never re-derived from a conversion.

```
User drags Red slider
  -> _rgb is written directly (origin = RGB, zero conversions)
  -> UI reads hslColor -> converts RGB->HSL once (forward only)
  -> UI reads rgbColor -> returns _rgb as-is (zero conversions)
```

This means:
- Editing in RGB and reading back RGB produces **the exact original value**
- Editing in HSL and reading back HSL produces **the exact original value**
- Cross-space reads involve a single forward conversion, never a round-trip
- No precision loss accumulates over time, regardless of how many edits are made

For more details, see:
- [CSS Color Module Level 4](https://www.w3.org/TR/css-color-4/) -- the W3C spec mandates the same approach
- [Sass Color Spaces](https://css.oddbird.net/sass/color-spaces/proposal/) -- stores colors in their original space

## License

```
Copyright 2020 Illia Achour

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
