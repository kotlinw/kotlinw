package kotlinw.koin.core.api

import kotlinw.remoting.api.client.RemotingClient
import kotlinw.remoting.api.internal.server.RemoteCallDelegator
import kotlinw.remoting.core.client.HttpRemotingClient
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.core.common.MutableRemotePeerRegistry
import kotlinw.remoting.core.common.RemotePeerRegistryImpl
import kotlinw.remoting.core.common.SynchronousCallSupport
import kotlinw.util.stdlib.Url

interface RemotingClientFactory {

    fun createRemotingClient(
        remoteServerBaseUrl: Url,
        synchronousCallSupportImplementor: SynchronousCallSupport,
        remotePeerRegistry: MutableRemotePeerRegistry = RemotePeerRegistryImpl(),
        incomingCallDelegators: Map<String, RemoteCallDelegator> = emptyMap(),
        messageCodec: MessageCodec<*>? = null
    ): RemotingClient
}

class RemotingClientFactoryImpl(
    private val defaultMessageCodec: MessageCodec<*>?
) : RemotingClientFactory {

    override fun createRemotingClient(
        remoteServerBaseUrl: Url,
        synchronousCallSupportImplementor: SynchronousCallSupport,
        remotePeerRegistry: MutableRemotePeerRegistry,
        incomingCallDelegators: Map<String, RemoteCallDelegator>,
        messageCodec: MessageCodec<*>?
    ): RemotingClient =
        HttpRemotingClient(
            (messageCodec ?: defaultMessageCodec) ?: throw IllegalStateException(),
            synchronousCallSupportImplementor,
            remotePeerRegistry,
            remoteServerBaseUrl,
            incomingCallDelegators
        )
}
