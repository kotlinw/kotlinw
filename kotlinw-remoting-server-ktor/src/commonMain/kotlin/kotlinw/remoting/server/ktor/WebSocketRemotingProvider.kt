package kotlinw.remoting.server.ktor

import arrow.core.nonFatalOrThrow
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.application.pluginOrNull
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.codec.MessageCodecWithMetadataPrefetchSupport
import kotlinw.remoting.core.common.BidirectionalMessagingManager
import kotlinw.remoting.core.common.BidirectionalMessagingManagerImpl
import kotlinw.remoting.core.common.DelegatingRemotingClient
import kotlinw.remoting.core.common.NewConnectionData
import kotlinw.remoting.core.common.RemovedConnectionData
import kotlinw.remoting.core.ktor.WebSocketBidirectionalMessagingConnection
import kotlinw.remoting.server.ktor.RemotingProvider.InstallationContext
import kotlinw.util.stdlib.collection.ConcurrentHashMap
import kotlinw.util.stdlib.collection.ConcurrentMutableMap
import kotlinw.uuid.Uuid
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import xyz.kotlinw.remoting.api.MessagingPeerId
import xyz.kotlinw.remoting.api.MessagingConnectionId
import xyz.kotlinw.remoting.api.internal.RemoteCallHandlerImplementor

private val logger by lazy { PlatformLogging.getLogger() }

class WebSocketRemotingProvider(
    private val identifyClient: (ApplicationCall) -> MessagingPeerId,
    private val onConnectionAdded: ((NewConnectionData) -> Unit)? = null,
    private val onConnectionRemoved: ((RemovedConnectionData) -> Unit)? = null
) : RemotingProvider {

    override fun InstallationContext.install() {
        require(messageCodec is MessageCodecWithMetadataPrefetchSupport) {
            "Message codec should be an instance of ${MessageCodecWithMetadataPrefetchSupport::class} but got: $messageCodec"
        }

        if (ktorApplication.pluginOrNull(WebSockets) == null) {
            logger.warning { "Installing Ktor server plugin ${WebSockets.key.name} with default settings." }
            ktorApplication.install(WebSockets)
        }

        val webSocketRemotingConfiguration =
            remotingConfiguration as? WebSocketRemotingConfiguration ?: throw AssertionError()

        val connections: ConcurrentMutableMap<MessagingConnectionId, BidirectionalMessagingManager> = ConcurrentHashMap()

        fun addConnection(
            peerId: MessagingPeerId,
            sessionId: MessagingConnectionId,
            messagingManager: BidirectionalMessagingManager
        ) {
            connections.compute(sessionId) { key, previousValue ->
                check(previousValue == null) { "Session is already connected: $key" }
                messagingManager
            }

            if (onConnectionAdded != null || webSocketRemotingConfiguration.onConnectionAdded != null) {
                val reverseRemotingClient = DelegatingRemotingClient(messagingManager)
                val newConnectionData =
                    NewConnectionData(peerId, sessionId, reverseRemotingClient, messagingManager)

                try {
                    onConnectionAdded?.invoke(newConnectionData)
                } catch (e: Throwable) {
                    logger.error(e.nonFatalOrThrow()) { "onConnectionAdded() has thrown an exception." }
                }

                try {
                    webSocketRemotingConfiguration.onConnectionAdded?.invoke(newConnectionData)
                } catch (e: Throwable) {
                    logger.warning(e.nonFatalOrThrow()) { "onConnectionAdded() has thrown an exception." }
                }
            }
        }

        fun removeConnection(sessionId: MessagingConnectionId) {
            val messagingManager = connections.remove(sessionId)

            if (messagingManager != null) {
                if (onConnectionRemoved != null || webSocketRemotingConfiguration.onConnectionRemoved != null) {
                    val removedConnectionData = RemovedConnectionData(messagingManager.remotePeerId, sessionId)

                    try {
                        onConnectionRemoved?.invoke(removedConnectionData)
                    } catch (e: Throwable) {
                        logger.warning(e.nonFatalOrThrow()) { "onConnectionRemoved() has thrown an exception." }
                    }

                    try {
                        webSocketRemotingConfiguration.onConnectionRemoved?.invoke(removedConnectionData)
                    } catch (e: Throwable) {
                        logger.warning(e.nonFatalOrThrow()) { "onConnectionRemoved() has thrown an exception." }
                    }
                }
            } else {
                logger.warning { "Tried to remove non-existing connection: " / ("sessionId" to sessionId) }
            }
        }

        val delegators =
            (remotingConfiguration.remoteCallHandlers as Iterable<RemoteCallHandlerImplementor>).associateBy { it.serviceId }

        ktorApplication.routing {

            fun Route.configureRouting() {
                setupWebsocketRouting(
                    messageCodec as MessageCodecWithMetadataPrefetchSupport<RawMessage>, // TODO check?
                    delegators,
                    identifyClient,
                    ::addConnection,
                    ::removeConnection
                )
            }

            route("/remoting") {
                if (remotingConfiguration.authenticationProviderName != null) {
                    authenticate(remotingConfiguration.authenticationProviderName) {
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
        delegators: Map<String, RemoteCallHandlerImplementor>,
        identifyClient: (ApplicationCall) -> MessagingPeerId,
        addConnection: suspend (MessagingPeerId, MessagingConnectionId, BidirectionalMessagingManager) -> Unit,
        removeConnection: suspend (MessagingConnectionId) -> Unit
    ) {

        // TODO fix string
        webSocket("/websocket") {
            val messagingPeerId = identifyClient(call) // TODO hibaell.
            val messagingConnectionId: MessagingConnectionId = Uuid.randomUuid().toString() // TODO customizable

            try {
                val connection =
                    WebSocketBidirectionalMessagingConnection(messagingPeerId, messagingConnectionId, this, messageCodec)
                val sessionMessagingManager = BidirectionalMessagingManagerImpl(connection, messageCodec, delegators)
                addConnection(messagingPeerId, messagingConnectionId, sessionMessagingManager)
                sessionMessagingManager.processIncomingMessages()
            } catch (e: ClosedReceiveChannelException) {
                val closeReason = closeReason.await()
                logger.info { "Connection closed, reason: " / closeReason }
            } catch (e: Throwable) {
                logger.error(e.nonFatalOrThrow()) { "Disconnected." }
            } finally {
                removeConnection(messagingConnectionId)
            }
        }
    }
}
