package kotlinw.remoting.server.ktor

import arrow.core.nonFatalOrThrow
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import kotlinw.remoting.api.internal.server.RemoteCallDelegator
import kotlinw.remoting.api.internal.server.RemotingMethodDescriptor
import kotlinw.remoting.api.internal.server.RemotingMethodDescriptor.DownstreamColdFlow
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.RemotingMessage
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.core.codec.MessageCodecWithMetadataPrefetchSupport
import kotlinw.remoting.core.common.BidirectionalMessagingManager
import kotlinw.remoting.core.common.BidirectionalMessagingManagerImpl
import kotlinw.remoting.core.ktor.WebSocketBidirectionalMessagingConnection
import kotlinw.util.stdlib.ByteArrayView.Companion.toReadOnlyByteArray
import kotlinw.util.stdlib.ByteArrayView.Companion.view
import kotlinw.util.stdlib.collection.ConcurrentHashMap
import kotlinw.util.stdlib.collection.ConcurrentMutableMap
import kotlinw.uuid.Uuid
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer
import xyz.kotlinw.remoting.api.MessagingPeerId
import xyz.kotlinw.remoting.api.MessagingSessionId

enum class ServerToClientCommunicationType {
    WebSockets, ServerSentEvents
}

class RemotingConfiguration {

    var messageCodec: MessageCodec<out RawMessage>? = null

    var remoteCallDelegators: Collection<RemoteCallDelegator>? = null

    var identifyClient: ((ApplicationCall) -> MessagingPeerId)? = null

    var supportedServerToClientCommunicationTypes: Set<ServerToClientCommunicationType> = mutableSetOf()

    var onConnectionAdded: ((MessagingPeerId, MessagingSessionId, BidirectionalMessagingManager) -> Unit)? = null

    var onConnectionRemoved: ((MessagingPeerId, MessagingSessionId) -> Unit)? = null

    var authenticationProviderName: String? = null
}

private val logger by lazy { PlatformLogging.getLogger() }

private const val RemotingServerPluginName = "RemotingServer"

val RemotingServerPlugin =
    createApplicationPlugin(
        name = RemotingServerPluginName,
        createConfiguration = ::RemotingConfiguration
    ) {
        val authenticationProviderName: String? = pluginConfig.authenticationProviderName
        if (authenticationProviderName != null && application.pluginOrNull(Authentication) == null) {
            throw IllegalStateException("Ktor server plugin '${Authentication.key.name} must be installed if ${RemotingConfiguration::class.simpleName}.${RemotingConfiguration::authenticationProviderName.name} is specified.")
        }

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
                if (application.pluginOrNull(WebSockets) == null) {
                    throw IllegalStateException("Install required Ktor plugin: '${WebSockets.key.name}' (required by '$RemotingServerPluginName')")
                }
            }

            if (isServerSentEventSupportRequired) {
                TODO()
            }
        }

        val connections: ConcurrentMutableMap<MessagingSessionId, BidirectionalMessagingManager> = ConcurrentHashMap()

        fun addConnection(
            peerId: MessagingPeerId,
            sessionId: MessagingSessionId,
            messagingManager: BidirectionalMessagingManager
        ) {
            connections.compute(sessionId) { key, previousValue ->
                check(previousValue == null) { "Session is already connected: $key" }
                messagingManager
            }

            try {
                pluginConfig.onConnectionAdded?.invoke(peerId, sessionId, messagingManager)
            } catch (e: Throwable) {
                logger.warning(e.nonFatalOrThrow()) { "onConnectionAdded() has thrown an exception." }
            }
        }

        fun removeConnection(sessionId: MessagingSessionId) {
            val messagingManager = connections.remove(sessionId)

            if (messagingManager != null) {
                try {
                    pluginConfig.onConnectionRemoved?.invoke(messagingManager.remotePeerId, sessionId)
                } catch (e: Throwable) {
                    logger.warning(e.nonFatalOrThrow()) { "onConnectionAdded() has thrown an exception." }
                }
            } else {
                logger.warning { "Tried to remove non-existing connection: " / ("sessionId" to sessionId) }
            }
        }

        application.routing {

            fun Route.configureRouting() {
                if (isWebSocketSupportRequired) {
                    setupWebsocketRouting(
                        messageCodec as MessageCodecWithMetadataPrefetchSupport<RawMessage>, // TODO check?
                        delegators,
                        identifyClient,
                        ::addConnection,
                        ::removeConnection
                    )
                }

                if (isServerSentEventSupportRequired) {
                    TODO()
                }

                setupRemoteCallRouting(
                    messageCodec,
                    delegators
                )
            }

            route("/remoting") {
                if (authenticationProviderName != null) {
                    authenticate(authenticationProviderName) {
                        configureRouting()
                    }
                } else {
                    configureRouting()
                }
            }
        }
    }

private fun Route.setupWebsocketRouting(
    messageCodec: MessageCodecWithMetadataPrefetchSupport<RawMessage>,
    delegators: Map<String, RemoteCallDelegator>,
    identifyClient: (ApplicationCall) -> MessagingPeerId,
    addConnection: (MessagingPeerId, MessagingSessionId, BidirectionalMessagingManager) -> Unit,
    removeConnection: (MessagingSessionId) -> Unit
) {

    // TODO fix string
    webSocket("/websocket") {
        val messagingPeerId = identifyClient(call) // TODO hibaell.
        val messagingSessionId: MessagingSessionId = Uuid.randomUuid().toString()

        try {
            val connection =
                WebSocketBidirectionalMessagingConnection(messagingPeerId, messagingSessionId, this, messageCodec)
            val sessionMessagingManager = BidirectionalMessagingManagerImpl(connection, messageCodec, delegators)
            addConnection(messagingPeerId, messagingSessionId, sessionMessagingManager)
            sessionMessagingManager.processIncomingMessages()
        } catch (e: ClosedReceiveChannelException) {
            val closeReason = closeReason.await()
            logger.info { "Connection closed, reason: " / closeReason }
        } catch (e: Throwable) {
            logger.error(e.nonFatalOrThrow()) { "Disconnected." }
        } finally {
            removeConnection(messagingSessionId)
        }
    }
}

private fun Route.setupRemoteCallRouting(
    messageCodec: MessageCodec<out RawMessage>,
    remoteCallDelegators: Map<String, RemoteCallDelegator>
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
                                    is RemotingMethodDescriptor.DownstreamColdFlow<*, *> -> {
                                        logger.warning { "Remoting methods with ${Flow::class.simpleName} return types are not supported by this endpoint." }
                                        call.response.status(HttpStatusCode.BadRequest)
                                    }

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

private suspend fun <M : RawMessage> handleSynchronousCall(
    call: ApplicationCall,
    messageCodec: MessageCodec<M>,
    callDescriptor: RemotingMethodDescriptor.SynchronousCall<*, *>,
    delegator: RemoteCallDelegator
) {
    val isBinaryCodec = messageCodec.isBinary

    val rawRequestMessage =
        if (isBinaryCodec) {
            RawMessage.Binary(call.receive<ByteArray>().view())
        } else {
            RawMessage.Text(call.receiveText())
        }

    val parameter = messageCodec.decodeMessage(
        rawRequestMessage as M,
        callDescriptor.parameterSerializer
    ).payload

    val result = delegator.processCall(callDescriptor.memberId, parameter)

    val responseMessage = RemotingMessage(result, null) // TODO metadata
    val rawResponseMessage = messageCodec.encodeMessage(
        responseMessage,
        callDescriptor.resultSerializer as KSerializer<Any?>
    )

    call.response.status(HttpStatusCode.OK)
    call.response.header(HttpHeaders.ContentType, messageCodec.contentType)

    if (isBinaryCodec) {
        check(rawResponseMessage is RawMessage.Binary)
        call.respondBytes(rawResponseMessage.byteArrayView.toReadOnlyByteArray())
    } else {
        check(rawResponseMessage is RawMessage.Text)
        call.respondText(rawResponseMessage.text)
    }
}

// TODO utánanézni, hogy ez használható-e itt: createRouteScopedPlugin
