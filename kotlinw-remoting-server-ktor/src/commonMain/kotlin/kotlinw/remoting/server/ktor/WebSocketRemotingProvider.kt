package kotlinw.remoting.server.ktor

import arrow.core.nonFatalOrThrow
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.authenticate
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import kotlinw.remoting.api.internal.server.RemoteCallHandler
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.codec.MessageCodecWithMetadataPrefetchSupport
import kotlinw.remoting.core.common.BidirectionalMessagingManager
import kotlinw.remoting.core.common.BidirectionalMessagingManagerImpl
import kotlinw.remoting.core.ktor.WebSocketBidirectionalMessagingConnection
import kotlinw.remoting.server.ktor.RemotingProvider.InstallationContext
import kotlinw.util.stdlib.collection.ConcurrentHashMap
import kotlinw.util.stdlib.collection.ConcurrentMutableMap
import kotlinw.uuid.Uuid
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import xyz.kotlinw.remoting.api.MessagingPeerId
import xyz.kotlinw.remoting.api.MessagingSessionId

private val logger by lazy { PlatformLogging.getLogger() }

class WebSocketRemotingProvider(
    private val identifyClient: (ApplicationCall) -> MessagingPeerId,
    private val onConnectionAdded: ((MessagingPeerId, MessagingSessionId, BidirectionalMessagingManager) -> Unit)? = null,
    private val onConnectionRemoved: ((MessagingPeerId, MessagingSessionId) -> Unit)? = null
) : RemotingProvider {

    override fun InstallationContext.install() {
        require(messageCodec is MessageCodecWithMetadataPrefetchSupport) {
            "Message codec should be an instance of ${MessageCodecWithMetadataPrefetchSupport::class} but got: $messageCodec"
        }

        if (ktorApplication.pluginOrNull(WebSockets) == null) {
            logger.warning { "Installing Ktor server plugin ${WebSockets.key.name} with default settings." }
            ktorApplication.install(WebSockets)
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
                onConnectionAdded?.invoke(peerId, sessionId, messagingManager)
            } catch (e: Throwable) {
                logger.warning(e.nonFatalOrThrow()) { "onConnectionAdded() has thrown an exception." }
            }
        }

        fun removeConnection(sessionId: MessagingSessionId) {
            val messagingManager = connections.remove(sessionId)

            if (messagingManager != null) {
                try {
                    onConnectionRemoved?.invoke(messagingManager.remotePeerId, sessionId)
                } catch (e: Throwable) {
                    logger.warning(e.nonFatalOrThrow()) { "onConnectionAdded() has thrown an exception." }
                }
            } else {
                logger.warning { "Tried to remove non-existing connection: " / ("sessionId" to sessionId) }
            }
        }

        val delegators = remoteCallHandlers.associateBy { it.servicePath }

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
        delegators: Map<String, RemoteCallHandler>,
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
}