package kotlinw.remoting.ktor.core

import io.ktor.http.*
import kotlinw.remoting.core.PayloadSerializer
import kotlinx.serialization.StringFormat

fun PayloadSerializer.Companion.TextPayloadSerializer(
    contentType: ContentType,
    serialFormat: StringFormat
): PayloadSerializer.TextPayloadSerializer =
    PayloadSerializer.TextPayloadSerializer(contentType.toString(), serialFormat)
