package kotlinw.remoting.api

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel

interface ClientConnection<R: Any?, S: Any?> {

    val receiveChannel: ReceiveChannel<R>

    val sendChannel: SendChannel<S>

    fun disconnect()
}
