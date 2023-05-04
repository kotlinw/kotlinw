package kotlinw.remoting.server.core

interface RemoteCallDelegator {

    val servicePath: String

    val methodDescriptors: Map<String, RemotingMethodDescriptor>

    suspend fun processCall(methodId: String, parameter: Any): Any
}
