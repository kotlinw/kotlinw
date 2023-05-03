package kotlinw.remoting.client.core

import kotlinw.remoting.api.client.RemotingClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KProperty1

interface RemotingClientDownstreamFlowSupport : RemotingClient {

    fun <T : Any, F : Any> getDownstreamSharedFlow(
        serviceKClass: KClass<T>,
        kProperty: KProperty1<T, SharedFlow<F>>,
        serviceId: String,
        propertyId: String,
        flowValueDeserializer: KSerializer<F>
    ): SharedFlow<F>

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
