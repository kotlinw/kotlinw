package kotlinw.remoting.core

import kotlinw.remoting.client.core.RemotingClientSynchronousCallSupport
import kotlinw.util.stdlib.Url
import kotlinx.serialization.KSerializer
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

class HttpRemotingClient<M : RawMessage>(
    private val messageCodec: MessageCodec<M>,
    private val httpClient: RemotingHttpClientImplementor,
    private val remoteServerBaseUrl: Url
) : RemotingClientSynchronousCallSupport {

    interface RemotingHttpClientImplementor {

        suspend fun <M: RawMessage> post(
            url: String,
            requestBody: M,
            messageCodecDescriptor: MessageCodecDescriptor
        ): M
    }

    private fun buildServiceUrl(serviceName: String, methodName: String): String =
        "$remoteServerBaseUrl/remoting/call/$serviceName/$methodName" // TODO

    override suspend fun <T : Any, P : Any, R : Any> call(
        serviceKClass: KClass<T>,
        methodKFunction: KFunction<R>,
        serviceName: String,
        methodName: String,
        parameter: P,
        parameterSerializer: KSerializer<P>,
        resultDeserializer: KSerializer<R>
    ): R {
        val parameterMessage = RemotingMessage(parameter, null) // TODO metadata
        val rawResultMessage =
            messageCodec.encodeMessage(parameterMessage, parameterSerializer)

        val rawResponseMessage = httpClient.post(
            buildServiceUrl(serviceName, methodName),
            rawResultMessage,
            messageCodec
        )

        val resultMessage =
            messageCodec.decodeMessage(rawResponseMessage, resultDeserializer)
        // TODO metadata

        return resultMessage.payload
    }
}
