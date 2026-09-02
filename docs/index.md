# anyColorPicker — Compose Multiplatform Color Picker

:avocado: Handy, :snake: flexible, and :zap: lightning-fast color picker components for Compose Multiplatform — Android, iOS, Desktop (JVM), and Web (Wasm).

## :pill: Features

- Compose Multiplatform (Android, iOS, Desktop/JVM, Web/Wasm)
- Material 3 theming via `ColorPickerDefaults`
- HSL, RGB, CMYK, and LAB color models
- Alpha channel support
- Zero-drift editing via `ColorPickerState` origin tracking
- Unidirectional data flow — one state object, many components
- Hex string parsing and formatting
- Ready-made color picker dialog
- Accessibility semantics and RTL layout support

## :hammer: Setup

```kotlin
// build.gradle.kts
implementation("codes.side:colorpicker:1.0.0")
```

In a Kotlin Multiplatform project, add it to `commonMain`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("codes.side:colorpicker:1.0.0")
        }
    }
}
```

Published targets: `android`, `jvm`, `iosArm64`, `iosSimulatorArm64`, `wasmJs`.

## :rocket: Quick Start

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

## :art: Components

### Full Pickers

One composable per color model, stacking that model's channel sliders plus an optional alpha slider:

```kotlin
val state = rememberColorPickerState()

HslColorPicker(state = state, showAlpha = true)
RgbColorPicker(state = state, showAlpha = true)
CmykColorPicker(state = state, showAlpha = true)
LabColorPicker(state = state, showAlpha = true)
```

Every full picker and channel slider accepts a `coloringMode` (`AlphaSlider` has none — it always previews the actual color):

- `ColoringMode.Independent` — each slider's gradient shows the channel's full range, independent of the other channels
- `ColoringMode.Contextual` — each slider's gradient previews the actual resulting color at every position

### Individual Sliders

Compose any subset of channel sliders against a shared state:

```kotlin
// HSL
HueSlider(state = state)
SaturationSlider(state = state)
LightnessSlider(state = state)

// RGB
RedSlider(state = state)
GreenSlider(state = state)
BlueSlider(state = state)

// CMYK
CyanSlider(state = state)
MagentaSlider(state = state)
YellowSlider(state = state)
KeySlider(state = state)

// LAB
LightnessLabSlider(state = state)
LabASlider(state = state)
LabBSlider(state = state)

// Alpha (works with any origin space)
AlphaSlider(state = state)
```

Sliders expose `label` and `valueLabel` slots plus `semanticLabel` / `semanticValueText` accessibility parameters.

### Color Swatch

Renders a color over a transparency checkerboard:

```kotlin
ColorSwatch(
    color = state.hslColor.toComposeColor(),
    modifier = Modifier.fillMaxWidth().height(48.dp),
    contentDescription = "Selected color",
)
```

### Dialog

A Material 3 `AlertDialog` with an HSL picker and a live swatch:

```kotlin
ColorPickerDialog(
    onColorSelected = { hsl -> /* confirmed color */ },
    onDismiss = { /* close */ },
    initialColor = HslColor(hue = 200f, saturation = 0.8f, lightness = 0.5f),
    title = "Pick a Color",
    confirmText = "Select",
    dismissText = "Cancel",
    showAlpha = true,
)
```

### Theming

Colors and shapes default to `MaterialTheme` and can be overridden per component:

```kotlin
HslColorPicker(
    state = state,
    colors = ColorPickerDefaults.colors(
        checkerboardLight = Color.White,
        checkerboardDark = Color.LightGray,
    ),
    shapes = ColorPickerDefaults.shapes(
        trackShape = RoundedCornerShape(4.dp),
        swatchShape = RoundedCornerShape(8.dp),
    ),
)
```

## :gear: State & Conversions

`ColorPickerState` is the single source of truth. It stores the color in whichever space was last written to (the *origin*) and derives the other spaces on demand — so editing in a space and reading it back is always exact, and no precision drift accumulates.

```kotlin
val state = rememberColorPickerState()

// Read any color space
state.hslColor
state.rgbColor
state.cmykColor
state.labColor
state.argbInt
state.pickerColor // the authoritative color in its origin space

// Per-channel updates (NaN ignored, values clamped)
state.updateHue(180f)
state.updateRed(1f)
state.updateCyan(0.3f)
state.updateLabLightness(50f)
state.updateAlpha(0.5f)

// Whole-color updates (the written space becomes the origin)
state.updateFromHsl(HslColor(hue = 0f, saturation = 1f, lightness = 0.5f))
state.updateFromRgb(RgbColor(1f, 0f, 0f))
state.updateFromArgbInt(0xFFFF0000.toInt())
```

Use `rememberSaveableColorPickerState()` to keep the state across configuration changes and process death on platforms with saved-instance-state support (primarily Android).

Standalone conversions are float-based extension functions:

```kotlin
val rgb = HslColor(hue = 0f, saturation = 1f, lightness = 0.5f).toRgb()
val cmyk = rgb.toCmyk()
val lab = rgb.toLab()
val composeColor = rgb.toComposeColor()
val backToHsl = composeColor.toHslColor()

// Hex strings
rgb.toHexString()                     // "#FFFF0000"
rgb.toHexString(includeAlpha = false) // "#FF0000"
"#3380CC".toRgbColorOrNull()          // RgbColor, or null on invalid input
```

## :truck: Migrating from andcolorpicker (0.6.x)

The View-based `codes.side:andcolorpicker` artifact (XML `HSLColorPickerSeekBar` and friends) is discontinued. This library is a full Compose Multiplatform rewrite published under new coordinates:

```diff
- implementation("codes.side:andcolorpicker:0.6.2")
+ implementation("codes.side:colorpicker:1.0.0")
```

There is no 1:1 API mapping — migrate by concept:

| andcolorpicker (View-based)                                    | colorpicker (Compose)                                                                                             |
|----------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------|
| `HSLColorPickerSeekBar` (`hslMode` = hue/saturation/lightness) | `HueSlider` / `SaturationSlider` / `LightnessSlider`, or `HslColorPicker` for all three                           |
| `RGBColorPickerSeekBar`                                        | `RedSlider` / `GreenSlider` / `BlueSlider`, or `RgbColorPicker`                                                   |
| `CMYKColorPickerSeekBar`                                       | `CyanSlider` / `MagentaSlider` / `YellowSlider` / `KeySlider`, or `CmykColorPicker`                               |
| `LABColorPickerSeekBar`                                        | `LightnessLabSlider` / `LabASlider` / `LabBSlider`, or `LabColorPicker`                                           |
| `HSLAlphaColorPickerSeekBar`                                   | `AlphaSlider`                                                                                                     |
| `PickerGroup` + `registerPickers`                              | Pass one `ColorPickerState` to every component — they stay in sync automatically                                  |
| `SwatchView`                                                   | `ColorSwatch`                                                                                                     |
| `OnColorPickListener` / `addListener`                          | Read `state.hslColor` (or any other space) — it is Compose snapshot state; use `snapshotFlow` outside composition |
| `IntegerHSLColor` and friends                                  | `HslColor`, `RgbColor`, `CmykColor`, `LabColor` (float-based, with `fromInt` factories)                           |
| `hslColoringMode` = `pure` / `output`                          | `ColoringMode.Independent` / `ColoringMode.Contextual`                                                            |

## :memo: License

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
