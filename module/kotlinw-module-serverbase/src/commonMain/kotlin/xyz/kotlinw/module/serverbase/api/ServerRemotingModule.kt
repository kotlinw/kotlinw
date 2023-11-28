package xyz.kotlinw.module.serverbase.api

import io.ktor.server.application.install
import kotlinw.eventbus.local.LocalEventBus
import kotlinw.eventbus.local.dispatch
import xyz.kotlinw.module.ktor.server.KtorServerApplicationConfigurer
import kotlinw.module.serverbase.MessagingPeerConnectedEvent
import kotlinw.module.serverbase.MessagingPeerDisconnectedEvent
import kotlinw.remoting.api.internal.server.RemoteCallHandler
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.core.common.RemoteConnectionData
import kotlinw.remoting.core.common.RemoteConnectionId
import kotlinw.remoting.core.common.RemotePeerRegistryImpl
import kotlinw.remoting.server.ktor.RemotingServerPlugin
import kotlinw.remoting.server.ktor.ServerToClientCommunicationType.WebSockets
import kotlinw.util.stdlib.Priority
import kotlinw.util.stdlib.Priority.Companion.lowerBy
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module

@Module
class ServerRemotingModule {

    @Component
    fun remoteCallHandlersBinder(
        remoteCallHandlers: List<RemoteCallHandler>,
        eventBus: LocalEventBus,
        messageCodec: MessageCodec<*>
    ) =
        KtorServerApplicationConfigurer(Priority.Normal.lowerBy(10)) {
            val ktorApplication = application

            if (remoteCallHandlers.isNotEmpty()) {
                ktorApplication.install(RemotingServerPlugin) {
                    val remotePeerRegistry = RemotePeerRegistryImpl() // TODO

                    this.messageCodec = messageCodec
                    this.remoteCallHandlers = remoteCallHandlers
                    this.identifyClient = { 1 } // FIXME
                    this.supportedServerToClientCommunicationTypes =
                        setOf(WebSockets) // TODO configurable
                    this.onConnectionAdded = { peerId, sessionId, messagingManager ->
                        remotePeerRegistry.addConnection(
                            RemoteConnectionId(peerId, sessionId),
                            RemoteConnectionData(messagingManager)
                        )
                        eventBus.dispatch(
                            ktorServerCoroutineScope,
                            MessagingPeerConnectedEvent(peerId, sessionId)
                        )
                    }
                    this.onConnectionRemoved = { peerId, sessionId ->
                        remotePeerRegistry.removeConnection(RemoteConnectionId(peerId, sessionId))
                        eventBus.dispatch(
                            ktorServerCoroutineScope,
                            MessagingPeerDisconnectedEvent(peerId, sessionId)
                        )
                    }
                }
            }
        }
}
