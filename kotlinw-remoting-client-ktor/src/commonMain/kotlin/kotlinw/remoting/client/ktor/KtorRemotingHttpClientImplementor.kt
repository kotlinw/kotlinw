package kotlinw.remoting.client.ktor

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinw.remoting.core.HttpRemotingClient
import kotlinw.remoting.server.core.RawMessage

class KtorRemotingHttpClientImplementor(
    private val httpClient: HttpClient = HttpClient()
) : HttpRemotingClient.RemotingHttpClientImplementor {

    constructor(engine: HttpClientEngine) : this(HttpClient(engine))

    override suspend fun post(
        url: String,
        requestBody: RawMessage,
        contentType: String,
        isResponseBodyBinary: Boolean,
    ): RawMessage {
        val response =
            httpClient.post(url) {
                header(HttpHeaders.Accept, contentType)
                header(HttpHeaders.ContentType, contentType)

                setBody(
                    when (requestBody) {
                        is RawMessage.Binary -> requestBody.byteArray
                        is RawMessage.Text -> requestBody.text
                    }
                )
            }

        return if (isResponseBodyBinary)
            RawMessage.Binary(response.body<ByteArray>())
        else
            RawMessage.Text(response.bodyAsText())
    }
}
