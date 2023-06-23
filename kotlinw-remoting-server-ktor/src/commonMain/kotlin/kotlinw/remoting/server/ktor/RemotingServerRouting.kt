package kotlinw.remoting.server.ktor

import arrow.core.continuations.AtomicRef
import arrow.core.continuations.getAndUpdate
import arrow.core.continuations.update
import arrow.core.nonFatalOrThrow
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import kotlinw.remoting.api.internal.server.RemoteCallDelegator
import kotlinw.remoting.api.internal.server.RemotingMethodDescriptor
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.RemotingMessage
import kotlinw.remoting.core.RemotingMessageKind
import kotlinw.remoting.core.RemotingMessageMetadata
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.core.codec.MessageCodecWithMetadataPrefetchSupport
import kotlinw.util.stdlib.ByteArrayView.Companion.toReadOnlyByteArray
import kotlinw.util.stdlib.ByteArrayView.Companion.view
import kotlinw.util.stdlib.collection.ConcurrentHashMap
import kotlinw.util.stdlib.collection.ConcurrentMutableMap
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import kotlin.collections.Collection
import kotlin.collections.Map
import kotlin.collections.any
import kotlin.collections.associateBy
import kotlin.collections.set
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

private typealias ClientSessionId = Any

enum class ServerToClientCommunicationType {
    WebSockets
    // TODO ServerSentEvents
}

class RemotingConfiguration {

    var messageCodec: MessageCodec<out RawMessage>? = null

    var remoteCallDelegators: Collection<RemoteCallDelegator>? = null

    var identifyClient: ((ApplicationCall) -> ClientSessionId)? = null

    var supportServerToClientCommunication: Boolean = true
}

private val logger by lazy { PlatformLogging.getLogger() }

val RemotingPlugin =
    createApplicationPlugin(
        name = "RemotingPlugin",
        createConfiguration = ::RemotingConfiguration
    ) {
        val identifyClient = requireNotNull(pluginConfig.identifyClient)
        val remoteCallDelegators = requireNotNull(pluginConfig.remoteCallDelegators)
        val messageCodec = requireNotNull(pluginConfig.messageCodec)

        val delegators = remoteCallDelegators.associateBy { it.servicePath }

        val supportServerToClientCommunication = pluginConfig.supportServerToClientCommunication
        if (supportServerToClientCommunication) {
            if (application.pluginOrNull(WebSockets) == null) {
                throw IllegalStateException(MissingApplicationPluginException(WebSockets.key)) // TODO szövegben help link
            }

            if (messageCodec !is MessageCodecWithMetadataPrefetchSupport) {
                throw IllegalStateException("Configuration value '${RemotingConfiguration::messageCodec.name}' must be an instance of '${MessageCodecWithMetadataPrefetchSupport::class.simpleName}'  because server-to-client messaging is required.")
            }
        }

        val connections: ConcurrentMutableMap<ClientSessionId, WebSocketConnection> = ConcurrentHashMap()

        application.routing {
            route("/remoting") {
                if (supportServerToClientCommunication) {
                    setupWebsocketRouting(
                        messageCodec as MessageCodecWithMetadataPrefetchSupport<RawMessage>,
                        delegators,
                        identifyClient,
                        addConnection = { clientId, webSocketConnection ->
                            connections.compute(clientId) { key, previousValue ->
                                check(previousValue == null) { "Client is already connected: $key" }
                                webSocketConnection
                            }
                        },
                        removeConnection = {
                            connections.remove(it)
                        }
                    )
                }

                setupRemoteCallRouting(
                    messageCodec,
                    delegators,
                    identifyClient,
                    addActiveColdFlow = { clientId, flow, flowId, flowValueSerializer ->
                        connections[clientId]?.addActiveColdFlow(flowId, flow, flowValueSerializer as KSerializer<Any?>)
                            ?: throw IllegalStateException("Websocket connection is not active for clientId=$clientId")
                    }
                )
            }
        }
    }

private class WebSocketConnection(
    private val messageCodec: MessageCodec<out RawMessage>,
    private val webSocketSession: DefaultWebSocketSession
) {
    private class ActiveColdFlowData(val flowManagerCoroutineJob: Job) {

        val suspendedCoroutine = AtomicRef<Continuation<Unit>?>(null)
    }

    private val activeColdFlows: ConcurrentMutableMap<String, ActiveColdFlowData> = ConcurrentHashMap()

    private val websocketSessionSupervisorScope = CoroutineScope(SupervisorJob(webSocketSession.coroutineContext.job))

    fun onColdFlowValueCollected(flowId: String) {
        val activeColdFlowData = activeColdFlows[flowId] ?: throw IllegalStateException()
        val continuation = checkNotNull(activeColdFlowData.suspendedCoroutine.getAndUpdate { null })
        continuation.resume(Unit)
    }

    fun addActiveColdFlow(flowId: String, flow: Flow<Any?>, flowValueSerializer: KSerializer<Any?>) {
        activeColdFlows[flowId] = ActiveColdFlowData(
            websocketSessionSupervisorScope.launch(start = CoroutineStart.LAZY) {
                try {
                    flow.collect {
                        send(
                            RemotingMessage(
                                it,
                                RemotingMessageMetadata(
                                    messageKind = RemotingMessageKind.ColdFlowCollectKind.ColdFlowValue(flowId)
                                )
                            ),
                            flowValueSerializer
                        )

                        val currentColdFlowData = activeColdFlows[flowId] ?: throw IllegalStateException()
                        suspendCancellableCoroutine { continuation ->
                            currentColdFlowData.suspendedCoroutine.update {
                                check(it == null)
                                continuation
                            }
                        }
                    }

                    send(
                        RemotingMessage(
                            Unit, // TODO null
                            RemotingMessageMetadata(
                                messageKind = RemotingMessageKind.ColdFlowCollectKind.ColdFlowCompleted(flowId, true)
                            )
                        ),
                        serializer<Unit>()
                    )
                } catch (e: Exception) {
                    throw e.nonFatalOrThrow() // TODO
                } finally {
                    activeColdFlows.remove(flowId)
                }
            }
        )
    }

    suspend fun <T> send(message: RemotingMessage<T>, payloadSerializer: KSerializer<T>) {
        if (messageCodec.isBinary) {
            webSocketSession.send(
                (messageCodec.encodeMessage(message, payloadSerializer) as RawMessage.Binary)
                    .byteArrayView.toReadOnlyByteArray()
            )
        } else {
            webSocketSession.send(
                (messageCodec.encodeMessage(message, payloadSerializer) as RawMessage.Text).text
            )
        }
    }

    fun onCollectColdFlow(flowId: String) {
        val coldFlowData = activeColdFlows[flowId] ?: throw IllegalStateException()
        coldFlowData.flowManagerCoroutineJob.start()
    }
}

private fun Route.setupWebsocketRouting(
    messageCodec: MessageCodecWithMetadataPrefetchSupport<RawMessage>,
    delegators: Map<String, RemoteCallDelegator>,
    identifyClient: (ApplicationCall) -> ClientSessionId,
    addConnection: (ClientSessionId, WebSocketConnection) -> Unit,
    removeConnection: (ClientSessionId) -> Unit
) {

    suspend fun processIncomingMessage(webSocketConnection: WebSocketConnection, rawMessage: RawMessage) {
        val metadata = messageCodec.extractMetadata(rawMessage)
        val messageKind = metadata.metadata?.messageKind ?: throw IllegalStateException() // TODO hibaüz.

        when (messageKind) {
            is RemotingMessageKind.CallRequest -> TODO(messageKind.toString())

            is RemotingMessageKind.CallResponse -> TODO(messageKind.toString())

            is RemotingMessageKind.ColdFlowCollectKind.ColdFlowCompleted -> TODO(messageKind.toString())

            is RemotingMessageKind.ColdFlowCollectKind.ColdFlowValue -> TODO(messageKind.toString())

            is RemotingMessageKind.CollectColdFlow ->
                webSocketConnection.onCollectColdFlow(messageKind.callId)

            is RemotingMessageKind.ColdFlowValueCollected ->
                webSocketConnection.onColdFlowValueCollected(messageKind.callId)
        }
    }

    // TODO fix string
    webSocket("/websocket") {
        val clientId = identifyClient(call) // TODO hibaell.

        try {
            val webSocketConnection = WebSocketConnection(messageCodec, this)
            addConnection(clientId, webSocketConnection)

            for (frame in incoming) {
                when (frame) {
                    is Frame.Binary -> processIncomingMessage(
                        webSocketConnection,
                        RawMessage.Binary(frame.readBytes().view())
                    )

                    is Frame.Text -> processIncomingMessage(webSocketConnection, RawMessage.Text(frame.readText()))
                    else -> {
                        // Ignore
                    }
                }
            }
        } catch (e: Exception) {
            // TODO log
        } finally {
            removeConnection(clientId)
        }
    }
}

private fun Route.setupRemoteCallRouting(
    messageCodec: MessageCodec<out RawMessage>,
    remoteCallDelegators: Map<String, RemoteCallDelegator>,
    identifyClient: (ApplicationCall) -> ClientSessionId,
    addActiveColdFlow: (clientSessionId: ClientSessionId, flow: Flow<Any?>, flowId: String, flowValueSerializer: KSerializer<out Any?>) -> Unit
) {
    val contentType = ContentType.parse(messageCodec.contentType)

    route("/call") { // TODO configurable path
        contentType(contentType) {
            logger.info { "Remote call handlers: " / remoteCallDelegators }
            post("/{serviceId}/{methodId}") {
                // TODO handle errors

                val serviceId = call.parameters["serviceId"]
                if (serviceId != null) {
                    val delegator = remoteCallDelegators[serviceId]
                    if (delegator != null) {
                        val methodId = call.parameters["methodId"]
                        if (methodId != null) {
                            val methodDescriptor = delegator.methodDescriptors[methodId]
                            if (methodDescriptor != null) {
                                logger.trace { "Processing RPC call: " / serviceId / methodId }

                                when (methodDescriptor) {
                                    is RemotingMethodDescriptor.DownstreamColdFlow<*, *> ->
                                        handleDownstreamColdFlowProviderCall(
                                            call,
                                            messageCodec,
                                            methodDescriptor,
                                            delegator,
                                            identifyClient,
                                            addActiveColdFlow
                                        )

                                    is RemotingMethodDescriptor.SynchronousCall<*, *> ->
                                        handleSynchronousCall(
                                            call,
                                            messageCodec,
                                            methodDescriptor,
                                            delegator
                                        )
                                }
                            } else {
                                logger.warning {
                                    "Invalid incoming RPC call, handler does not support the requested method: " /
                                            listOf(serviceId, methodId)
                                }
                            }
                        } else {
                            logger.warning {
                                "Invalid incoming RPC call, no `methodId` present: " /
                                        named("serviceId", serviceId) / call.request.uri
                            }
                        }
                    } else {
                        logger.warning {
                            "Invalid incoming RPC call, no handler found for " /
                                    named("serviceId", serviceId)
                        }
                    }
                } else {
                    logger.warning { "Invalid incoming RPC call, no `serviceId` present: " / call.request.uri }
                }
            }
        }
    }
}

private suspend fun <M : RawMessage> handleDownstreamColdFlowProviderCall(
    call: ApplicationCall,
    messageCodec: MessageCodec<M>,
    callDescriptor: RemotingMethodDescriptor.DownstreamColdFlow<*, *>,
    delegator: RemoteCallDelegator,
    identifyClient: (ApplicationCall) -> ClientSessionId,
    addActiveColdFlow: (clientSessionId: ClientSessionId, flow: Flow<Any?>, flowId: String, flowValueSerializer: KSerializer<out Any?>) -> Unit
) {
    val (parameter, metadata) =
        decodeRequest(call, messageCodec, callDescriptor.parameterSerializer)
    val flowId = (metadata!!.messageKind as RemotingMessageKind.CallRequest).callId // TODO hibaell

    val resultFlow = delegator.processCall(callDescriptor.memberId, parameter) as Flow<Any>

    addActiveColdFlow(identifyClient(call), resultFlow, flowId, callDescriptor.flowValueSerializer)

    call.response.status(HttpStatusCode.OK)
}

private suspend fun <M : RawMessage, T : Any> decodeRequest(
    call: ApplicationCall,
    messageCodec: MessageCodec<M>,
    parameterSerializer: KSerializer<T>,
): RemotingMessage<T> {
    val isBinaryCodec = messageCodec.isBinary

    val rawRequestMessage =
        if (isBinaryCodec) {
            RawMessage.Binary(call.receive<ByteArray>().view())
        } else {
            RawMessage.Text(call.receiveText())
        }

    return messageCodec.decodeMessage(
        rawRequestMessage as M,
        parameterSerializer
    )
}

private suspend fun <M : RawMessage> handleSynchronousCall(
    call: ApplicationCall,
    messageCodec: MessageCodec<M>,
    callDescriptor: RemotingMethodDescriptor.SynchronousCall<*, *>,
    delegator: RemoteCallDelegator
) {
    val parameter = decodeRequest(call, messageCodec, callDescriptor.parameterSerializer).payload

    val result = delegator.processCall(callDescriptor.memberId, parameter)

    val responseMessage = RemotingMessage(result, null) // TODO metadata
    val rawResponseMessage = messageCodec.encodeMessage(
        responseMessage,
        callDescriptor.resultSerializer as KSerializer<Any?>
    )

    call.response.status(HttpStatusCode.OK)
    call.response.header(HttpHeaders.ContentType, messageCodec.contentType)

    if (messageCodec.isBinary) {
        call.respondBytes((rawResponseMessage as RawMessage.Binary).byteArrayView.toReadOnlyByteArray())
    } else {
        call.respondText((rawResponseMessage as RawMessage.Text).text)
    }
}
