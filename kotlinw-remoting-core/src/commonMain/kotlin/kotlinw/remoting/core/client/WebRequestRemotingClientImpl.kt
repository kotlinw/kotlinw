package kotlinw.remoting.core.client

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import kotlinw.remoting.core.RawMessage
import kotlinw.remoting.core.RemotingMessage
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.core.common.SynchronousCallSupport
import kotlinw.util.stdlib.Url
import kotlinx.serialization.KSerializer
import xyz.kotlinw.remoting.api.internal.RemotingClientCallSupport

class WebRequestRemotingClientImpl<M : RawMessage>(
    private val messageCodec: MessageCodec<M>,
    private val httpSupportImplementor: SynchronousCallSupport,
    private val remoteServerBaseUrl: Url,
    loggerFactory: LoggerFactory
) : RemotingClientCallSupport {

    private val logger = loggerFactory.getLogger()

    private fun buildServiceUrl(serviceName: String, methodName: String): String =
        "$remoteServerBaseUrl/remoting/call/$serviceName/$methodName" // TODO konfigurálható path-t

    override suspend fun <T : Any, P : Any, R> call(
        serviceKClass: KClass<T>,
        methodKFunction: KFunction<R>,
        serviceId: String,
        methodId: String,
        parameter: P,
        parameterSerializer: KSerializer<P>,
        resultDeserializer: KSerializer<R>
    ): R {
        val requestMessage = RemotingMessage(parameter, null) // TODO metadata
        val rawRequestMessage =
            messageCodec.encodeMessage(requestMessage, parameterSerializer)
        val rawResponseMessage =
            httpSupportImplementor.call(
                buildServiceUrl(serviceId, methodId),
                rawRequestMessage,
                messageCodec
            )

        val resultMessage =
            try {
                messageCodec.decodeMessage(rawResponseMessage, resultDeserializer)
            } catch (e: Exception) {
                throw RuntimeException(
                    "Failed to decode response message of RPC method $serviceId.$methodId: $rawResponseMessage",
                    e
                )
            }

        // TODO metadata

        return resultMessage.payload
    }
}
