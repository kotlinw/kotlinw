package xyz.kotlinw.module.remoting.server

import io.ktor.server.application.install
import kotlinw.eventbus.local.LocalEventBus
import kotlinw.eventbus.local.dispatch
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.core.common.MutableRemotePeerRegistry
import kotlinw.remoting.core.common.MutableRemotePeerRegistryImpl
import kotlinw.remoting.core.common.RemoteConnectionData
import kotlinw.remoting.core.common.RemoteConnectionId
import kotlinw.remoting.core.common.RemotePeerRegistry
import kotlinw.remoting.server.ktor.RemotingConfiguration
import kotlinw.remoting.server.ktor.RemotingServerPlugin
import kotlinw.util.stdlib.Priority
import kotlinw.util.stdlib.Priority.Companion.lowerBy
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.module.ktor.server.KtorServerApplicationConfigurer

@Module
class ServerRemotingModule {

    @Component
    fun remotePeerRegistry(): RemotePeerRegistry = MutableRemotePeerRegistryImpl()

    @Component
    fun remoteCallHandlersBinder(
        remotingConfigurations: List<RemotingConfiguration>,
        defaultMessageCodec: MessageCodec<*>?
    ): KtorServerApplicationConfigurer {
        return KtorServerApplicationConfigurer(Priority.Normal.lowerBy(10)) {
            application.install(RemotingServerPlugin) {
                this.defaultMessageCodec = defaultMessageCodec
                this.remotingConfigurations = remotingConfigurations
                this.ktorServerCoroutineScope = this@KtorServerApplicationConfigurer.ktorServerCoroutineScope
            }
        }
    }
}

// TODO
// require(remotePeerRegistry is MutableRemotePeerRegistry)
//this.onConnectionAdded = { peerId, sessionId, messagingManager ->
//    remotePeerRegistry.addConnection(
//        RemoteConnectionId(peerId, sessionId),
//        RemoteConnectionData(messagingManager)
//    )
//    eventBus.dispatch(
//        ktorServerCoroutineScope,
//        MessagingPeerConnectedEvent(peerId, sessionId)
//    )
//}
//this.onConnectionRemoved = { peerId, sessionId ->
//    remotePeerRegistry.removeConnection(RemoteConnectionId(peerId, sessionId))
//    eventBus.dispatch(
//        ktorServerCoroutineScope,
//        MessagingPeerDisconnectedEvent(peerId, sessionId)
//    )
//}
