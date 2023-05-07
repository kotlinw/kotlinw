package kotlinw.remoting.api.internal.client

import kotlinw.remoting.api.client.RemotingClient
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

interface RemotingClientSynchronousCallSupport : RemotingClient {

    suspend fun <T : Any, P: Any, R> call(
        serviceKClass: KClass<T>,
        methodKFunction: KFunction<R>,
        serviceName: String,
        methodName: String,
        parameter: P,
        parameterSerializer: KSerializer<P>,
        resultDeserializer: KSerializer<R>
    ): R
}
