package kotlinw.remoting.core.common

import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.RawMessage.Binary
import kotlinw.remoting.core.RawMessage.Text
import kotlinw.remoting.core.codec.MessageCodecDescriptor
import kotlinw.util.stdlib.ByteArrayView.Companion.toReadOnlyByteArray
import kotlinw.util.stdlib.ByteArrayView.Companion.view
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class WebSocketBidirectionalMessagingConnection<M : RawMessage>(
    private val webSocketSession: DefaultWebSocketSession,
    private val messageCodecDescriptor: MessageCodecDescriptor
) : BidirectionalMessagingConnection<M>, CoroutineScope by webSocketSession {

    override suspend fun incomingRawMessages(): Flow<M> =
        flow {
            for (frame in webSocketSession.incoming) {
                emit(
                    if (messageCodecDescriptor.isBinary) {
                        Binary((frame as Frame.Binary).readBytes().view())
                    } else {
                        Text((frame as Frame.Text).readText())
                    }
                            as M
                )
            }
        }

    override suspend fun sendMessage(rawMessage: M) {
        if (messageCodecDescriptor.isBinary) {
            webSocketSession.send((rawMessage as RawMessage.Binary).byteArrayView.toReadOnlyByteArray())
        } else {
            webSocketSession.send((rawMessage as RawMessage.Text).text)
        }
    }
}
