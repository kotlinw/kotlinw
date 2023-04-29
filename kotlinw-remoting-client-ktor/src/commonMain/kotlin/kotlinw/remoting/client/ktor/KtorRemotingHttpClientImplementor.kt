package kotlinw.remoting.client.ktor

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import kotlinw.remoting.core.HttpRemotingClient
import kotlinw.remoting.core.MessageCodec
import kotlinw.remoting.core.MessageCodecDescriptor
import kotlinw.remoting.core.RawMessage
import kotlinw.util.stdlib.toReadOnlyByteArray
import kotlinw.util.stdlib.view

class KtorRemotingHttpClientImplementor(
    private val httpClient: HttpClient
) : HttpRemotingClient.RemotingHttpClientImplementor {

    constructor(engine: HttpClientEngine) : this(HttpClient(engine))

    override suspend fun <M : RawMessage> post(
        url: String,
        requestBody: M,
        messageCodecDescriptor: MessageCodecDescriptor
    ): M {
        val response =
            httpClient.post(url) {
                header(HttpHeaders.Accept, messageCodecDescriptor.contentType)
                header(HttpHeaders.ContentType, messageCodecDescriptor.contentType)

                setBody(
                    if (messageCodecDescriptor.isBinary) {
                        (requestBody as RawMessage.Binary).byteArrayView.toReadOnlyByteArray()
                    } else {
                        (requestBody as RawMessage.Text).text
                    }
                )
            }

        return if (messageCodecDescriptor.isBinary)
            RawMessage.Binary(response.body<ByteArray>().view()) as M
        else
            RawMessage.Text(response.bodyAsText()) as M
    }
}
