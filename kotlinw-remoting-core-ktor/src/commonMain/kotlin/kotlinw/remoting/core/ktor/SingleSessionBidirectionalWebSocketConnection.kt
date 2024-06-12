package kotlinw.remoting.core.ktor

import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.close
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.codec.MessageCodecDescriptor
import kotlinw.remoting.core.common.SingleSessionBidirectionalMessagingConnection
import kotlinw.util.stdlib.ByteArrayView.Companion.toReadOnlyByteArray
import kotlinw.util.stdlib.ByteArrayView.Companion.view
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import xyz.kotlinw.remoting.api.RemoteConnectionId
import xyz.kotlinw.util.stdlib.runCatchingCleanup

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
        runCatchingCleanup {
            webSocketSession.close()
        }
        runCatchingCleanup {
            webSocketSession.cancel() // TODO https://youtrack.jetbrains.com/issue/KTOR-4110
        }
    }

    override fun toString(): String {
        return "WebSocketBidirectionalMessagingConnection(remoteConnectionId=$remoteConnectionId, webSocketSession=$webSocketSession, messageCodecDescriptor=$messageCodecDescriptor)"
    }
}
