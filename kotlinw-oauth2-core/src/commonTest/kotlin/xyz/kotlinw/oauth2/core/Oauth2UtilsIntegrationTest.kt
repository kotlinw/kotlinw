package xyz.kotlinw.oauth2.core

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel.NONE
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.DefaultJson
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinw.util.stdlib.Url
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json

class Oauth2UtilsIntegrationTest {

    private fun createHttpClient(): HttpClient {
        val client = HttpClient {
            install(ContentNegotiation) {
                json(Json(DefaultJson) {
                    ignoreUnknownKeys = true
                })
            }
            install(Logging) {
                level = NONE
            }
        }
        return client
    }

    @Test
    fun testGetOpenidConfigurationMetadata() = runTest {
        createHttpClient().fetchOpenidConnectProviderMetadata(Url("https://sso.erinors.com/realms/erinors/.well-known/openid-configuration"))
            .also {
                assertEquals(
                    Url("https://sso.erinors.com/realms/erinors/protocol/openid-connect/auth"),
                    it.authorizationEndpoint
                )
            }
        createHttpClient().fetchOpenidConnectProviderMetadata(Url("https://accounts.google.com/.well-known/openid-configuration"))
            .also {
                assertEquals(Url("https://accounts.google.com/o/oauth2/v2/auth"), it.authorizationEndpoint)
            }
    }
}
