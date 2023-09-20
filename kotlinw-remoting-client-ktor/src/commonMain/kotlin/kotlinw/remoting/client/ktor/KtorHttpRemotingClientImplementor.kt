package kotlinw.remoting.client.ktor

import arrow.atomic.AtomicBoolean
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
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
    private val httpClient: HttpClient
) : SynchronousCallSupport, BidirectionalCommunicationImplementor {

    internal constructor(engine: HttpClientEngine) : this(HttpClient(engine))

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
