package xyz.kotlinw.oauth2.core

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel.ALL
import io.ktor.client.plugins.logging.LogLevel.INFO
import io.ktor.client.plugins.logging.LogLevel.NONE
import io.ktor.client.plugins.logging.Logging
import io.ktor.serialization.kotlinx.json.DefaultJson
import io.ktor.serialization.kotlinx.json.json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import kotlin.time.Duration.Companion.minutes
import kotlinw.util.stdlib.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.TestResult
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import xyz.kotlinw.oauth2.keycloak.createKeycloakAuthorizationServerUrl

class Oauth2UtilsIntegrationTest {

    private val testAuthorizationServerUrl = createKeycloakAuthorizationServerUrl(Url("https://sso.erinors.com"), "erinors")

    private fun createHttpClient(): HttpClient {
        val client = HttpClient {
            install(ContentNegotiation) {
                json(Json(DefaultJson) {
                    ignoreUnknownKeys = true
                })
            }
            install(Logging) {
                level = INFO
            }
        }
        return client
    }

    @Test
    fun testGetOpenidConfigurationMetadata(): TestResult {
        return runTest {
            createHttpClient().fetchOpenidConnectProviderMetadata(testAuthorizationServerUrl)
                .also {
                    assertEquals(
                        Url("https://sso.erinors.com/realms/erinors/protocol/openid-connect/auth"),
                        it.authorizationEndpoint
                    )
                }
            createHttpClient().fetchOpenidConnectProviderMetadata(Url("https://accounts.google.com"))
                .also {
                    assertEquals(Url("https://accounts.google.com/o/oauth2/v2/auth"), it.authorizationEndpoint)
                }
        }
    }

    @Test
    fun testDeviceAuthorizationGrant() = runTest(timeout = 1.minutes) {
        withContext(Dispatchers.Default) {
            val httpClient = createHttpClient()

            val providerMetadata =
                httpClient.fetchOpenidConnectProviderMetadata(testAuthorizationServerUrl)
            val tokenEndpoint = providerMetadata.tokenEndpoint ?: fail()
            val deviceAuthorizationEndpoint = providerMetadata.deviceAuthorizationEndpoint
                ?: fail("OpenID provider does not support Device Authorization Grant")

            httpClient.authorizeDevice(deviceAuthorizationEndpoint, tokenEndpoint, "kotlinw-test-device") {
                println("To successfully perform the test, authorize at: ${it.verificationUriComplete} (userCode=${it.userCode})")
            }
        }
    }
}
