package kotlinw.koin.core.internal

import kotlinx.coroutines.CoroutineScope

internal actual fun <T> runBlockingImpl(block: suspend CoroutineScope.() -> T): T {
    TODO("Not yet implemented")
}
