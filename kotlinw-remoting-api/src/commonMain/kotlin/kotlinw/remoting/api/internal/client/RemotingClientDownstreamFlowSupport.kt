package kotlinw.remoting.api.internal.client

import kotlinw.remoting.api.client.RemotingClient
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

interface RemotingClientDownstreamFlowSupport : RemotingClient {

    suspend fun <T : Any, P : Any, F : Any> requestDownstreamColdFlow(
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
