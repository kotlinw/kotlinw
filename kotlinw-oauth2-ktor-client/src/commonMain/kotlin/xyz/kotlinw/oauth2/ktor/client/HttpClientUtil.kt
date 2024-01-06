package xyz.kotlinw.oauth2.ktor.client

import io.ktor.client.HttpClient
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BearerAuthProvider
import io.ktor.client.plugins.pluginOrNull

// TODO move to more general place
fun HttpClient.cleanBearerTokens() =
    pluginOrNull(Auth)?.providers?.filterIsInstance<BearerAuthProvider>()?.firstOrNull()?.clearToken()
