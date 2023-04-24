package kotlinw.remoting.client.core

import kotlinw.remoting.api.ClientConnection
import kotlinw.remoting.api.ClientSubscription
import kotlinw.remoting.api.client.RemotingClient
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationStrategy
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

    suspend fun <T : Any, R : Any?> subscribe(
        serviceKClass: KClass<T>,
        methodKFunction: KFunction<ClientSubscription<R>>,
        serviceName: String,
        methodName: String,
        arguments: Array<Any?>
    ): ClientSubscription<R>

    suspend fun <T : Any, R : Any?, S : Any?> connect(
        serviceKClass: KClass<T>,
        methodKFunction: KFunction<ClientConnection<R, S>>,
        serviceName: String,
        methodName: String,
        arguments: Array<Any?>
    ): ClientConnection<R, S>
}
