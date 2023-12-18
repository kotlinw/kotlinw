package kotlinw.remoting.core.common

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlinw.remoting.core.ServiceLocator
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.KSerializer
import xyz.kotlinw.remoting.api.RemotingClient
import xyz.kotlinw.remoting.api.internal.RemotingClientCallSupport
import xyz.kotlinw.remoting.api.internal.RemotingClientFlowSupport

class DelegatingRemotingClient(
        private val bidirectionalMessagingManager: BidirectionalMessagingManager
    ) : RemotingClient, RemotingClientCallSupport, RemotingClientFlowSupport {

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
            bidirectionalMessagingManager.requestColdFlowResult(
                ServiceLocator(serviceId, methodId),
                parameter,
                parameterSerializer,
                flowValueDeserializer,
                callId
            )

        override suspend fun <T : Any, P : Any, R> call(
            serviceKClass: KClass<T>,
            methodKFunction: KFunction<R>,
            serviceId: String,
            methodId: String,
            parameter: P,
            parameterSerializer: KSerializer<P>,
            resultDeserializer: KSerializer<R>
        ): R =
            bidirectionalMessagingManager.call(
                ServiceLocator(serviceId, methodId),
                parameter,
                parameterSerializer,
                resultDeserializer
            )
    }
