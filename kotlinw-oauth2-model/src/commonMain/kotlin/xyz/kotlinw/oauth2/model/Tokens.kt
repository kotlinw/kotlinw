package xyz.kotlinw.oauth2.model

import kotlinw.util.stdlib.Url
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

typealias AccessToken = String

typealias RefreshToken = String

@Serializable
data class Oauth2TokenResponse(

    @SerialName("access_token")
    val accessToken: AccessToken,

    @SerialName("token_type")
    val tokenType: String,

    @SerialName("expires_in")
    val accessTokenExpirySeconds: Int? = null,

    @SerialName("refresh_token")
    val refreshToken: RefreshToken? = null,

    @SerialName("refresh_expires_in")
    val refreshTokenExpirySeconds: Int? = null,

    val scope: String? = null,

    @SerialName("id_token")
    val idToken: String? = null,

    @SerialName("not-before-policy")
    val notBeforePolicy: Int? = null,

    @SerialName("device_secret")
    val deviceSecret: String? = null,

    @SerialName("session_state")
    val sessionState: String? = null
)

@Serializable
data class Oauth2TokenErrorResponse(

    val error: String,

    @SerialName("error_description")
    val errorDescription: String? = null,

    @SerialName("error_uri")
    val errorUri: Url? = null
)
