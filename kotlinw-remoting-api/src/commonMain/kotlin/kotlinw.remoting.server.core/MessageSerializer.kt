package kotlinw.remoting.server.core

import kotlinx.serialization.KSerializer

interface MessageSerializer {

    fun <T : Any> readMessage(requestData: RawMessage, payloadDeserializer: KSerializer<T>): T

    fun <T : Any> writeMessage(payload: T, payloadSerializer: KSerializer<T>): RawMessage
}
