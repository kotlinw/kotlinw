package kotlinw.remoting.core.client

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlinw.remoting.core.ServiceLocator
import kotlinw.remoting.core.common.BidirectionalMessagingManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer
import xyz.kotlinw.remoting.api.PersistentRemotingConnection

class PersistentRemotingConnectionImpl(
    private val messagingManager: BidirectionalMessagingManager
) : PersistentRemotingConnection, CoroutineScope by messagingManager {

    override suspend fun <T : Any, P : Any, R> call(
        serviceKClass: KClass<T>,
        methodKFunction: KFunction<R>,
        serviceId: String,
        methodId: String,
        parameter: P,
        parameterSerializer: KSerializer<P>,
        resultDeserializer: KSerializer<R>
    ): R =
        messagingManager.call(
            ServiceLocator(serviceId, methodId),
            parameter,
            parameterSerializer,
            resultDeserializer
        )

    override suspend fun <T : Any, P : Any, F> requestIncomingColdFlow(
        serviceKClass: KClass<T>,
        methodKFunction: KFunction<Flow<F>>,
        serviceId: String,
        methodId: String,
        parameter: P,
        parameterSerializer: KSerializer<P>,
        flowValueDeserializer: KSerializer<F>,
        callId: String
    ): Flow<F> =
        messagingManager.requestColdFlowResult(
            ServiceLocator(serviceId, methodId),
            parameter,
            parameterSerializer,
            flowValueDeserializer,
            callId
        )

    override suspend fun close() {
        messagingManager.close()
    }
}
