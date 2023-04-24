package kotlinw.remoting.api

import kotlinx.coroutines.channels.ReceiveChannel

interface ClientSubscription<R: Any?> {

    val receiveChannel: ReceiveChannel<R>

    fun unsubscribe()
}
