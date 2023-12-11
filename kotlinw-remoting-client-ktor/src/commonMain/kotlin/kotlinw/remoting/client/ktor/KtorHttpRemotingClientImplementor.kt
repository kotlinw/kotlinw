package kotlinw.remoting.client.ktor

import arrow.atomic.AtomicBoolean
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.codec.MessageCodecDescriptor
import kotlinw.remoting.core.common.BidirectionalCommunicationImplementor
import kotlinw.remoting.core.common.BidirectionalMessagingConnection
import kotlinw.remoting.core.common.SynchronousCallSupport
import kotlinw.remoting.core.ktor.WebSocketBidirectionalMessagingConnection
import kotlinw.util.stdlib.ByteArrayView.Companion.toReadOnlyByteArray
import kotlinw.util.stdlib.ByteArrayView.Companion.view
import kotlinw.util.stdlib.Url
import kotlinx.datetime.Clock

class KtorHttpRemotingClientImplementor(
    private val httpClient: HttpClient,
    private val httpRequestCustomizer: HttpRequestBuilder.() -> Unit = {}
) : SynchronousCallSupport, BidirectionalCommunicationImplementor {

    internal constructor(engine: HttpClientEngine, httpRequestCustomizer: HttpRequestBuilder.() -> Unit = {})
            : this(HttpClient(engine), httpRequestCustomizer)

    override suspend fun <M : RawMessage> call(
        url: String,
        rawParameter: M,
        messageCodecDescriptor: MessageCodecDescriptor
    ): M {
        val response =
            httpClient.post(url) {
                httpRequestCustomizer()

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

    private val runInSessionIsRunning = AtomicBoolean(false)

    override suspend fun runInSession(
        url: Url,
        messageCodecDescriptor: MessageCodecDescriptor,
        block: suspend BidirectionalMessagingConnection.() -> Unit
    ) {
        if (runInSessionIsRunning.compareAndSet(false, true)) {
            try {
                val messagingPeerId = url.toString()
                httpClient.webSocket(
                    url.toString(),
                    request = {
                        // TODO
//                        timeout {
//                            connectTimeoutMillis = 3.seconds.inWholeMilliseconds // TODO config
//                        }
                    }
                ) {
                    block(
                        WebSocketBidirectionalMessagingConnection(
                            messagingPeerId,
                            messagingPeerId + "@" + Clock.System.now().toEpochMilliseconds(),
                            this,
                            messageCodecDescriptor
                        )
                    )
                }
            } catch (e: Exception) {
                // TODO elkapni a websocket specifikus exception-öket, és általánosat dobni helyettük
                throw e
            } finally {
                runInSessionIsRunning.value = false
            }
        } else {
            throw IllegalStateException("Concurrent invocation of ${KtorHttpRemotingClientImplementor::runInSession.name}() is not supported.")
        }
    }
}
