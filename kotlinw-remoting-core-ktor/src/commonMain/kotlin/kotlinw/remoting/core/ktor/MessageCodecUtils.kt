package kotlinw.remoting.core.ktor

import io.ktor.http.ContentType
import kotlinw.remoting.core.codec.KotlinxSerializationTextMessageCodec1
import kotlinx.serialization.StringFormat

fun GenericTextMessageCodec(
    serialFormat: StringFormat,
    contentType: ContentType
): KotlinxSerializationTextMessageCodec1 =
    KotlinxSerializationTextMessageCodec1(serialFormat, contentType.toString())
