package kotlinw.remoting.server.ktor

import io.ktor.server.application.ApplicationCall
import xyz.kotlinw.remoting.api.RemoteCallContext
import xyz.kotlinw.remoting.api.RemoteConnectionId

data class WebRequestRemoteCallContext(
    override val remoteConnectionId: RemoteConnectionId,
    val applicationCall: ApplicationCall
) :
    RemoteCallContext
