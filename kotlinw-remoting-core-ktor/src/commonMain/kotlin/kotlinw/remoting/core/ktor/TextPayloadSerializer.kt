package kotlinw.remoting.core.ktor

import io.ktor.http.*
import kotlinw.remoting.core.MessageCodecDescriptor
import kotlinx.serialization.StringFormat

fun MessageCodecDescriptor.Companion.Text(
    contentType: ContentType,
    serialFormat: StringFormat
): MessageCodecDescriptor.Text =
    MessageCodecDescriptor.Text(contentType.toString(), serialFormat)
