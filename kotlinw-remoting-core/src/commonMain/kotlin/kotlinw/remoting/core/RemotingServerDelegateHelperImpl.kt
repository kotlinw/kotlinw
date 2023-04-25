package kotlinw.remoting.core

import kotlinw.remoting.server.core.RemotingServerDelegate.Payload
import kotlinw.remoting.server.core.RemotingServerDelegateHelper
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialFormat
import kotlinx.serialization.StringFormat

class RemotingServerDelegateHelperImpl(
    private val serialFormat: SerialFormat
) : RemotingServerDelegateHelper {

    init {
        require(serialFormat is StringFormat || serialFormat is BinaryFormat)
    }

    override suspend fun <T : Any> readRequest(
        requestData: Payload,
        payloadDeserializer: KSerializer<T>
    ): T {
        val requestDeserializer = RemoteCallRequestSerializer(payloadDeserializer)
        // TODO allow access to metadata
        return when (serialFormat) {
            is StringFormat -> serialFormat.decodeFromString(
                requestDeserializer,
                (requestData as Payload.Text).text
            ).payload

            is BinaryFormat -> serialFormat.decodeFromByteArray(
                requestDeserializer,
                (requestData as Payload.Binary).byteArray
            ).payload

            else -> throw IllegalStateException()
        }
    }

    override fun <T : Any> writeResponse(payload: T, payloadSerializer: KSerializer<T>): Payload {
        val responseSerializer = RemoteCallResponseSerializer(payloadSerializer)
        val callResponse = RemoteCallResponse(payload, null) // TODO
        return when (serialFormat) {
            is StringFormat -> Payload.Text(serialFormat.encodeToString(responseSerializer, callResponse))
            is BinaryFormat -> Payload.Binary(serialFormat.encodeToByteArray(responseSerializer, callResponse))
            else -> throw IllegalStateException()
        }
    }
}
