package kotlinw.remoting.server.core

import kotlinx.coroutines.flow.SharedFlow
import kotlinx.serialization.KSerializer

interface RemoteCallDelegator {

    val servicePath: String

    val methodDescriptors: Map<String, RemotingMethodDescriptor>

    suspend fun processCall(methodId: String, parameter: Any): Any
}
