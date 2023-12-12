package xyz.kotlinw.oauth2.server

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.http.HttpMethod
import io.ktor.http.URLBuilder
import io.ktor.server.application.ApplicationCall
import io.ktor.server.auth.OAuthServerSettings
import io.ktor.util.GenerateOnlyNonceManager
import io.ktor.util.NonceManager
import xyz.kotlinw.oauth2.core.OpenidConnectProviderMetadata
import xyz.kotlinw.oauth2.core.tokenEndpointOrThrow

fun OAuth2ServerSettings(
    authorizationServerMetadata: OpenidConnectProviderMetadata,
    requestMethod: HttpMethod = HttpMethod.Get,
    clientId: String,
    clientSecret: String,
    defaultScopes: List<String> = emptyList(),
    accessTokenRequiresBasicAuth: Boolean = false,
    nonceManager: NonceManager = GenerateOnlyNonceManager,
    authorizeUrlInterceptor: URLBuilder.() -> Unit = {},
    passParamsInURL: Boolean = false,
    extraAuthParameters: List<Pair<String, String>> = emptyList(),
    extraTokenParameters: List<Pair<String, String>> = emptyList(),
    accessTokenInterceptor: HttpRequestBuilder.() -> Unit = {},
    onStateCreated: suspend (call: ApplicationCall, state: String) -> Unit = { _, _ -> }
) =
    OAuthServerSettings.OAuth2ServerSettings(
        authorizationServerMetadata.issuer.value,
        authorizationServerMetadata.authorizationEndpoint.value,
        authorizationServerMetadata.tokenEndpointOrThrow.value,
        requestMethod,
        clientId,
        clientSecret,
        defaultScopes,
        accessTokenRequiresBasicAuth,
        nonceManager,
        authorizeUrlInterceptor,
        passParamsInURL,
        extraAuthParameters,
        extraTokenParameters,
        accessTokenInterceptor,
        onStateCreated
    )
