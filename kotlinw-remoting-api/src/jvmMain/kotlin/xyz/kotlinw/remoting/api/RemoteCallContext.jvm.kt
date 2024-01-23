package xyz.kotlinw.remoting.api

import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.ThreadContextElement

private val remoteCallContextTls = ThreadLocal<RemoteCallContext?>()

actual class RemoteCallContextElement actual constructor(
    actual val context: RemoteCallContext
) : ThreadContextElement<RemoteCallContext?>, CoroutineContext.Element {

    companion object Key : CoroutineContext.Key<RemoteCallContextElement>

    override val key: CoroutineContext.Key<RemoteCallContextElement> get() = Key

    override fun updateThreadContext(context: CoroutineContext): RemoteCallContext? {
        val previousState = remoteCallContextTls.get()
        remoteCallContextTls.set(context[RemoteCallContextElement]?.context)
        return previousState
    }

    override fun restoreThreadContext(context: CoroutineContext, oldState: RemoteCallContext?) {
        if (oldState != null) {
            remoteCallContextTls.set(oldState)
        } else {
            remoteCallContextTls.remove()
        }
    }
}
