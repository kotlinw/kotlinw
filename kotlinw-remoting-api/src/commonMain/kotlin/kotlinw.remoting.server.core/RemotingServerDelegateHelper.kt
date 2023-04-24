package kotlinw.remoting.server.core

import kotlinx.serialization.KSerializer

interface RemotingServerDelegateHelper {

    suspend fun <T : Any> readRequest(
        requestData: RemotingServerDelegate.Payload,
        payloadDeserializer: KSerializer<T>
    ): T

    fun <T : Any> writeResponse(payload: T, payloadSerializer: KSerializer<T>): RemotingServerDelegate.Payload
}
