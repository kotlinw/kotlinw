package kotlinw.remoting.core.ktor

import io.ktor.http.*
import kotlinw.remoting.core.MessageSerializerDescriptor
import kotlinx.serialization.StringFormat

fun MessageSerializerDescriptor.Companion.Text(
    contentType: ContentType,
    serialFormat: StringFormat
): MessageSerializerDescriptor.Text =
    MessageSerializerDescriptor.Text(contentType.toString(), serialFormat)
