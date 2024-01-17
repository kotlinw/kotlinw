package kotlinw.remoting.server.ktor

import arrow.core.nonFatalOrThrow
import io.ktor.server.application.install
import io.ktor.server.application.pluginOrNull
import io.ktor.server.auth.Principal
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.codec.MessageCodecWithMetadataPrefetchSupport
import kotlinw.remoting.core.common.BidirectionalMessagingManager
import kotlinw.remoting.core.common.BidirectionalMessagingManagerImpl
import kotlinw.remoting.core.common.DelegatingRemotingClient
import kotlinw.remoting.core.common.NewConnectionData
import kotlinw.remoting.core.common.RemoteConnectionId
import kotlinw.remoting.core.common.RemovedConnectionData
import kotlinw.remoting.core.ktor.WebSocketBidirectionalMessagingConnection
import kotlinw.remoting.server.ktor.RemotingProvider.InstallationContext
import kotlinw.util.stdlib.collection.ConcurrentHashMap
import kotlinw.util.stdlib.collection.ConcurrentMutableMap
import kotlinw.util.stdlib.infiniteLoop
import kotlinw.uuid.Uuid
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import xyz.kotlinw.remoting.api.MessagingConnectionId
import xyz.kotlinw.remoting.api.internal.RemoteCallHandlerImplementor

class WebSocketRemotingProvider(
    loggerFactory: LoggerFactory,
    private val onConnectionAdded: ((NewConnectionData) -> Unit)? = null,
    private val onConnectionRemoved: ((RemovedConnectionData) -> Unit)? = null
) : RemotingProvider {

    private val logger = loggerFactory.getLogger()

    override fun InstallationContext.install() {
        require(messageCodec is MessageCodecWithMetadataPrefetchSupport) {
            "Message codec should be an instance of ${MessageCodecWithMetadataPrefetchSupport::class} but got: $messageCodec"
        }

        if (ktorApplication.pluginOrNull(WebSockets) == null) {
            logger.warning { "Installing ktor-server plugin '${WebSockets.key.name}' with default settings." }
            ktorApplication.install(WebSockets)
        }

        val webSocketRemotingConfiguration =
            remotingConfiguration as? WebSocketRemotingConfiguration ?: throw AssertionError()

        val connections: ConcurrentMutableMap<MessagingConnectionId, BidirectionalMessagingManager> =
            ConcurrentHashMap()

        fun addConnection(
            remoteConnectionId: RemoteConnectionId,
            principal: Principal?,
            messagingManager: BidirectionalMessagingManager
        ) {
            connections.compute(remoteConnectionId.connectionId) { key, previousValue ->
                check(previousValue == null) { "Connection already exists: $key" }
                messagingManager
            }

            if (onConnectionAdded != null || webSocketRemotingConfiguration.onConnectionAdded != null) {
                val reverseRemotingClient = DelegatingRemotingClient(messagingManager)
                val newConnectionData =
                    NewConnectionData(remoteConnectionId, principal, reverseRemotingClient, messagingManager)

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
                    val removedConnectionData =
                        RemovedConnectionData(messagingManager.remoteConnectionId, messagingManager.principal)

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
            (remotingConfiguration.remoteCallHandlers as Iterable<RemoteCallHandlerImplementor<*>>).associateBy { it.serviceId }

        ktorApplication.routing {

            fun Route.configureRouting() {
                // TODO fix path
                webSocket("/websocket/${webSocketRemotingConfiguration.id}") {
                    val principal = remotingConfiguration.extractPrincipal(call)
                    val messagingPeerId = remotingConfiguration.identifyClient(call, principal) // TODO hibaell.
                    val messagingConnectionId: MessagingConnectionId = Uuid.randomUuid().toString() // TODO customizable
                    val remoteConnectionId = RemoteConnectionId(messagingPeerId, messagingConnectionId)

                    logger.debug {
                        "Connected WS client: " /
                                mapOf("principal" to principal, "remoteConnectionId" to remoteConnectionId)
                    }

                    val connection =
                        WebSocketBidirectionalMessagingConnection(
                            remoteConnectionId,
                            this,
                            messageCodec as MessageCodecWithMetadataPrefetchSupport<RawMessage> // TODO check?
                        )

                    try {
                        val sessionMessagingManager =
                            BidirectionalMessagingManagerImpl(
                                connection,
                                messageCodec as MessageCodecWithMetadataPrefetchSupport<RawMessage>,
                                delegators,
                                principal
                            )
                        addConnection(remoteConnectionId, principal, sessionMessagingManager)
                        sessionMessagingManager.processIncomingMessages()
                    } catch (e: ClosedReceiveChannelException) {
                        val closeReason = closeReason.await()
                        logger.debug { "Connection closed, reason: " / closeReason }
                        throw e
                    } catch (e: Throwable) {
                        logger.error(e.nonFatalOrThrow()) { "Disconnected: " / remoteConnectionId }
                        throw e
                    } finally {
                        cancel() // Explicitly cancel the coroutine scope of the WebSocket connection (bug?)
                        removeConnection(messagingConnectionId)
                    }
                }
            }

            route("/remoting") {
                if (remotingConfiguration.authenticationProviderName != null) {
                    logger.info { "Remote call handlers (authorization by '${webSocketRemotingConfiguration.authenticationProviderName}'): " / delegators.mapValues { it.value.serviceId } }
                    authenticate(remotingConfiguration.authenticationProviderName) {
                        configureRouting()
                    }
                } else {
                    logger.info { "Remote call handlers: " / delegators.mapValues { it.value.serviceId } }
                    configureRouting()
                }
            }
        }
    }
}
