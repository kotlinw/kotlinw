package kotlinw.remoting.core.ktor

import io.ktor.http.ContentType
import kotlinw.remoting.core.MessageCodecImpl
import kotlinx.serialization.SerialFormat

fun MessageCodecImpl(
    serialFormat: SerialFormat,
    contentType: ContentType,
    isBinary: Boolean
): MessageCodecImpl =
    MessageCodecImpl(serialFormat, contentType.toString(), isBinary)
