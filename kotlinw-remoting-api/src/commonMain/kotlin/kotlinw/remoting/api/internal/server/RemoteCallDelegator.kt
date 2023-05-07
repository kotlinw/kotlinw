package kotlinw.remoting.api.internal.server

interface RemoteCallDelegator {

    val servicePath: String

    val methodDescriptors: Map<String, RemotingMethodDescriptor>

    suspend fun processCall(methodId: String, parameter: Any): Any?
}
