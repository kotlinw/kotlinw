package kotlinw.remoting.core

import kotlinw.remoting.server.core.RemotingServerDelegate.Payload
import kotlinw.remoting.server.core.RemotingServerDelegateHelper
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.StringFormat

class RemotingServerDelegateHelperImpl(
    private val serializer: SerialFormat
) : RemotingServerDelegateHelper {

    init {
        require(serializer is StringFormat || serializer is BinaryFormat)
    }

    override suspend fun <T : Any> readRequest(
        requestData: Payload,
        payloadDeserializer: KSerializer<T>
    ): T {
        val requestDeserializer = RemoteCallRequestSerializer(payloadDeserializer)
        return when (serializer) {
            is StringFormat -> serializer.decodeFromString(
                requestDeserializer,
                (requestData as Payload.Text).text
            ).payload

            is BinaryFormat -> serializer.decodeFromByteArray(
                requestDeserializer,
                (requestData as Payload.Binary).byteArray
            ).payload

            else -> throw IllegalStateException()
        }
    }

    override fun <T : Any> writeResponse(payload: T, payloadSerializer: KSerializer<T>): Payload {
        val responseSerializer = RemoteCallResponseSerializer(payloadSerializer)
        val callResponse = RemoteCallResponse(payload, null)
        return when (serializer) {
            is StringFormat -> Payload.Text(serializer.encodeToString(responseSerializer, callResponse))
            is BinaryFormat -> Payload.Binary(serializer.encodeToByteArray(responseSerializer, callResponse))
            else -> throw IllegalStateException()
        }
    }
}
