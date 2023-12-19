package xyz.kotlinw.oauth2.ktor.client

import arrow.atomic.AtomicInt
import arrow.atomic.value
import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.auth.providers.BearerTokens
import io.ktor.client.plugins.auth.providers.bearer
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.statement.HttpResponse
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.kotlinw.oauth2.core.MutableOAuth2TokenStorage
import xyz.kotlinw.oauth2.core.MutableOAuth2TokenStorageImpl
import xyz.kotlinw.oauth2.core.OAuth2BearerTokens
import xyz.kotlinw.oauth2.core.OAuth2TokenResponse
import xyz.kotlinw.oauth2.core.tokens

fun Auth.bearer(
    realm: String? = null,
    tokenStorage: MutableOAuth2TokenStorage = MutableOAuth2TokenStorageImpl(),
    alwaysSendCredentials: (HttpRequestBuilder) -> Boolean = { true },
    loadInitialTokens: suspend () -> OAuth2BearerTokens? = { null },
    renewTokens: suspend (httpClient: HttpClient, httpResponse: HttpResponse, oldTokens: OAuth2BearerTokens?) -> OAuth2TokenResponse,
) {
    val tokensRevisionHolder = AtomicInt(0)
    val refreshTokensLock = Mutex()

    bearer {
        this.realm = realm
        this.sendWithoutRequest(alwaysSendCredentials)

        fun OAuth2BearerTokens.convertTokens() = BearerTokens(accessToken, refreshToken ?: "")

        fun BearerTokens.convertTokens() = OAuth2BearerTokens(accessToken, refreshToken.ifEmpty { null })

        loadTokens {
            val initialTokens = loadInitialTokens()
            tokenStorage.updateAndGetTokens { initialTokens }
            initialTokens?.convertTokens()
        }
        refreshTokens {
            val revisionBeforeLock = tokensRevisionHolder.value
            refreshTokensLock.withLock {
                if (tokensRevisionHolder.value == revisionBeforeLock) {
                    val revisionBeforeRefresh = tokensRevisionHolder.value
                    renewTokens(client, response, oldTokens?.convertTokens()).tokens.let { renewedTokens ->
                        tokenStorage.updateAndGetTokens { storedTokens ->
                            if (tokensRevisionHolder.value == revisionBeforeRefresh) {
                                renewedTokens
                            } else {
                                storedTokens
                            }
                        }
                    }
                } else {
                    tokenStorage.tokens
                }
            }
                ?.convertTokens()
        }
    }

    val provider = providers.last() as BearerAuthProvider
    tokenStorage.addTokenChangeListener {
        tokensRevisionHolder.incrementAndGet()

        if (it == null) {
            provider.clearToken() // Force the provider to request fresh tokens next time
        }
    }
}
