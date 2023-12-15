package xyz.kotlinw.remoting.api.internal

interface RemoteCallHandlerImplementor: RemoteCallHandler {

    val servicePath: String // TODO helyette serviceId

    val methodDescriptors: Map<String, RemotingMethodDescriptor>

    suspend fun processCall(methodId: String, parameter: Any): Any?
}
