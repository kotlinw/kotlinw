package kotlinw.remoting.server.ktor

import io.ktor.server.application.ApplicationCall
import xyz.kotlinw.remoting.api.MessagingPeerId

interface RemotingClientAuthenticator {

    fun authenticateClient(call: ApplicationCall): MessagingPeerId
}
