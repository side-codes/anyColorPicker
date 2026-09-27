package codes.side.colorpicker.foundation

// The browser runs Kotlin on one thread, the one that also handles input.
internal actual fun planeWorkers(): Int = 1
