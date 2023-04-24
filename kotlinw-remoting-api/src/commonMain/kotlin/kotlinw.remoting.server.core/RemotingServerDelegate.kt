package kotlinw.remoting.server.core

import kotlin.jvm.JvmInline

interface RemotingServerDelegate {

    val servicePath: String

    sealed interface Payload {

        @JvmInline
        value class Text(val text: String) : Payload

        @JvmInline
        value class Binary(val byteArray: ByteArray) : Payload
    }

    suspend fun processCall(methodPath: String, requestData: Payload): Payload
}
