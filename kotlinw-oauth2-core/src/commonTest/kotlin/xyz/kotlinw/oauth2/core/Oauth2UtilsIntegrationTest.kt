package xyz.kotlinw.oauth2.core

import arrow.core.raise.recover
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel.INFO
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.submitForm
import io.ktor.http.Parameters
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

class Oauth2UtilsIntegrationTest {

    private fun createHttpClient(): HttpClient {
        val client = HttpClient {
            install(Logging) {
                level = INFO
            }
        }
        return client
    }

    @Test
    fun testGetOpenidConfigurationMetadata(): TestResult {
        return runTest {
            createHttpClient().fetchOpenidConnectProviderMetadata(Url("https://id.erinors.com/realms/erinors"))
                .also {
                    println(it)
                    assertEquals(
                        Url("https://id.erinors.com/realms/erinors/protocol/openid-connect/auth"),
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
                httpClient.fetchOpenidConnectProviderMetadata(Url("https://id.erinors.com/realms/erinors"))
            val tokenEndpoint = providerMetadata.tokenEndpoint ?: fail()
            val deviceAuthorizationEndpoint = providerMetadata.deviceAuthorizationEndpoint
                ?: fail("OpenID provider does not support Device Authorization Grant")

            val tokenResponse =
                httpClient.authorizeDevice(deviceAuthorizationEndpoint, tokenEndpoint, "kotlinw-test-device") {
                    println("To successfully perform the test, authenticate using Google at: ${it.verificationUriComplete} (userCode=${it.userCode})")
                }

            println(tokenResponse)

            val tokenExchangeResponse = httpClient.submitForm(
                tokenEndpoint.value,
                Parameters.build {
                    append("client_id", "kotlinw-test-device")
                    append("subject_token", tokenResponse.accessToken)
                    append("grant_type", "urn:ietf:params:oauth:grant-type:token-exchange")
                    append("requested_token_type", "urn:ietf:params:oauth:token-type:access_token")
                    append("requested_issuer", "google")
                }
            )

            println(tokenExchangeResponse.body<OAuth2TokenResponse>())
        }
    }

    @Test
    fun testClientCredentialsGrant() = runTest {
        val httpClient = createHttpClient()
        val response =
            recover({
                ClientCredentialsGrant.requestToken(
                    httpClient,
                    Url("https://id.erinors.com/realms/erinors/protocol/openid-connect/token"),
                    "",
                    ""
                )
            },{
                fail(it.toString())
            })
        println(response.decodeJwtAccessToken())
    }
}
