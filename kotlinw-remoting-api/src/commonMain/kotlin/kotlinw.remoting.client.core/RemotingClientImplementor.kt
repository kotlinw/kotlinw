package kotlinw.remoting.client.core

import kotlinw.remoting.api.MessagingConnection
import kotlinw.remoting.api.MessageReceiver
import kotlinw.remoting.api.client.RemotingClient
import kotlinw.remoting.server.core.RemotingServerDelegateHelper
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

interface RemotingClientImplementor : RemotingClient, RemotingServerDelegateHelper {

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
        methodKFunction: KFunction<MessageReceiver<R>>,
        serviceName: String,
        methodName: String,
        arguments: Array<Any?>
    ): MessageReceiver<R>

    suspend fun <T : Any, R : Any?, S : Any?> connect(
        serviceKClass: KClass<T>,
        methodKFunction: KFunction<MessagingConnection<R, S>>,
        serviceName: String,
        methodName: String,
        arguments: Array<Any?>
    ): MessagingConnection<R, S>
}
