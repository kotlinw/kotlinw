package kotlinw.remoting.server.core

import kotlinx.serialization.KSerializer

interface MessageCodec {

    fun <T : Any> decodeMessage(rawMessage: RawMessage, payloadDeserializer: KSerializer<T>): T

    fun <T : Any> encodeMessage(payload: T, payloadSerializer: KSerializer<T>): RawMessage
}
