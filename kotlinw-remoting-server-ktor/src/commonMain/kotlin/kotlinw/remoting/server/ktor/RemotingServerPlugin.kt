package kotlinw.remoting.server.ktor

import arrow.core.continuations.AtomicRef
import arrow.core.continuations.update
import arrow.core.nonFatalOrThrow
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.plugin
import io.ktor.server.request.receive
import io.ktor.server.request.receiveText
import io.ktor.server.request.uri
import io.ktor.server.response.header
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.contentType
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import io.ktor.websocket.DefaultWebSocketSession
import io.ktor.websocket.Frame
import io.ktor.websocket.readBytes
import io.ktor.websocket.readText
import io.ktor.websocket.send
import kotlin.collections.set
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import kotlinw.remoting.api.internal.server.RemoteCallDelegator
import kotlinw.remoting.api.internal.server.RemotingMethodDescriptor
import kotlinw.remoting.api.internal.server.RemotingMethodDescriptor.DownstreamColdFlow
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.RemotingMessage
import kotlinw.remoting.core.RemotingMessageKind
import kotlinw.remoting.core.RemotingMessageMetadata
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.core.codec.MessageCodecWithMetadataPrefetchSupport
import kotlinw.remoting.core.common.BidirectionalMessagingConnection
import kotlinw.remoting.core.common.WebSocketBidirectionalMessagingConnection
import kotlinw.remoting.core.ktor.WebSocketConnection
import kotlinw.util.stdlib.ByteArrayView.Companion.toReadOnlyByteArray
import kotlinw.util.stdlib.ByteArrayView.Companion.view
import kotlinw.util.stdlib.collection.ConcurrentHashMap
import kotlinw.util.stdlib.collection.ConcurrentMutableMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer

private typealias ClientSessionId = Any

enum class ServerToClientCommunicationType {
    WebSockets, ServerSentEvents
}

class RemotingConfiguration {

    var messageCodec: MessageCodec<out RawMessage>? = null

    var remoteCallDelegators: Collection<RemoteCallDelegator>? = null

    var identifyClient: ((ApplicationCall) -> ClientSessionId)? = null

    var supportedServerToClientCommunicationTypes: Set<ServerToClientCommunicationType> = mutableSetOf()
}

private val logger by lazy { PlatformLogging.getLogger() }

val RemotingServerPlugin =
    createApplicationPlugin(
        name = "RemotingPlugin",
        createConfiguration = ::RemotingConfiguration
    ) {
        val identifyClient = requireNotNull(pluginConfig.identifyClient)
        val remoteCallDelegators = requireNotNull(pluginConfig.remoteCallDelegators)
        val messageCodec = requireNotNull(pluginConfig.messageCodec)

        val supportedServerToClientCommunicationTypes = pluginConfig.supportedServerToClientCommunicationTypes
        val isWebSocketSupportRequired =
            supportedServerToClientCommunicationTypes.contains(ServerToClientCommunicationType.WebSockets)
        val isServerSentEventSupportRequired =
            supportedServerToClientCommunicationTypes.contains(ServerToClientCommunicationType.ServerSentEvents)

        val delegators = remoteCallDelegators.associateBy { it.servicePath }
        if (
            delegators.values.flatMap { it.methodDescriptors.values }.filterIsInstance<DownstreamColdFlow<*, *>>().any()
            && supportedServerToClientCommunicationTypes.isEmpty()
        ) {
            throw IllegalStateException("Plugin configuration '${RemotingConfiguration::supportedServerToClientCommunicationTypes.name}' should be specified because downstream communication is required.")
        }

        if (supportedServerToClientCommunicationTypes.isNotEmpty()) {
            if (messageCodec !is MessageCodecWithMetadataPrefetchSupport) {
                throw IllegalStateException("Configuration value '${RemotingConfiguration::messageCodec.name}' must be an instance of '${MessageCodecWithMetadataPrefetchSupport::class.simpleName}'  because server-to-client messaging is requested.")
            }

            if (isWebSocketSupportRequired) {
                application.plugin(WebSockets)
            }

            if (isServerSentEventSupportRequired) {
                TODO()
            }
        }

        val connections: ConcurrentMutableMap<ClientSessionId, WebSocketConnection> = ConcurrentHashMap()

        application.routing {
            route("/remoting") {
                if (isWebSocketSupportRequired) {
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

                if (isServerSentEventSupportRequired) {
                    TODO()
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
            val connection = WebSocketBidirectionalMessagingConnection(this, messageCodec)
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

// TODO utánanézni, hogy ez használható-e itt: createRouteScopedPlugin
