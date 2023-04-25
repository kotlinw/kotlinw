package kotlinw.remoting.server.core

import kotlinx.serialization.KSerializer

data class RemotingMethodDescriptor<P: Any, R: Any>(
    val methodId: String,
    val parameterSerializer: KSerializer<out P>,
    val resultSerializer: KSerializer<out  R>
)
