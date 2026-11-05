---
title: "Setup and requirements"
description: "Add anyColorPicker to an Android or Kotlin Multiplatform project from Maven Central: the artifacts, published targets and requirements."
section: Get started
nav_order: 10
---

anyColorPicker is published on Maven Central. For the Material 3 pickers:

```kotlin
// build.gradle.kts
implementation("codes.side:colorpicker-material3:2.0.0")
```

In a Kotlin Multiplatform project, add it to `commonMain`:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("codes.side:colorpicker-material3:2.0.0")
        }
    }
}
```

On a design system other than Material, depend on `codes.side:colorpicker-foundation` instead and draw
the pickers yourself: see [Without Material](without-material.md).

## Artifacts

Each artifact brings the ones below it, and each works on its own:

| Artifact                            | Holds                                                                                                                                                         |
|-------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `codes.side:colorpicker-material3`  | The Material 3 pickers, sliders, planes, swatch, dialog and theme, in `codes.side.colorpicker.material3`.                                                     |
| `codes.side:colorpicker-foundation` | `ColorPickerState`, in `codes.side.colorpicker.state`; the `Basic*` components and `ColorPickerStrings`, in `codes.side.colorpicker.foundation`. No Material. |
| `codes.side:color-compose`          | `ColorValue.toComposeColor()` and `Color.toColorValue()`.                                                                                                     |
| `codes.side:color`                  | `ColorValue`, the color spaces, conversion, gamut mapping, CSS strings and hex. No Compose runtime or UI.                                                     |

Published targets: `android`, `jvm`, `iosArm64`, `iosSimulatorArm64`, `wasmJs`.

## Requirements

Built with Kotlin 2.4.20, Compose Multiplatform 1.12.1 and Material 3 1.9.0. Android needs
`minSdk` 24 and is built against `compileSdk` 37; the desktop jars are Java 17 bytecode. Building
the repository itself needs JDK 21.

The 1.x `codes.side:colorpicker` artifact cannot share a classpath with 2.x: both ship
`codes.side.colorpicker.state.ColorPickerState`. [Migrating from 1.x](migrating-from-1x.md) maps one API onto the other.

Next: [Gallery](gallery.md) shows every picker, and [Color pickers](pickers.md) how to use them.
