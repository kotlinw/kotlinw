package kotlinw.remoting.server.core

import kotlinx.serialization.KSerializer

sealed interface RemotingMethodDescriptor {

    val memberId: String

    data class DownstreamSharedFlow<F: Any>(
        override val memberId: String,
        val flowValueSerializer: KSerializer<out  F>
    ): RemotingMethodDescriptor

    data class SynchronousCall<P: Any, R: Any>(
        override val memberId: String,
        val parameterSerializer: KSerializer<out P>,
        val resultSerializer: KSerializer<out  R>
    ): RemotingMethodDescriptor

    data class DownstreamColdFlow<P: Any, F:Any>(
        override val memberId: String,
        val parameterSerializer: KSerializer<out P>,
        val flowValueSerializer: KSerializer< F>
    ): RemotingMethodDescriptor
}
