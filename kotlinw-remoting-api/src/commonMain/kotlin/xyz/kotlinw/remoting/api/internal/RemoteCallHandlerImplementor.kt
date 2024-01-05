package xyz.kotlinw.remoting.api.internal

interface RemoteCallHandlerImplementor<T: Any>: RemoteCallHandler<T> {

    val serviceId: String

    val methodDescriptors: Map<String, RemotingMethodDescriptor>

    suspend fun processCall(methodId: String, parameter: Any): Any?
}
