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
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlinw.remoting.core.client.HttpRemotingClient
import kotlinw.remoting.core.client.HttpRemotingClient.BidirectionalCommunicationImplementor.BidirectionalConnection
import kotlinw.remoting.core.codec.MessageCodecDescriptor
import kotlinw.remoting.core.RawMessage
import kotlinw.util.stdlib.Url
import kotlinw.util.stdlib.concurrent.value
import kotlinw.util.stdlib.toReadOnlyByteArray
import kotlinw.util.stdlib.view
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class KtorHttpRemotingClientImplementor(
    private val httpClient: HttpClient
) : HttpRemotingClient.SynchronousCallSupportImplementor, HttpRemotingClient.BidirectionalCommunicationImplementor {

    constructor(engine: HttpClientEngine) : this(HttpClient(engine))

    override suspend fun <M : RawMessage> post(
        url: String,
        requestBody: M,
        messageCodecDescriptor: MessageCodecDescriptor
    ): M {
        val response =
            httpClient.post(url) {
                header(HttpHeaders.Accept, messageCodecDescriptor.contentType)
                header(HttpHeaders.ContentType, messageCodecDescriptor.contentType)

                setBody(
                    if (messageCodecDescriptor.isBinary) {
                        (requestBody as RawMessage.Binary).byteArrayView.toReadOnlyByteArray()
                    } else {
                        (requestBody as RawMessage.Text).text
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
    ): BidirectionalConnection<M> {
        val clientWebSocketSession = webSocketSessionDataLock.withLock {
            webSocketSessionDataHolder.value
                ?: httpClient.webSocketSession(url.toString()).also {
                    webSocketSessionDataHolder.value = it
                }
        }

        return object : BidirectionalConnection<M>, CoroutineScope by clientWebSocketSession {

            override suspend fun incomingMessages(): Flow<M> =
                flow {
                    for (frame in clientWebSocketSession.incoming) {
                        emit(
                            if (messageCodecDescriptor.isBinary) {
                                RawMessage.Binary((frame as Frame.Binary).readBytes().view())
                            } else {
                                RawMessage.Text((frame as Frame.Text).readText())
                            }
                                    as M
                        )
                    }
                }

            override suspend fun sendMessage(rawMessage: M) {
                if (messageCodecDescriptor.isBinary) {
                    clientWebSocketSession.send((rawMessage as RawMessage.Binary).byteArrayView.toReadOnlyByteArray())
                } else {
                    clientWebSocketSession.send((rawMessage as RawMessage.Text).text)
                }
            }
        }
    }
}
