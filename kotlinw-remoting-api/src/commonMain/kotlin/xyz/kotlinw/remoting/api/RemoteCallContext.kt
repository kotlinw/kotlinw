package xyz.kotlinw.remoting.api

import kotlin.coroutines.CoroutineContext
import kotlin.jvm.JvmInline

@JvmInline
value class RemoteCallContext(
    val remoteConnectionId: RemoteConnectionId
)

expect class RemoteCallContextElement(context: RemoteCallContext) : CoroutineContext.Element {

    val context: RemoteCallContext
}
