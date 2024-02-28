package kotlinw.remoting.server.ktor

import arrow.core.nonFatalOrThrow
import io.ktor.server.application.pluginOrNull
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.server.websocket.WebSockets
import io.ktor.server.websocket.webSocket
import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.api.withLoggingContext
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.codec.MessageCodecWithMetadataPrefetchSupport
import kotlinw.remoting.core.common.BidirectionalMessagingManager
import kotlinw.remoting.core.common.BidirectionalMessagingManagerImpl
import kotlinw.remoting.core.common.DelegatingRemotingClient
import kotlinw.remoting.core.common.NewConnectionData
import kotlinw.remoting.core.common.RemovedConnectionData
import kotlinw.remoting.core.ktor.SingleSessionBidirectionalWebSocketConnection
import kotlinw.remoting.server.ktor.RemotingProvider.InstallationContext
import kotlinw.util.stdlib.collection.ConcurrentHashMap
import kotlinw.util.stdlib.collection.ConcurrentMutableMap
import kotlinx.coroutines.CoroutineStart.UNDISPATCHED
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import xyz.kotlinw.remoting.api.MessagingConnectionId
import xyz.kotlinw.remoting.api.RemoteConnectionId
import xyz.kotlinw.remoting.api.internal.RemoteCallHandlerImplementor

class WebSocketRemotingProvider(
    loggerFactory: LoggerFactory,
    private val onConnectionAdded: (suspend (NewConnectionData) -> Unit)? = null,
    private val onConnectionRemoved: (suspend (RemovedConnectionData) -> Unit)? = null
) : RemotingProvider {

    private val logger = loggerFactory.getLogger()

    override fun InstallationContext.install() {
        require(messageCodec is MessageCodecWithMetadataPrefetchSupport) {
            "Message codec should be an instance of ${MessageCodecWithMetadataPrefetchSupport::class} but got: $messageCodec"
        }

        if (ktorApplication.pluginOrNull(WebSockets) == null) {
            logger.info { "Installing Ktor server plugin '${WebSockets.key.name}' with default settings." }
            ktorApplication.installServerWebSockets()
        }

        val webSocketRemotingConfiguration =
            remotingConfiguration as? WebSocketRemotingConfiguration ?: throw AssertionError()

        val connections: ConcurrentMutableMap<MessagingConnectionId, BidirectionalMessagingManager> =
            ConcurrentHashMap()

        suspend fun addConnection(
            remoteConnectionId: RemoteConnectionId,
            messagingManager: BidirectionalMessagingManager
        ) {
            connections.compute(remoteConnectionId.connectionId) { key, previousValue ->
                check(previousValue == null) { "Connection already exists: $key" }
                messagingManager
            }

            if (onConnectionAdded != null || webSocketRemotingConfiguration.onConnectionAdded != null) {
                val reverseRemotingClient = DelegatingRemotingClient(messagingManager)
                val newConnectionData =
                    NewConnectionData(remoteConnectionId, reverseRemotingClient, messagingManager)

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

        suspend fun removeConnection(sessionId: MessagingConnectionId) {
            val messagingManager = connections.remove(sessionId)

            if (messagingManager != null) {
                if (onConnectionRemoved != null || webSocketRemotingConfiguration.onConnectionRemoved != null) {
                    val removedConnectionData =
                        RemovedConnectionData(messagingManager.remoteConnectionId)

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
                    val messagingConnectionId: MessagingConnectionId =
                        remotingConfiguration.identifyConnection(call) // TODO hibaell.
                    val remoteConnectionId = RemoteConnectionId(messagingPeerId, messagingConnectionId)

                    // TODO ezt a WebRequestRemotingProvider-be is
                    logger.withLoggingContext(mapOf("kotlinw.principal" to principal.toString())) {
                        logger.debug { "Connected WS client: " / mapOf("remoteConnectionId" to remoteConnectionId) }

                        val connection =
                            SingleSessionBidirectionalWebSocketConnection(
                                remoteConnectionId,
                                this,
                                messageCodec as MessageCodecWithMetadataPrefetchSupport<RawMessage> // TODO check?
                            )

                        try {
                            val messagingManager =
                                BidirectionalMessagingManagerImpl(
                                    connection,
                                    messageCodec as MessageCodecWithMetadataPrefetchSupport<RawMessage>,
                                    delegators
                                )

                            coroutineScope {
                                // Start incoming message processing before calling "onConnect" handlers
                                // to allow the usage of the reverse remoting client in those handlers
                                launch(start = UNDISPATCHED) {
                                    messagingManager.processIncomingMessages()
                                }

                                addConnection(remoteConnectionId, messagingManager)
                            }
                        } catch (e: ClosedReceiveChannelException) {
                            val closeReason = closeReason.await()
                            logger.debug { "Connection closed, reason: " / closeReason }
                            throw e
                        } catch (e: Throwable) {
                            logger.error(e.nonFatalOrThrow()) { "Disconnected: " / remoteConnectionId }
                            throw e
                        } finally {
                            cancel() // TODO https://youtrack.jetbrains.com/issue/KTOR-4110
                            removeConnection(messagingConnectionId)
                        }
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
