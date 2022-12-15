package kotlinw.util.coroutine

interface SuspendingCloseable {

    suspend fun close()
}

suspend inline fun <T, C : SuspendingCloseable> C.use(block: (C) -> T) =
    try {
        block(this)
    } finally {
        runCatching { close() }
    }
