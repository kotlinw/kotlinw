package kotlinw.remoting.client.ktor

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinw.remoting.core.RemotingHttpClientImplementor
import kotlinw.remoting.core.RemotingServerDelegateHelperImpl
import kotlinw.remoting.server.core.RemotingServerDelegate.Payload
import kotlinw.remoting.server.core.RemotingServerDelegateHelper

class KtorRemotingHttpClientImplementor(
    private val httpClient: HttpClient = HttpClient()
) : RemotingHttpClientImplementor {

    constructor(engine: HttpClientEngine) : this(HttpClient(engine))

    override suspend fun post(
        url: String,
        requestBody: Payload,
        contentType: String,
        isResponseBodyText: Boolean,
    ): Payload {
        val response =
            httpClient.post(url) {
                header(HttpHeaders.Accept, contentType)
                header(HttpHeaders.ContentType, contentType)

                setBody(
                    when (requestBody) {
                        is Payload.Binary -> requestBody.byteArray
                        is Payload.Text -> requestBody.text
                    }
                )
            }

        return if (isResponseBodyText)
            Payload.Text(response.bodyAsText())
        else
            Payload.Binary(response.body<ByteArray>())
    }
}
