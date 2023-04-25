package kotlinw.remoting.api

import kotlinx.coroutines.channels.ReceiveChannel

@OptIn(ExperimentalStdlibApi::class)
interface MessageSender<S: Any?>: AutoCloseable {

    val sendChannel: ReceiveChannel<S>
}
