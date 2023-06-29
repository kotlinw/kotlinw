package kotlinw.remoting.client.ktor

import arrow.core.continuations.AtomicRef
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.client.HttpRemotingClient
import kotlinw.remoting.core.codec.MessageCodecDescriptor
import kotlinw.remoting.core.common.BidirectionalMessagingConnection
import kotlinw.remoting.core.common.SynchronousCallSupport
import kotlinw.remoting.core.common.WebSocketBidirectionalMessagingConnection
import kotlinw.util.stdlib.ByteArrayView.Companion.toReadOnlyByteArray
import kotlinw.util.stdlib.ByteArrayView.Companion.view
import kotlinw.util.stdlib.Url
import kotlinw.util.stdlib.concurrent.value
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
                        (rawParameter as RawMessage.Binary).byteArrayView.toReadOnlyByteArray()
                    } else {
                        (rawParameter as RawMessage.Text).text
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

    override suspend fun <M : RawMessage> connect(
        url: Url,
        messageCodecDescriptor: MessageCodecDescriptor
    ): BidirectionalMessagingConnection<M> {
        val clientWebSocketSession = webSocketSessionDataLock.withLock {
            webSocketSessionDataHolder.value
                ?: httpClient.webSocketSession(url.toString()).also {
                    webSocketSessionDataHolder.value = it
                }
        }

        return WebSocketBidirectionalMessagingConnection(clientWebSocketSession, messageCodecDescriptor)
    }
}
