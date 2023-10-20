package kotlinw.koin.core.api

import kotlinw.remoting.api.client.RemotingClient
import kotlinw.remoting.api.internal.server.RemoteCallHandler
import kotlinw.remoting.core.client.HttpRemotingClient
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.core.common.MutableRemotePeerRegistry
import kotlinw.remoting.core.common.RemotePeerRegistryImpl
import kotlinw.remoting.core.common.SynchronousCallSupport
import kotlinw.util.stdlib.Url

interface RemotingClientFactory {

    fun createRemotingClient(
        remoteServerBaseUrl: Url,
        incomingCallDelegators: Map<String, RemoteCallHandler> = emptyMap(),
        synchronousCallSupportImplementor: SynchronousCallSupport? = null,
        remotePeerRegistry: MutableRemotePeerRegistry = RemotePeerRegistryImpl(),
        messageCodec: MessageCodec<*>? = null
    ): RemotingClient
}

class RemotingClientFactoryImpl(
    private val defaultMessageCodec: MessageCodec<*>?,
    private val defaultSynchronousCallSupportImplementor: SynchronousCallSupport?
) : RemotingClientFactory {

    override fun createRemotingClient(
        remoteServerBaseUrl: Url,
        incomingCallDelegators: Map<String, RemoteCallHandler>,
        synchronousCallSupportImplementor: SynchronousCallSupport?,
        remotePeerRegistry: MutableRemotePeerRegistry,
        messageCodec: MessageCodec<*>?
    ): RemotingClient =
        HttpRemotingClient(
            messageCodec
                ?: defaultMessageCodec
                ?: throw IllegalStateException(), // TODO hibaüz.
            synchronousCallSupportImplementor
                ?: defaultSynchronousCallSupportImplementor
                ?: throw IllegalStateException(), // TODO hibaüz.
            remotePeerRegistry,
            remoteServerBaseUrl,
            incomingCallDelegators
        )
}
