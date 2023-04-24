package kotlinw.remoting.client.ktor

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinw.remoting.api.ClientConnection
import kotlinw.remoting.api.ClientSubscription
import kotlinw.remoting.client.core.RemotingClientImplementor
import kotlinw.remoting.core.RemoteCallRequest
import kotlinw.remoting.core.RemoteCallRequestSerializer
import kotlinw.remoting.core.RemoteCallResponse
import kotlinw.remoting.core.RemoteCallResponseSerializer
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.StringFormat
import kotlinx.serialization.serializer
import kotlin.reflect.KClass
import kotlin.reflect.KFunction

class KtorHttpRemotingClient(
    private val serializer: SerialFormat,
    private val contentType: ContentType,
    httpClientEngine: HttpClientEngine
) :
    RemotingClientImplementor {

    private val httpClient = HttpClient(httpClientEngine) {
        // TODO
    }

    init {
        require(serializer is StringFormat || serializer is BinaryFormat)
    }

    override suspend fun <T : Any, P : Any, R : Any> call(
        serviceKClass: KClass<T>,
        methodKFunction: KFunction<*>,
        serviceName: String,
        methodName: String,
        parameter: P,
        parameterSerializer: KSerializer<P>,
        resultDeserializer: KSerializer<R>
    ): R {
        val response = httpClient.post(buildServiceUrl(serviceName, methodName)) {
            val requestBody = requestFactory(parameter)

            header(HttpHeaders.Accept, contentType.toString())
            header(HttpHeaders.ContentType, contentType.toString())

            val requestSerializer = RemoteCallRequestSerializer(parameterSerializer)
            when (serializer) {
                is StringFormat -> setBody(serializer.encodeToString(requestSerializer, requestBody))
                is BinaryFormat -> setBody(serializer.encodeToByteArray(requestSerializer, requestBody))
            }
        }

        val deserializer = RemoteCallResponseSerializer(resultDeserializer)
        val responseBody =
            when (serializer) {
                is StringFormat -> serializer.decodeFromString(deserializer, response.bodyAsText())
                is BinaryFormat -> serializer.decodeFromByteArray(deserializer, response.body<ByteArray>())
                else -> throw IllegalStateException()
            }

        return responseBody.payload
    }

    private fun buildServiceUrl(serviceName: String, methodName: String): Url =
        Url("/$serviceName/$methodName") // TODO

    private fun <P : Any> requestFactory(parameter: P): RemoteCallRequest<P> =
        RemoteCallRequest(parameter, null)

    override suspend fun <T : Any, R> subscribe(
        serviceKClass: KClass<T>,
        methodKFunction: KFunction<ClientSubscription<R>>,
        serviceName: String,
        methodName: String,
        arguments: Array<Any?>
    ): ClientSubscription<R> {
        TODO("Not yet implemented")
    }

    override suspend fun <T : Any, R, S> connect(
        serviceKClass: KClass<T>,
        methodKFunction: KFunction<ClientConnection<R, S>>,
        serviceName: String,
        methodName: String,
        arguments: Array<Any?>
    ): ClientConnection<R, S> {
        TODO("Not yet implemented")
    }
}
