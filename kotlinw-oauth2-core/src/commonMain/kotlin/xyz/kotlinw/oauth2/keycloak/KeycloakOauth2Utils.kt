package xyz.kotlinw.oauth2.keycloak

import kotlinw.util.stdlib.Url
import kotlinw.util.stdlib.trailingPathSeparatorRemoved

fun createKeycloakAuthorizationServerUrl(keycloakBaseUrl: Url, realm: String) =
    Url("${keycloakBaseUrl.trailingPathSeparatorRemoved().value}/realms/$realm")
