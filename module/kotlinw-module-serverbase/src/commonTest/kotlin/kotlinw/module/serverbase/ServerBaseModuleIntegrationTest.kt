package kotlinw.module.serverbase

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.server.application.call
import io.ktor.server.cio.CIO
import io.ktor.server.engine.ApplicationEngineFactory
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinw.configuration.core.ConfigurationPropertyLookupSource
import kotlinw.configuration.core.ConstantConfigurationPropertyResolver
import kotlinw.configuration.core.EnumerableConfigurationPropertyLookupSource
import kotlinw.configuration.core.EnumerableConfigurationPropertyLookupSourceImpl
import kotlinw.koin.core.api.startKoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.withOptions
import org.koin.core.qualifier.named
import org.koin.dsl.module

class ServerBaseModuleIntegrationTest {

    @Test
    fun testBootstrap() {
        val host = "localhost"
        val port = 8080

        val koinApplication = startKoin {
            modules(
                serverBaseModule,
                module {
                    single(named("testConfigurationPropertyLookup")) {
                        EnumerableConfigurationPropertyLookupSourceImpl(
                            ConstantConfigurationPropertyResolver.of(
                                "kotlinw.serverbase.host" to host,
                                "kotlinw.serverbase.port" to port.toString()
                            )
                        )
                    }.withOptions {
                        bind<ConfigurationPropertyLookupSource>()
                        bind<EnumerableConfigurationPropertyLookupSource>()
                    }
                    single<ApplicationEngineFactory<*, *>> { CIO }
                    single {
                        KtorServerApplicationConfigurer {
                            application.routing {
                                get("/test") {
                                    call.respondText("test-response")
                                }
                            }
                        }
                    }
                }
            )
        }

        runBlocking {
            delay(500)
            assertEquals("test-response", HttpClient().get("http://$host:$port/test").bodyAsText())
            delay(500)
        }

        try {
        } finally {
            koinApplication.close()
        }
    }
}
