package kotlinw.remoting.api.internal.server

// TODO rename: RemoteCallHandler
interface RemoteCallDelegator {

    val servicePath: String // TODO helyette serviceId

    val methodDescriptors: Map<String, RemotingMethodDescriptor>

    suspend fun processCall(methodId: String, parameter: Any): Any?
}
