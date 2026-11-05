---
title: "Running the samples"
description: "Run the anyColorPicker sample app on desktop, Android and iOS from the repository with Gradle."
icon: material/play-circle-outline
---

The same sample app runs on every supported platform:

```sh
./gradlew :sample:desktopApp:run             # desktop window
./gradlew :sample:androidApp:installDebug    # device or emulator
```

`sample/iosApp` holds the SwiftUI entry points. The Xcode project is not checked in, so
it needs creating once on a Mac against the `ComposeApp` framework that `:sample:shared`
produces.

The app's *Custom thumb* section runs the code in [Custom slider thumb](custom-thumb.md), and its *Built on
foundation* section a picker like the one in [Without Material](without-material.md).
