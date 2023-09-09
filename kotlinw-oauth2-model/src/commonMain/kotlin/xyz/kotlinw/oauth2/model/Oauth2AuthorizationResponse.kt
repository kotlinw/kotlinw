package xyz.kotlinw.oauth2.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Oauth2AuthorizationResponse(

    @SerialName("access_token")
    val accessToken: String,

    @SerialName("expires_in")
    val accessTokenExpirySeconds: Int,

    @SerialName("refresh_token")
    val refreshToken: String? = null,

    @SerialName("refresh_expires_in")
    val refreshTokenExpirySeconds: Int? = null,

    val scope: String,

    @SerialName("token_type")
    val tokenType: String,

    @SerialName("id_token")
    val idToken: String? = null,

    @SerialName("not-before-policy")
    val notBeforePolicy: Int? = null,

    @SerialName("device_secret")
    val deviceSecret: String? = null,

    @SerialName("session_state")
    val sessionState: String? = null
)
