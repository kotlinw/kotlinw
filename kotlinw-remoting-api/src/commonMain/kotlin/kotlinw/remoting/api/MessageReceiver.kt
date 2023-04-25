package kotlinw.remoting.api

import kotlinx.coroutines.channels.ReceiveChannel

@OptIn(ExperimentalStdlibApi::class)
interface MessageReceiver<R: Any?>: AutoCloseable {

    val receiveChannel: ReceiveChannel<R>
}
