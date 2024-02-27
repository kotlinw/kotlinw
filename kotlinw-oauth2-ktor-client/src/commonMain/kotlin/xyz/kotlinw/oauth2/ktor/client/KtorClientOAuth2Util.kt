package xyz.kotlinw.oauth2.ktor.client

import arrow.core.continuations.AtomicRef
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeoutCapability
import io.ktor.client.plugins.api.createClientPlugin
import io.ktor.http.HttpHeaders
import io.ktor.utils.io.core.use
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinw.logging.api.Logger
import kotlinw.util.stdlib.Url
import kotlinw.util.stdlib.concurrent.value
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Clock.System
import kotlinx.datetime.Instant
import xyz.kotlinw.oauth2.core.ClientCredentialsGrant
import xyz.kotlinw.oauth2.core.OAuth2AccessToken
import xyz.kotlinw.oauth2.core.Oauth2TokenErrorResponse

private data class OAuth2AccessTokenData(val accessToken: OAuth2AccessToken, val expiryTimestamp: Instant) {

    companion object {

        val Expired = OAuth2AccessTokenData("", Instant.DISTANT_PAST)
    }

    val isExpired get() = expiryTimestamp < Clock.System.now()
}

class TokenRequestFailedException(val tokenErrorResponse: Oauth2TokenErrorResponse) :
    RuntimeException("Token request failed: $tokenErrorResponse")

fun HttpClientConfig<*>.configureTokenAuth(
    tokenEndpointUrl: Url,
    clientId: String,
    clientSecret: String,
    logger: Logger
) {
    val token = AtomicRef(OAuth2AccessTokenData.Expired)
    val fetchTokenLock = Mutex()

    install(
        createClientPlugin("OAuth2AuthorizationHeaderPlugin") {
            onRequest { request, _ ->
                val tokenExpiryThreshold = (
                        request.getCapabilityOrNull(HttpTimeoutCapability)
                            ?.let {
                                (it.connectTimeoutMillis ?: 0L) + (it.requestTimeoutMillis ?: 0L)
                            } ?: 0L
                        ).milliseconds
                if (token.value.isExpired) {
                    fetchTokenLock.withLock {
                        if (token.value.isExpired) {
                            logger.debug { "Requesting access token: " / named("clientId", clientId) }
                            val tokenResponse =
                                arrow.core.raise.recover({
                                    HttpClient(this@createClientPlugin.client.engine).use {
                                        ClientCredentialsGrant.requestToken(
                                            it,
                                            tokenEndpointUrl,
                                            clientId,
                                            clientSecret
                                        ) // TODO handle errors
                                    }
                                }, {
                                    throw TokenRequestFailedException(it)
                                })
                            token.value = OAuth2AccessTokenData(
                                tokenResponse.accessToken,
                                System.now()
                                        - tokenExpiryThreshold
                                        + (tokenResponse.accessTokenExpirySeconds ?: 0).seconds
                            )
                        }
                    }
                }

                request.headers.append(HttpHeaders.Authorization, "Bearer ${token.value.accessToken}")
            }
        }
    )
}
