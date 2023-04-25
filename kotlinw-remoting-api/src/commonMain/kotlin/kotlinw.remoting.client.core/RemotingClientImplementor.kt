package kotlinw.remoting.client.core

import kotlinw.remoting.api.client.RemotingClient
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

interface RemotingClientImplementor : RemotingClient {

    suspend fun <T : Any, P: Any, R : Any> call(
        serviceKClass: KClass<T>,
        methodKFunction: KFunction<*>,
        serviceName: String,
        methodName: String,
        parameter: P,
        parameterSerializer: KSerializer<P>,
        resultDeserializer: KSerializer<R>
    ): R
}
