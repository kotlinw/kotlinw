package xyz.kotlinw.remoting.api.internal

import xyz.kotlinw.remoting.api.RemotingClient
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

interface RemotingClientFlowSupport : RemotingClient {

    suspend fun <T : Any, P : Any, F> requestIncomingColdFlow(
        serviceKClass: KClass<T>,
        methodKFunction: KFunction<Flow<F>>,
        serviceId: String,
        methodId: String,
        parameter: P,
        parameterSerializer: KSerializer<P>,
        flowValueDeserializer: KSerializer<F>,
        callId: String
    ): Flow<F>
}
