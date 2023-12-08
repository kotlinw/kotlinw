package xyz.kotlinw.oauth2.core

import xyz.kotlinw.jwt.model.JwtToken.Converters.asInstant
import xyz.kotlinw.jwt.model.JwtToken.Converters.asString
import xyz.kotlinw.jwt.model.JwtToken.JwtTokenPayload

object OAuth2JwtTokenFields {

    const val FIELD_AZP = "azp"
    const val FIELD_AUTH_TIME = "auth_time"
    const val FIELD_ACR = "acr"
    const val FIELD_SCOPE = "scope"
    const val FIELD_SID = "sid"
    const val FIELD_SESSION_STATE = "session_state"

    val JwtTokenPayload.clientId get() = jsonObject[FIELD_AZP]?.asString

    val JwtTokenPayload.authenticationTimestamp get() = jsonObject[FIELD_AUTH_TIME]?.asInstant

    val JwtTokenPayload.authenticationContextClassReference get() = jsonObject[FIELD_ACR]?.asString

    val JwtTokenPayload.scope get() = jsonObject[FIELD_SCOPE]?.asString

    val JwtTokenPayload.sessionId get() = jsonObject[FIELD_SID]?.asString

    val JwtTokenPayload.sessionState get() = jsonObject[FIELD_SESSION_STATE]?.asString
}
