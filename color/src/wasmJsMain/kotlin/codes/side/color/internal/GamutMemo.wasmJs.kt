package codes.side.color.internal

// The browser runs Kotlin on one thread.
private val memo = GamutMemo()

internal actual fun gamutMemo(): GamutMemo = memo
