package xyz.kotlinw.module.pwa.server

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.utils.io.ByteWriteChannel
import io.ktor.utils.io.jvm.javaio.toOutputStream
import java.io.OutputStream

//TODO util
class OutputStreamContentWithLength(
    override val contentType: ContentType,
    override val contentLength: Long? = null,
    override val status: HttpStatusCode? = null,
    private val body: suspend OutputStream.() -> Unit
) : OutgoingContent.WriteChannelContent() {

    override suspend fun writeTo(channel: ByteWriteChannel) {
        channel.toOutputStream().use { it.body() }
    }
}
