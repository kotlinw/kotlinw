package xyz.kotlinw.oauth2.core

import arrow.core.raise.Raise
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.pluginOrNull
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import io.ktor.http.appendPathSegments
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import io.ktor.utils.io.core.use
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import kotlinw.util.stdlib.Url
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.kotlinw.jwt.model.JwtToken.Companion.parseJwtToken
import xyz.kotlinw.serialization.json.standardLongTermJson
import xyz.kotlinw.util.ktor.client.process

private val logger = PlatformLogging.getLogger()

private suspend fun <T> HttpClient.withConfiguredHttpClient(block: suspend (HttpClient) -> T): T =
    config {
        pluginOrNull(ContentNegotiation) ?: install(ContentNegotiation) {
            json(standardLongTermJson()) // TODO serializerservice
        }
    }.use {
        block(it)
    }

suspend fun HttpClient.fetchOpenidConnectProviderMetadata(authorizationServerUrl: Url) =
    withConfiguredHttpClient {
        it.get(
            URLBuilder(authorizationServerUrl.value)
                .appendPathSegments(".well-known", "openid-configuration")
                .build()
        )
            .body<OpenidConnectProviderMetadata>()
    }

internal fun buildGenericAuthorizationUrl(
    authorizationEndpoint: Url,
    clientId: String,
    responseType: String? = null,
    redirectUrl: Url? = null,
    scopes: List<String>? = null,
    state: String? = null,
    codeChallenge: String? = null,
    codeChallengeMethod: String? = null
): Url =
    Url(
        URLBuilder(authorizationEndpoint.value)
            .parameters.apply {
                append("client_id", clientId)
                if (responseType != null) {
                    append("response_type", responseType)
                }
                if (redirectUrl != null) {
                    append("redirect_uri", redirectUrl.value)
                }
                if (!scopes.isNullOrEmpty()) {
                    append("scope", encodeScopes(scopes))
                }
                if (state != null) {
                    append("state", state)
                }
                if (codeChallenge != null) {
                    append("code_challenge", codeChallenge)
                }
                if (codeChallengeMethod != null) {
                    append("code_challenge_method", codeChallengeMethod)
                }
            }
            .build().toString()
    )

private fun encodeScopes(scopes: List<String>) = scopes.joinToString(" ")

/**
 * See: https://datatracker.ietf.org/doc/html/rfc6749#section-4.1
 */
object AuthorizationCodeGrant {

    private const val RESPONSE_TYPE_CODE = "code"

    fun buildAuthorizationUrl(
        authorizationEndpoint: Url,
        clientId: String,
        redirectUrl: Url? = null,
        scopes: List<String>? = null,
        state: String? = null
    ) =
        buildGenericAuthorizationUrl(authorizationEndpoint, clientId, RESPONSE_TYPE_CODE, redirectUrl, scopes, state)

    fun buildAuthorizationUrlWithPkce(
        authorizationEndpoint: Url,
        clientId: String,
        codeChallenge: String,
        codeChallengeMethod: String,
        redirectUrl: Url? = null,
        scopes: List<String>? = null,
        state: String? = null,
    ) =
        buildGenericAuthorizationUrl(
            authorizationEndpoint,
            clientId,
            RESPONSE_TYPE_CODE,
            redirectUrl,
            scopes,
            state,
            codeChallenge,
            codeChallengeMethod
        )
}

object ClientCredentialsGrant {

    // TODO context(HttpClient)
    context(Raise<Oauth2TokenErrorResponse>)
    suspend fun requestToken(
        httpClient: HttpClient,
        tokenEndpointUrl: Url,
        clientId: String,
        clientSecret: String
    ): OAuth2TokenResponse =
        httpClient.withConfiguredHttpClient {
            it.submitForm(
                tokenEndpointUrl.value,
                Parameters.build {
                    append("client_id", clientId)
                    append("client_secret", clientSecret)
                    append("grant_type", "client_credentials")
                }
            )
                .process({
                    it.body<OAuth2TokenResponse>()
                }, {
                    raise(it.body<Oauth2TokenErrorResponse>())
                })
        } // TODO 401 és további hibák kezelése
}

/**
 * See: https://datatracker.ietf.org/doc/html/rfc8628#section-3.2
 */
@Serializable
data class DeviceAuthorizationResponse(

    @SerialName("device_code")
    val deviceCode: String,

    @SerialName("user_code")
    val userCode: String,

    @SerialName("verification_uri")
    val verificationUri: Url,

    @SerialName("verification_uri_complete")
    val verificationUriComplete: Url? = null,

    @SerialName("expires_in")
    val expiresInSeconds: Int? = null,

    @SerialName("interval")
    val minimumPollingIntervalSeconds: Int = 5
)

/**
 * See: https://datatracker.ietf.org/doc/html/rfc8628#section-3.1
 */
private fun buildDeviceAuthorizationUrl(
    deviceAuthorizationEndpoint: Url,
    clientId: String,
    scopes: List<String>? = null
): Url =
    buildGenericAuthorizationUrl(deviceAuthorizationEndpoint, clientId, scopes = scopes)

/**
 * See: https://datatracker.ietf.org/doc/html/rfc8628
 */
// TODO context(HttpClient)
suspend fun HttpClient.authorizeDevice(
    deviceAuthorizationEndpoint: Url,
    tokenEndpoint: Url,
    clientId: String,
    scopes: List<String>? = null,
    defaultAuthorizationTimeout: Duration = 5.minutes,
    authorizationResponseCallback: suspend (DeviceAuthorizationResponse) -> Unit
): OAuth2TokenResponse {
    logger.debug { "Initiating device authorization flow..." }
    return withConfiguredHttpClient { httpClient ->
        val authorizationResponse = httpClient.submitForm(
            deviceAuthorizationEndpoint.value,
            Parameters.build {
                append("client_id", clientId)
                if (!scopes.isNullOrEmpty()) {
                    append("scope", encodeScopes(scopes))
                }
            }
        )

        if (!authorizationResponse.status.isSuccess()) {
            throw RuntimeException() // TODO handle error, like: 401 Unauthorized, {"error":"invalid_client","error_description":"Invalid client or Invalid client credentials"}
        }

        val deviceAuthorizationResponse = authorizationResponse.body<DeviceAuthorizationResponse>()

        authorizationResponseCallback(deviceAuthorizationResponse)

        withTimeout(deviceAuthorizationResponse.expiresInSeconds?.seconds ?: defaultAuthorizationTimeout) {
            var pollingDelay = deviceAuthorizationResponse.minimumPollingIntervalSeconds.seconds
            while (true) {
                logger.debug { "Waiting for " / pollingDelay / " before checking the result..." }
                delay(pollingDelay)

                val tokenResponse = httpClient.submitForm(
                    tokenEndpoint.value,
                    Parameters.build {
                        append("client_id", clientId)
                        append("device_code", deviceAuthorizationResponse.deviceCode)
                        append("grant_type", "urn:ietf:params:oauth:grant-type:device_code")
                    }
                )

                if (tokenResponse.status.isSuccess()) {
                    return@withTimeout tokenResponse.body<OAuth2TokenResponse>()
                } else {
                    when (val error = tokenResponse.body<Oauth2TokenErrorResponse>().error) {
                        "invalid_request", "invalid_client", "invalid_grant", "unauthorized_client", "unsupported_grant_type", "error_description", "error_uri" -> {
                            throw RuntimeException("OAuth2 token response error: $error") // TODO specifikus hibát
                        }

                        "authorization_pending" -> {
                            logger.trace { "Authorization pending." }
                        }

                        "slow_down" -> {
                            logger.trace { "Increasing polling delay." }
                            pollingDelay += 5.seconds
                        }

                        "access_denied", "expired_token" -> throw RuntimeException("OAuth2 token response error: $error") // TODO specifikus hibát
                    }
                }
            }

            @Suppress("UNREACHABLE_CODE")
            throw IllegalStateException()
        }
    }
}

fun OAuth2TokenResponse.decodeJwtAccessToken() = accessToken.parseJwtToken()

fun OAuth2TokenResponse.decodeJwtIdToken() = idToken?.parseJwtToken()
