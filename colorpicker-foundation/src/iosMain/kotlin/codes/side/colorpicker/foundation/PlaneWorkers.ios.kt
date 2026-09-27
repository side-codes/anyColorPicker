package codes.side.colorpicker.foundation

import platform.Foundation.NSProcessInfo

internal actual fun planeWorkers(): Int = NSProcessInfo.processInfo.activeProcessorCount.toInt()

internal actual val planeBuildSharesInputThread: Boolean = false
