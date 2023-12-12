package xyz.kotlinw.oauth2.core

import kotlin.jvm.JvmInline
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import xyz.kotlinw.jwt.model.JwtToken.Converters.asBoolean
import xyz.kotlinw.jwt.model.JwtToken.Converters.asInstant
import xyz.kotlinw.jwt.model.JwtToken.Converters.asString
import xyz.kotlinw.jwt.model.JwtToken.JwtTokenPayload

// See: https://openid.net/specs/openid-connect-core-1_0.html#StandardClaims
object OAuth2JwtTokenPayloadFields {

    const val FIELD_NAME = "name"
    const val FIELD_EMAIL_VERIFIED = "email_verified"
    const val FIELD_PREFERRED_USERNAME = "preferred_username"
    const val FIELD_GIVEN_NAME = "given_name"
    const val FIELD_FAMILY_NAME = "family_name"
    const val FIELD_MIDDLE_NAME = "middle_name"
    const val FIELD_EMAIL = "email"
    const val FIELD_ADDRESS = "address"
    const val FIELD_NICKNAME = "nickname"
    const val FIELD_PROFILE = "profile"
    const val FIELD_PICTURE = "picture"
    const val FIELD_WEBSITE = "website"
    const val FIELD_GENDER = "gender"
    const val FIELD_BIRTHDATE = "birthdate"
    const val FIELD_ZONEINFO = "zoneinfo"
    const val FIELD_LOCALE = "locale"
    const val FIELD_PHONE_NUMBER = "phone_number"
    const val FIELD_PHONE_NUMBER_VERIFIED = "phone_number_verified"
    const val FIELD_UPDATED_AT = "updated_at"

    val JwtTokenPayload.name get() = jsonObject[FIELD_NAME]?.asString

    val JwtTokenPayload.isEmailVerified get() = jsonObject[FIELD_EMAIL_VERIFIED]?.asBoolean

    val JwtTokenPayload.preferredUsername get() = jsonObject[FIELD_PREFERRED_USERNAME]?.asString

    val JwtTokenPayload.givenName get() = jsonObject[FIELD_GIVEN_NAME]?.asString

    val JwtTokenPayload.familyName get() = jsonObject[FIELD_FAMILY_NAME]?.asString

    val JwtTokenPayload.middleName get() = jsonObject[FIELD_MIDDLE_NAME]?.asString

    val JwtTokenPayload.email get() = jsonObject[FIELD_EMAIL]?.asString

    val JwtTokenPayload.nickname get() = jsonObject[FIELD_NICKNAME]?.asString

    val JwtTokenPayload.profile get() = jsonObject[FIELD_PROFILE]?.asString

    val JwtTokenPayload.picture get() = jsonObject[FIELD_PICTURE]?.asString

    val JwtTokenPayload.website get() = jsonObject[FIELD_WEBSITE]?.asString

    val JwtTokenPayload.gender get() = jsonObject[FIELD_GENDER]?.asString

    val JwtTokenPayload.birthdate get() = jsonObject[FIELD_BIRTHDATE]?.asString

    val JwtTokenPayload.zoneInfo get() = jsonObject[FIELD_ZONEINFO]?.asString

    val JwtTokenPayload.locale get() = jsonObject[FIELD_LOCALE]?.asString

    val JwtTokenPayload.phoneNumber get() = jsonObject[FIELD_PHONE_NUMBER]?.asString

    val JwtTokenPayload.isPhoneNumberVerified get() = jsonObject[FIELD_PHONE_NUMBER_VERIFIED]?.asBoolean

    val JwtTokenPayload.updatedAt get() = jsonObject[FIELD_UPDATED_AT]?.asInstant

    @JvmInline
    value class OidcUserInfoAddress(val jsonObject: JsonObject) {

        val formatted get() = jsonObject["formatted"]?.asString

        val streetAddress get() = jsonObject["street_address"]?.asString

        val locality get() = jsonObject["locality"]?.asString

        val region get() = jsonObject["region"]?.asString

        val postalCode get() = jsonObject["postal_code"]?.asString

        val country get() = jsonObject["country"]?.asString
    }

    val JwtTokenPayload.address get() = jsonObject[FIELD_ADDRESS]?.jsonObject?.let { OidcUserInfoAddress(it) }
}
