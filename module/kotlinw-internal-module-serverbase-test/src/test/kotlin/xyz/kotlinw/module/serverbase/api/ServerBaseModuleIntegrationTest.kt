package xyz.kotlinw.module.serverbase.api

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinw.configuration.core.ConstantConfigurationPropertyResolver
import kotlinw.configuration.core.EnumerableConfigurationPropertyLookupSourceImpl
import kotlinw.remoting.core.codec.JsonMessageCodec
import kotlinw.remoting.core.codec.MessageCodec
import kotlinw.remoting.server.ktor.RemotingClientAuthenticator
import kotlinw.uuid.Uuid
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Container
import xyz.kotlinw.di.api.ContainerScope
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.di.api.Scope
import xyz.kotlinw.module.ktor.server.KtorServerApplicationConfigurer
import xyz.kotlinw.remoting.api.MessagingPeerId

const val host = "localhost"
const val port = 8080

class ServerBaseModuleIntegrationTest {

    @Container
    interface TestContainer {

        companion object

        interface TestScope : ContainerScope

        @Scope(modules = [TestModule::class])
        fun testScope(): TestScope

        @Module(includeModules = [ServerBaseJvmModule::class])
        class TestModule {

            @Component
            fun configuration() =
                EnumerableConfigurationPropertyLookupSourceImpl(
                    ConstantConfigurationPropertyResolver.of(
                        "kotlinw.serverbase.host" to host,
                        "kotlinw.serverbase.port" to port.toString()
                    )
                )

            @Component
            fun messageCodec(): MessageCodec<*> = JsonMessageCodec.Default

            @Component
            fun testController() =
                KtorServerApplicationConfigurer {
                    ktorApplication.routing {
                        get("/test") {
                            call.respondText("test-response")
                        }
                    }
                }

            @Component
            fun remotingClientAuthenticator() = object : RemotingClientAuthenticator {

                override fun authenticateClient(call: ApplicationCall): MessagingPeerId = Uuid.randomUuid()
            }
        }
    }

    @Test
    fun testBootstrap() = runTest {

        TestContainer.create().testScope().apply {
            try {
                start()

                delay(500)
                assertEquals("test-response", HttpClient().get("http://$host:$port/test").bodyAsText())
                delay(500)

            } finally {
                close()
            }
        }
    }
}
