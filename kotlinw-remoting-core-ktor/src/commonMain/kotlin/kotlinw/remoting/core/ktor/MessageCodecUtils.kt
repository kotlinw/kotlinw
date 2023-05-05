package kotlinw.remoting.core.ktor

import io.ktor.http.ContentType
import kotlinw.remoting.core.codec.GenericTextMessageCodec
import kotlinx.serialization.StringFormat

fun GenericTextMessageCodec(
    serialFormat: StringFormat,
    contentType: ContentType
): GenericTextMessageCodec =
    GenericTextMessageCodec(serialFormat, contentType.toString())
