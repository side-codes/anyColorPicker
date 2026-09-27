package codes.side.color.internal

import kotlin.native.concurrent.ThreadLocal

@ThreadLocal
private val memo = GamutMemo()

internal actual fun gamutMemo(): GamutMemo = memo
