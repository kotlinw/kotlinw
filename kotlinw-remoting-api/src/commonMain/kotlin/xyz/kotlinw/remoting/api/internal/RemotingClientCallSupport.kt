package xyz.kotlinw.remoting.api.internal

import xyz.kotlinw.remoting.api.RemotingClient
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

interface RemotingClientCallSupport : RemotingClient {

    suspend fun <T : Any, P: Any, R> call(
        serviceKClass: KClass<T>,
        methodKFunction: KFunction<R>,
        serviceId: String,
        methodId: String,
        parameter: P,
        parameterSerializer: KSerializer<P>,
        resultDeserializer: KSerializer<R>
    ): R
}
