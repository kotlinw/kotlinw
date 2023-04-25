package kotlinw.remoting.core

import kotlinw.remoting.api.MessagingConnection
import kotlinw.remoting.api.MessageReceiver
import kotlinw.remoting.client.core.RemotingClientImplementor
import kotlinw.remoting.server.core.RawMessage
import kotlinw.remoting.server.core.MessageSerializer
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.StringFormat
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

class HttpRemotingClient(
    private val messageSerializerDescriptor: MessageSerializerDescriptor,
    private val httpClient: RemotingHttpClientImplementor,
    private val remotingServerBaseUrl: String
) : RemotingClientImplementor, MessageSerializer by MessageSerializerImpl(messageSerializerDescriptor) {

    interface RemotingHttpClientImplementor {

        suspend fun post(
            url: String,
            requestBody: RawMessage,
            contentType: String,
            isResponseBodyText: Boolean // TODO ehelyett a return type legyen generikus
        ): RawMessage
    }

    private val serialFormat = messageSerializerDescriptor.serialFormat

    private fun buildServiceUrl(serviceName: String, methodName: String): String =
        "$remotingServerBaseUrl/remoting/call/$serviceName/$methodName" // TODO

    private suspend fun <P : Any> wrapParameter(parameter: P): RemoteCallRequest<P> =
        RemoteCallRequest(parameter, null) // TODO

    override suspend fun <T : Any, P : Any, R : Any> call(
        serviceKClass: KClass<T>,
        methodKFunction: KFunction<*>,
        serviceName: String,
        methodName: String,
        parameter: P,
        parameterSerializer: KSerializer<P>,
        resultDeserializer: KSerializer<R>
    ): R {
        val wrappedParameter = wrapParameter(parameter)

        val requestSerializer = RemoteCallRequestSerializer(parameterSerializer)
        val requestBody =
            when (serialFormat) {
                is StringFormat ->
                    RawMessage.Text(serialFormat.encodeToString(requestSerializer, wrappedParameter))

                is BinaryFormat ->
                    RawMessage.Binary(serialFormat.encodeToByteArray(requestSerializer, wrappedParameter))

                else -> throw IllegalStateException()
            }

        val responsePayload = httpClient.post(
            buildServiceUrl(serviceName, methodName),
            requestBody,
            messageSerializerDescriptor.contentType,
            messageSerializerDescriptor.isText
        )

        val deserializer = RemoteCallResponseSerializer(resultDeserializer)
        val responseBody =
            when (serialFormat) {
                is StringFormat ->
                    serialFormat.decodeFromString(
                        deserializer,
                        (responsePayload as RawMessage.Text).text
                    )

                is BinaryFormat ->
                    serialFormat.decodeFromByteArray(
                        deserializer,
                        (responsePayload as RawMessage.Binary).byteArray
                    )

                else -> throw IllegalStateException()
            }

        return responseBody.payload
    }

    override suspend fun <T : Any, R> subscribe(
        serviceKClass: KClass<T>,
        methodKFunction: KFunction<MessageReceiver<R>>,
        serviceName: String,
        methodName: String,
        arguments: Array<Any?>
    ): MessageReceiver<R> {
        TODO("Not yet implemented")
    }

    override suspend fun <T : Any, R, S> connect(
        serviceKClass: KClass<T>,
        methodKFunction: KFunction<MessagingConnection<R, S>>,
        serviceName: String,
        methodName: String,
        arguments: Array<Any?>
    ): MessagingConnection<R, S> {
        TODO("Not yet implemented")
    }
}
