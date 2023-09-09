package xyz.kotlinw.oauth2.core

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.http.Parameters
import io.ktor.http.URLBuilder
import kotlinw.util.stdlib.Url
import xyz.kotlinw.oauth2.model.Oauth2AuthorizationResponse
import xyz.kotlinw.oauth2.model.Oauth2ResponseType
import xyz.kotlinw.oauth2.model.Oauth2ResponseType.Code
import xyz.kotlinw.oauth2.model.OpenidConnectProviderMetadata

suspend fun HttpClient.fetchOpenidConnectProviderMetadata(openidConfigurationUrl: Url) =
    get(openidConfigurationUrl.value).body<OpenidConnectProviderMetadata>()

fun buildAuthorizationUrl(
    authorizationEndpoint: Url,
    responseType: Oauth2ResponseType,
    clientId: String,
    redirectUrl: Url? = null,
    scopes: List<String>? = null,
    state: String? = null,
    codeChallenge: String? = null,
    codeChallengeMethod: String? = null
): Url =
    Url(
        URLBuilder(authorizationEndpoint.value)
            .parameters.apply {
                append("response_type", responseType.value)
                append("client_id", clientId)
                if (redirectUrl != null) {
                    append("redirect_uri", redirectUrl.value)
                }
                if (!scopes.isNullOrEmpty()) {
                    append("scope", scopes.joinToString(" "))
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

/**
 * See: https://datatracker.ietf.org/doc/html/rfc6749#section-4.1
 */
object AuthorizationCodeGrant {

    fun buildAuthorizationUrl(
        authorizationEndpoint: Url,
        clientId: String,
        redirectUrl: Url? = null,
        scopes: List<String>? = null,
        state: String? = null
    ) =
        buildAuthorizationUrl(authorizationEndpoint, Code, clientId, redirectUrl, scopes, state)

    fun buildAuthorizationUrlWithPkce(
        authorizationEndpoint: Url,
        clientId: String,
        codeChallenge: String,
        codeChallengeMethod: String,
        redirectUrl: Url? = null,
        scopes: List<String>? = null,
        state: String? = null,
    ) =
        buildAuthorizationUrl(
            authorizationEndpoint,
            Code,
            clientId,
            redirectUrl,
            scopes,
            state,
            codeChallenge,
            codeChallengeMethod
        )
}

object ClientCredentialsGrant {

    suspend fun HttpClient.requestToken(tokenEndpointUrl: Url, clientId: String, clientSecret: String) =
        submitForm(
            tokenEndpointUrl.value,
            Parameters.build {
                append("client_id", clientId)
                append("client_secret", clientSecret)
                append("grant_type", "client_credentials")
            }
        ).body<Oauth2AuthorizationResponse>()
}
