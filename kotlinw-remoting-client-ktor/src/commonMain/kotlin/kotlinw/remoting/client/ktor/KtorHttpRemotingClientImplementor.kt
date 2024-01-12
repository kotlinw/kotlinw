package kotlinw.remoting.client.ktor

import arrow.atomic.AtomicBoolean
import arrow.core.NonFatal
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.plugins.pluginOrNull
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
import kotlinw.remoting.core.common.BidirectionalMessagingConnection
import kotlinw.remoting.core.common.RemoteConnectionId
import kotlinw.remoting.core.common.SynchronousCallSupport
import kotlinw.remoting.core.ktor.WebSocketBidirectionalMessagingConnection
import kotlinw.util.stdlib.ByteArrayView.Companion.toReadOnlyByteArray
import kotlinw.util.stdlib.ByteArrayView.Companion.view
import kotlinw.util.stdlib.Url
import kotlinx.datetime.Clock

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
        // TODO itt ilyen is dobódhat, ha nem fut a szerver
//        java.io.IOException: HTTP/1.1 header parser received no bytes
//        at _COROUTINE._BOUNDARY._(CoroutineDebugging.kt:46)
//        at io.ktor.client.engine.java.JavaHttpResponseKt.executeHttpRequest(JavaHttpResponse.kt:19)
//        at io.ktor.client.engine.java.JavaHttpEngine.execute(JavaHttpEngine.kt:42)
//        at io.ktor.client.engine.HttpClientEngine$executeWithinCallContext$2.invokeSuspend(HttpClientEngine.kt:99)
//        at io.ktor.client.engine.HttpClientEngine$DefaultImpls.executeWithinCallContext(HttpClientEngine.kt:100)
//        at io.ktor.client.engine.HttpClientEngine$install$1.invokeSuspend(HttpClientEngine.kt:70)
//        at io.ktor.client.plugins.HttpSend$DefaultSender.execute(HttpSend.kt:138)
//        at io.ktor.client.plugins.HttpRedirect$Plugin$install$1.invokeSuspend(HttpRedirect.kt:64)
//        at io.ktor.client.plugins.HttpCallValidator$Companion$install$3.invokeSuspend(HttpCallValidator.kt:151)
//        at io.ktor.client.plugins.HttpSend$Plugin$install$1.invokeSuspend(HttpSend.kt:104)
//        at io.ktor.client.plugins.DefaultTransformKt$defaultTransformers$1.invokeSuspend(DefaultTransform.kt:57)
//        at io.ktor.client.plugins.HttpCallValidator$Companion$install$1.invokeSuspend(HttpCallValidator.kt:130)
//        at io.ktor.client.plugins.HttpRequestLifecycle$Plugin$install$1.invokeSuspend(HttpRequestLifecycle.kt:38)
//        at io.ktor.client.HttpClient.execute$ktor_client_core(HttpClient.kt:191)
//        at io.ktor.client.statement.HttpStatement.executeUnsafe(HttpStatement.kt:108)
//        at io.ktor.client.statement.HttpStatement.execute(HttpStatement.kt:47)
//        at kotlinw.remoting.client.ktor.KtorHttpRemotingClientImplementor.call(KtorHttpRemotingClientImplementor.kt:129)
//        at kotlinw.remoting.core.client.HttpRemotingClient.call(HttpRemotingClient.kt:185)
//        at com.whocos.dsl.InstallDevEnvironmentDslIntegrationTest$queryEnvironment$1.invokeSuspend(InstallDevEnvironmentDslIntegrationTest.kt:40)
//        Caused by: java.io.IOException: HTTP/1.1 header parser received no bytes
//        at java.net.http/jdk.internal.net.http.common.Utils.wrapWithExtraDetail(Utils.java:348)
//        at java.net.http/jdk.internal.net.http.Http1Response$HeadersReader.onReadError(Http1Response.java:565)
//        Caused by: java.io.EOFException: EOF reached while reading

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
        block: suspend BidirectionalMessagingConnection.() -> Unit
    ) {
        if (runInSessionIsRunning.compareAndSet(false, true)) {
            try {
                httpClient.pluginOrNull(WebSockets)
                    ?: throw IllegalStateException("${WebSockets.Plugin.key} plugin is not installed.")

                val messagingPeerId = url.toString()
                // TODO túl későn, csak itt derül ki, ha a WebSockets plugin nincs install-álva

                logger.debug { "Connecting to WebSocket server: " / url }

                httpClient.webSocketSession(url.toString()).also { clientWebSocketSession ->
                    logger.debug { "Connected to WebSocket server: " / url }

                    try {
                        block(
                            WebSocketBidirectionalMessagingConnection(
                                RemoteConnectionId(
                                    messagingPeerId,
                                    messagingPeerId + "@" + Clock.System.now().toEpochMilliseconds()
                                ),
                                clientWebSocketSession,
                                messageCodecDescriptor
                            )
                        )
                    } finally {
                        clientWebSocketSession.close()
                        logger.debug { "Disconnected from WebSocket server: " / url }
                    }
                }
            } catch (e: Throwable) {
                if (NonFatal(e)) {
                    if (logger.isTraceEnabled) {
                        logger.trace(e) { "WebSocket connection failed." }
                    } else {
                        logger.debug { "WebSocket connection failed." }
                    }
                }

                throw e
            } finally {
                runInSessionIsRunning.value = false
            }
        } else {
            throw IllegalStateException("Concurrent invocation of ${KtorHttpRemotingClientImplementor::runInSession.name}() is not supported.")
        }
    }
}
