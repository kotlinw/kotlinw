package kotlinw.remoting.core.ktor

import io.ktor.http.ContentType
import kotlinw.remoting.core.codec.KotlinxSerializationTextMessageCodec
import kotlinx.serialization.StringFormat

fun GenericTextMessageCodec(
    serialFormat: StringFormat,
    contentType: ContentType
): KotlinxSerializationTextMessageCodec =
    KotlinxSerializationTextMessageCodec(serialFormat, contentType.toString())
