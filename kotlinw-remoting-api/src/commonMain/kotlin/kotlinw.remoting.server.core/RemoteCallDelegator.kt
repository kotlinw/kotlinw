package kotlinw.remoting.server.core

interface RemoteCallDelegator {

    val servicePath: String

    suspend fun processCall(
        methodPath: String,
        requestData: RawMessage,
        messageSerializer: MessageSerializer
    ): RawMessage
}
