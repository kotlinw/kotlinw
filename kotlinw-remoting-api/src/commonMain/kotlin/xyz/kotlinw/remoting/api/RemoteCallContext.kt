package xyz.kotlinw.remoting.api

import kotlin.coroutines.CoroutineContext

// TODO internal API
interface RemoteCallContext {

    val remoteConnectionId: RemoteConnectionId
}

data class PersistentConnectionRemoteCallContext(override val remoteConnectionId: RemoteConnectionId) :
    RemoteCallContext

expect class RemoteCallContextElement(context: RemoteCallContext) : CoroutineContext.Element {

    val context: RemoteCallContext
}
