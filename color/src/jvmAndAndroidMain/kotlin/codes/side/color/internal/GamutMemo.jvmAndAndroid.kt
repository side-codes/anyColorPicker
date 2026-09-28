package codes.side.color.internal

private val memos = ThreadLocal.withInitial(::GamutMemo)

internal actual fun gamutMemo(): GamutMemo = checkNotNull(memos.get())
