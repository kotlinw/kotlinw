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
import kotlinw.koin.core.api.startKoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals

class ServerBaseModuleIntegrationTest {

    @Test
    fun testBootstrap() {
        val koinApplication = startKoin {
            modules(
                serverBaseModule,
                module {
                    single<ApplicationEngineFactory<*, *>> { CIO }
                    single {
                        KtorServerApplicationConfigurer {
                            routing {
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
            assertEquals("test-response", HttpClient().get("http://localhost:8080/test").bodyAsText())
            delay(500)
        }

        try {
        } finally {
            koinApplication.close()
        }
    }
}
