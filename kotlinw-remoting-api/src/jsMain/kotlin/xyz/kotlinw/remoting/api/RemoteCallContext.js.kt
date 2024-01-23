package xyz.kotlinw.remoting.api

import kotlin.coroutines.CoroutineContext

actual class RemoteCallContextElement actual constructor(
    actual val context: RemoteCallContext
) : CoroutineContext.Element {

    companion object Key : CoroutineContext.Key<RemoteCallContextElement>

    override val key: CoroutineContext.Key<RemoteCallContextElement> get() = Key
}
