package kotlinw.remoting.core

import kotlinw.remoting.server.core.RemotingServerDelegate.Payload

interface RemotingHttpClientImplementor {

    suspend fun post(
        url: String,
        requestBody: Payload,
        contentType: String,
        isResponseBodyText: Boolean // TODO ehelyett a return type legyen generikus
    ): Payload
}
