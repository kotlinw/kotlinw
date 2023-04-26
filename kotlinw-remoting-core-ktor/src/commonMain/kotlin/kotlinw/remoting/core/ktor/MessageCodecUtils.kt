package kotlinw.remoting.core.ktor

import io.ktor.http.ContentType
import kotlinw.remoting.core.GenericMessageCodec
import kotlinw.remoting.core.GenericTextMessageCodec
import kotlinw.remoting.core.RawMessage
import kotlinx.serialization.StringFormat

fun GenericTextMessageCodec(
    serialFormat: StringFormat,
    contentType: ContentType
): GenericTextMessageCodec =
    GenericTextMessageCodec(serialFormat, contentType.toString())
