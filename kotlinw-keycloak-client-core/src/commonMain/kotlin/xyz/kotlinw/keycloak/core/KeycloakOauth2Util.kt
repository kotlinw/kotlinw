package xyz.kotlinw.keycloak.core

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import kotlinw.util.stdlib.Url
import kotlinw.util.stdlib.trailingPathSeparatorRemoved
import xyz.kotlinw.jwt.model.JwtToken.JwtTokenPayload
import xyz.kotlinw.oauth2.core.AccessToken

fun createKeycloakAuthorizationServerUrl(keycloakBaseUrl: Url, realm: String) =
    Url("${keycloakBaseUrl.trailingPathSeparatorRemoved().value}/realms/$realm")

suspend fun HttpClient.fetchKeycloakExternalIdentityProviderToken(
    keycloakBaseUrl: Url,
    realm: String,
    providerAlias: String,
    keycloakAccessToken: AccessToken
) =
    fetchKeycloakExternalIdentityProviderToken(
        createKeycloakAuthorizationServerUrl(keycloakBaseUrl, realm), providerAlias, keycloakAccessToken
    )

suspend fun HttpClient.fetchKeycloakExternalIdentityProviderToken(
    keycloakRealmBaseUrl: Url,
    providerAlias: String,
    keycloakAccessToken: AccessToken
) =
    get(keycloakRealmBaseUrl.value + "/broker/$providerAlias/token") {
        bearerAuth(keycloakAccessToken)
    }.body<String>()

val JwtTokenPayload.allowedOrigins get() = jsonObject["allowed-origins"]

val JwtTokenPayload.realmAccess get() = jsonObject["realm_access"]

val JwtTokenPayload.resourceAccess get() = jsonObject["resource_access"]
