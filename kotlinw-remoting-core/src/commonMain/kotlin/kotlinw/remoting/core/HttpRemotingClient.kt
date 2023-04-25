package kotlinw.remoting.core

import kotlinw.remoting.client.core.RemotingClientImplementor
import kotlinw.remoting.server.core.RawMessage
import kotlinw.remoting.server.core.MessageCodec
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

class HttpRemotingClient(
    private val messageCodecDescriptor: MessageCodecDescriptor,
    private val httpClient: RemotingHttpClientImplementor,
    private val remotingServerBaseUrl: String
) : RemotingClientImplementor, MessageCodec by MessageCodecImpl(messageCodecDescriptor) {

    interface RemotingHttpClientImplementor {

        suspend fun post(
            url: String,
            requestBody: RawMessage,
            contentType: String,
            isResponseBodyText: Boolean // TODO ehelyett a return type legyen generikus
        ): RawMessage
    }

    private val messageCodec = MessageCodecImpl(messageCodecDescriptor)

    private fun buildServiceUrl(serviceName: String, methodName: String): String =
        "$remotingServerBaseUrl/remoting/call/$serviceName/$methodName" // TODO

    override suspend fun <T : Any, P : Any, R : Any> call(
        serviceKClass: KClass<T>,
        methodKFunction: KFunction<*>,
        serviceName: String,
        methodName: String,
        parameter: P,
        parameterSerializer: KSerializer<P>,
        resultDeserializer: KSerializer<R>
    ): R {
        val rawRequestMessage = messageCodec.encodeMessage(parameter, parameterSerializer)

        val rawResponseMessage = httpClient.post(
            buildServiceUrl(serviceName, methodName),
            rawRequestMessage,
            messageCodecDescriptor.contentType,
            messageCodecDescriptor.isText
        )

        return messageCodec.decodeMessage(rawResponseMessage, resultDeserializer)
    }
}
