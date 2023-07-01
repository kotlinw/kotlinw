package kotlinw.remoting.client.ktor

import arrow.core.continuations.AtomicRef
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.websocket.*
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.client.HttpRemotingClient
import kotlinw.remoting.core.codec.MessageCodecDescriptor
import kotlinw.remoting.core.common.BidirectionalMessagingConnection
import kotlinw.remoting.core.common.SynchronousCallSupport
import kotlinw.remoting.core.ktor.WebSocketBidirectionalMessagingConnection
import kotlinw.util.stdlib.ByteArrayView.Companion.toReadOnlyByteArray
import kotlinw.util.stdlib.ByteArrayView.Companion.view
import kotlinw.util.stdlib.Url
import kotlinw.util.stdlib.concurrent.value
import kotlinw.uuid.Uuid
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class KtorHttpRemotingClientImplementor(
    private val httpClient: HttpClient
) : SynchronousCallSupport, HttpRemotingClient.BidirectionalCommunicationImplementor {

    constructor(engine: HttpClientEngine) : this(HttpClient(engine))

    override suspend fun <M : RawMessage> call(
        url: String,
        rawParameter: M,
        messageCodecDescriptor: MessageCodecDescriptor
    ): M {
        val response =
            httpClient.post(url) {
                header(HttpHeaders.Accept, messageCodecDescriptor.contentType)
                header(HttpHeaders.ContentType, messageCodecDescriptor.contentType)

                setBody(
                    if (messageCodecDescriptor.isBinary) {
                        check(rawParameter is RawMessage.Binary)
                        rawParameter.byteArrayView.toReadOnlyByteArray()
                    } else {
                        check(rawParameter is RawMessage.Text)
                        rawParameter.text
                    }
                )
            }

        return if (messageCodecDescriptor.isBinary)
            RawMessage.Binary(response.body<ByteArray>().view()) as M
        else
            RawMessage.Text(response.bodyAsText()) as M
    }

    private val webSocketSessionDataHolder = AtomicRef<DefaultClientWebSocketSession?>(null)

    private val webSocketSessionDataLock = Mutex()

    override suspend fun connect(
        url: Url,
        messageCodecDescriptor: MessageCodecDescriptor
    ): BidirectionalMessagingConnection {
        val clientWebSocketSession = webSocketSessionDataLock.withLock {
            webSocketSessionDataHolder.value
                ?: httpClient.webSocketSession(url.toString()).also {
                    webSocketSessionDataHolder.value = it
                }
        }

        val messagingPeerId = url.toString()
        return WebSocketBidirectionalMessagingConnection(
            messagingPeerId,
            Uuid.randomUuid().toString(),
            clientWebSocketSession,
            messageCodecDescriptor
        )
    }
}
