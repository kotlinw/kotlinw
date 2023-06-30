package kotlinw.remoting.api.internal.server

import kotlinx.serialization.KSerializer

sealed interface RemotingMethodDescriptor {

    val memberId: String

    data class SynchronousCall<P : Any, R>(
        override val memberId: String,
        val parameterSerializer: KSerializer<P>,
        val resultSerializer: KSerializer<R>
    ) : RemotingMethodDescriptor

    data class DownstreamColdFlow<P : Any, F>(
        override val memberId: String,
        val parameterSerializer: KSerializer<P>,
        val flowValueSerializer: KSerializer<F>
    ) : RemotingMethodDescriptor
}
