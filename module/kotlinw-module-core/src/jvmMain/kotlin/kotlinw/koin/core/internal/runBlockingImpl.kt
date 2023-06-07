package kotlinw.koin.core.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking

internal actual fun <T> runBlockingImpl(block: suspend CoroutineScope.() -> T): T = runBlocking(block = block)
