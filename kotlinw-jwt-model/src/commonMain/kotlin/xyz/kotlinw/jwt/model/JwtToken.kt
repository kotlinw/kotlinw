package xyz.kotlinw.jwt.model

import kotlin.jvm.JvmInline
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.long
import xyz.kotlinw.jwt.model.JwtToken.Converters.asInstant
import xyz.kotlinw.jwt.model.JwtToken.Converters.asString
import xyz.kotlinw.jwt.model.JwtToken.Converters.asStringList

@Serializable
data class JwtToken(
    val header: JwtTokenHeader,
    val payload: JwtTokenPayload?,
    val signature: String
) {

    object Converters {

        inline val JsonElement.asString get() = jsonPrimitive.content

        inline val JsonElement.asInstant get() = jsonPrimitive.long.let { Instant.fromEpochSeconds(it) }

        inline val JsonElement.asStringList
            get() =
                when (this) {
                    is JsonArray -> toList().map { it.asString }
                    is JsonPrimitive -> listOf(asString)
                    else -> throw IllegalStateException("Unexpected value: $this")
                }
    }

    @JvmInline
    @Serializable
    value class JwtTokenHeader(val jsonObject: JsonObject) {

        companion object {

            const val FIELD_ALG = "alg"
            const val FIELD_TYP = "typ"
            const val FIELD_KID = "kid"

            private val commonFields = setOf(FIELD_ALG, FIELD_TYP, FIELD_KID)
        }

        val algorithm get() = jsonObject[FIELD_ALG]?.asString

        val type get() = jsonObject[FIELD_TYP]?.asString

        val keyId get() = jsonObject[FIELD_KID]?.asString

        val additionalFields get() = jsonObject.filterKeys { it !in commonFields }
    }

    @JvmInline
    @Serializable
    value class JwtTokenPayload(val jsonObject: JsonObject) {

        companion object {

            const val FIELD_ISS = "iss"
            const val FIELD_SUB = "sub"
            const val FIELD_AUD = "aud"
            const val FIELD_EXP = "exp"
            const val FIELD_NBF = "nbf"
            const val FIELD_IAT = "iat"
            const val FIELD_JTI = "jti"
            const val FIELD_TYP = "typ"
            const val FIELD_AZP = "azp"

            private val commonFields = setOf(
                FIELD_ISS, FIELD_SUB, FIELD_AUD, FIELD_EXP, FIELD_NBF, FIELD_IAT, FIELD_JTI, FIELD_TYP, FIELD_AZP
            )
        }

        val issuer get() = jsonObject[FIELD_ISS]?.asStringList

        val subject get() = jsonObject[FIELD_SUB]?.asString

        val audience get() = jsonObject[FIELD_AUD]?.asStringList

        val expires get() = jsonObject[FIELD_EXP]?.asInstant

        val notBefore get() = jsonObject[FIELD_NBF]?.asInstant

        val issuedAt get() = jsonObject[FIELD_IAT]?.asInstant

        val jwtId get() = jsonObject[FIELD_JTI]?.asString

        val type get() = jsonObject[FIELD_TYP]?.asString

        val clientId get() = jsonObject[FIELD_AZP]?.asString

        val additionalFields get() = jsonObject.filterKeys { it !in commonFields }
    }
}
