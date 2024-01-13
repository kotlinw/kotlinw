package xyz.kotlinw.oauth2.server

import io.ktor.server.auth.OAuthAccessTokenResponse
import xyz.kotlinw.oauth2.core.OAuth2TokenResponse

fun OAuthAccessTokenResponse.OAuth2.convertToOauth2TokenResponse(): OAuth2TokenResponse {
    val extraParameters = extraParameters
    // TODO pass further extra parameters as well
    return OAuth2TokenResponse(
        accessToken = accessToken,
        tokenType = tokenType,
        accessTokenExpirySeconds = expiresIn.toInt(),
        refreshToken = refreshToken,
        refreshTokenExpirySeconds = extraParameters["refresh_expires_in"]?.toInt(),
        scope = extraParameters["scope"],
        idToken = extraParameters["id_token"],
        notBeforePolicy = extraParameters["not-before-policy"]?.toInt(),
        deviceSecret = extraParameters["device_secret"],
        sessionState = extraParameters["session_state"]
    )
}
