package kotlinw.remoting.client.ktor

import arrow.atomic.AtomicBoolean
import arrow.core.NonFatal
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.client.plugins.websocket.webSocketSession
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.isSuccess
import io.ktor.websocket.close
import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.codec.MessageCodecDescriptor
import kotlinw.remoting.core.common.BidirectionalCommunicationImplementor
import kotlinw.remoting.core.common.SingleSessionBidirectionalMessagingConnection
import kotlinw.remoting.core.common.SynchronousCallSupport
import kotlinw.remoting.core.ktor.SingleSessionBidirectionalWebSocketConnection
import kotlinw.util.stdlib.ByteArrayView.Companion.toReadOnlyByteArray
import kotlinw.util.stdlib.ByteArrayView.Companion.view
import kotlinw.util.stdlib.Url
import kotlinx.coroutines.cancel
import kotlinx.datetime.Clock.System
import xyz.kotlinw.remoting.api.RemoteConnectionId

class KtorHttpRemotingClientImplementor(
    private val httpClient: HttpClient,
    loggerFactory: LoggerFactory,
    private val httpRequestCustomizer: HttpRequestBuilder.() -> Unit = {}
) : SynchronousCallSupport, BidirectionalCommunicationImplementor {

    private val logger = loggerFactory.getLogger()

    internal constructor(
        engine: HttpClientEngine,
        loggerFactory: LoggerFactory,
        httpRequestCustomizer: HttpRequestBuilder.() -> Unit = {}
    )
            : this(HttpClient(engine), loggerFactory, httpRequestCustomizer)

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

        return if (response.status.isSuccess()) {
            if (messageCodecDescriptor.isBinary)
                RawMessage.Binary(response.body<ByteArray>().view()) as M
            else
                RawMessage.Text(response.bodyAsText()) as M
        } else {
            throw RuntimeException("Response status: ${response.status}") // TODO more info, specific result
        }
    }

    private val runInSessionIsRunning = AtomicBoolean(false)

    override suspend fun runInSession(
        url: Url,
        messageCodecDescriptor: MessageCodecDescriptor,
        block: suspend SingleSessionBidirectionalMessagingConnection.() -> Unit
    ) {
        if (runInSessionIsRunning.compareAndSet(false, true)) {
            try {
                httpClient.pluginOrNull(WebSockets)
                    ?: throw IllegalStateException("${WebSockets.Plugin.key} plugin is not installed.")

                val messagingPeerId = url.toString()
                // TODO túl későn, csak itt derül ki, ha a WebSockets plugin nincs install-álva

                logger.debug { "Connecting to WebSocket server: " / url }
                var clientWebSocketSession: DefaultClientWebSocketSession? = null
                try {
                    clientWebSocketSession = httpClient.webSocketSession(url.toString())
                    logger.debug { "Connected to WebSocket server: " / url }

                    block(
                        SingleSessionBidirectionalWebSocketConnection(
                            RemoteConnectionId(
                                messagingPeerId,
                                messagingPeerId + "@" + System.now().toEpochMilliseconds()
                            ),
                            clientWebSocketSession,
                            messageCodecDescriptor
                        )
                    )

                    logger.debug { "Closing WebSocket connection normally: " / url }
                    runCatching {
                        clientWebSocketSession.close()
                    }
                } catch (e: Exception) {
                    if (NonFatal(e)) {
                        if (logger.isDebugEnabled) {
                            logger.debug(e) { "WebSocket connection failed: " / url }
                        } else {
                            logger.info { "WebSocket connection failed: " / url }
                        }
                    }

                    runCatching {
                        clientWebSocketSession?.close()
                    }

                    throw e
                } finally {
                    clientWebSocketSession?.cancel() // TODO https://youtrack.jetbrains.com/issue/KTOR-4110
                    logger.debug { "Disconnected from WebSocket server: " / url }
                }
            } finally {
                runInSessionIsRunning.value = false
            }
        } else {
            throw IllegalStateException("Concurrent invocation of ${KtorHttpRemotingClientImplementor::runInSession.name}() is not supported.")
        }
    }
}
