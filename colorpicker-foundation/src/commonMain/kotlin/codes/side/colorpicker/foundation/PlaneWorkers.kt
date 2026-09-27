package codes.side.colorpicker.foundation

/** The processors this platform can run a plane's rows on at once. */
internal expect fun planeWorkers(): Int

/**
 * Whether a plane is built on the thread that also handles input, as `Dispatchers.Default` is in the
 * browser, so its build must step aside every few rows to let events through.
 */
internal expect val planeBuildSharesInputThread: Boolean
