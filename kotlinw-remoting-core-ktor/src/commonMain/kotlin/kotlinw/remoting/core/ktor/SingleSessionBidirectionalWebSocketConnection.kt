package kotlinw.remoting.core.ktor

import io.ktor.websocket.*
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.codec.MessageCodecDescriptor
import kotlinw.remoting.core.common.SingleSessionBidirectionalMessagingConnection
import kotlinw.remoting.core.common.RemoteConnectionId
import kotlinw.util.stdlib.ByteArrayView.Companion.toReadOnlyByteArray
import kotlinw.util.stdlib.ByteArrayView.Companion.view
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class SingleSessionBidirectionalWebSocketConnection(
    override val remoteConnectionId: RemoteConnectionId,
    private val webSocketSession: DefaultWebSocketSession,
    private val messageCodecDescriptor: MessageCodecDescriptor
) : SingleSessionBidirectionalMessagingConnection, CoroutineScope by webSocketSession {

    override suspend fun incomingRawMessages(): Flow<RawMessage> =
        flow {
            for (frame in webSocketSession.incoming) {
                emit(
                    if (messageCodecDescriptor.isBinary) {
                        check(frame is Frame.Binary)
                        RawMessage.Binary(frame.readBytes().view())
                    } else {
                        check(frame is Frame.Text)
                        RawMessage.Text(frame.readText())
                    }
                )
            }
        }

    override suspend fun sendRawMessage(rawMessage: RawMessage) {
        if (messageCodecDescriptor.isBinary) {
            check(rawMessage is RawMessage.Binary)
            webSocketSession.send(rawMessage.byteArrayView.toReadOnlyByteArray())
        } else {
            check(rawMessage is RawMessage.Text)
            webSocketSession.send(rawMessage.text)
        }
    }

    override suspend fun close() {
        webSocketSession.close()
    }

    override fun toString(): String {
        return "WebSocketBidirectionalMessagingConnection(remoteConnectionId=$remoteConnectionId, webSocketSession=$webSocketSession, messageCodecDescriptor=$messageCodecDescriptor)"
    }
}