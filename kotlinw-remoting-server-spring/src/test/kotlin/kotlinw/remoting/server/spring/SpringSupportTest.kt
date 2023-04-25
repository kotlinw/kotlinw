package kotlinw.remoting.server.spring

import io.ktor.client.*
import io.ktor.http.*
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinw.remoting.client.ktor.KtorRemotingHttpClientImplementor
import kotlinw.remoting.core.HttpRemotingClient
import kotlinw.remoting.core.MessageSerializerImpl
import kotlinw.remoting.processor.test.ExampleService
import kotlinw.remoting.processor.test.ExampleServiceClientProxy
import kotlinw.remoting.processor.test.ExampleServiceServerDelegate
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.web.servlet.config.annotation.EnableWebMvc
import kotlin.test.Test
import kotlin.test.assertEquals


@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT, classes = [RemotingServerSpringModule::class])
class SpringSupportTest {

    @Configuration
    @EnableAutoConfiguration
    @EnableWebMvc
    @Import(RemotingServerSpringModule::class)
    class TestSpringModule {

        @Bean
        fun exampleService(): ExampleService = mockk<ExampleService>(relaxed = true) {
            coEvery { p1IntReturnsString(123) } returns "abc"
        }

        @Bean
        fun exampleServiceReceivedCallProcessor() =
            ExampleServiceServerDelegate(exampleService(), MessageSerializerImpl(Json.Default))
    }

    @Value("\${local.server.port}")
    private var port: Int = -1

    @Autowired
    private lateinit var exampleService: ExampleService

    @Test
    fun test() {
        val proxy: ExampleService =
            ExampleServiceClientProxy(
                HttpRemotingClient(
                    Json.Default,
                    ContentType.Application.Json.toString(),
                    KtorRemotingHttpClientImplementor(),
                    "http://localhost:$port"
                )
            )

        runTest {
            assertEquals("abc", proxy.p1IntReturnsString(123))
        }

        coVerify {
            exampleService.p1IntReturnsString(123)
        }
    }
}
