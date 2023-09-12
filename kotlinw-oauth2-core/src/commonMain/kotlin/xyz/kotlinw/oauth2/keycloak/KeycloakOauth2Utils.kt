package xyz.kotlinw.oauth2.keycloak

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import kotlinw.util.stdlib.Url
import kotlinw.util.stdlib.trailingPathSeparatorRemoved
import xyz.kotlinw.oauth2.model.AccessToken

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
