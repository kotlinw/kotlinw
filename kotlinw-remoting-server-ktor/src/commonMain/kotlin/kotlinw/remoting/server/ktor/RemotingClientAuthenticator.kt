package kotlinw.remoting.server.ktor

import io.ktor.server.application.ApplicationCall
import xyz.kotlinw.remoting.api.MessagingPeerId

// TODO jobb nevet
// FIXME van ez egyáltalán használva?
interface RemotingClientAuthenticator {

    //TODO jobb nevet
    fun authenticateClient(call: ApplicationCall): MessagingPeerId
}
