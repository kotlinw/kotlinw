package xyz.kotlinw.module.test

import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.TestApplicationBuilder
import io.ktor.server.testing.testApplication
import kotlin.DeprecationLevel.ERROR
import kotlinw.configuration.core.DeploymentMode.Development
import xyz.kotlinw.di.api.ComponentQuery
import xyz.kotlinw.di.api.ContainerScope
import xyz.kotlinw.di.api.runApplication
import xyz.kotlinw.module.ktor.server.KtorServerApplicationConfigurer
import xyz.kotlinw.module.ktor.server.KtorServerModule

@Deprecated(
    level = ERROR,
    message = "Do not call `runTest()` from `testApplication(), `runTest()` without receiver will call it implicitly."
)
fun <T : TestScopeBase> ApplicationTestBuilder.runTest(
    rootScopeFactory: () -> T,
    block: suspend T.() -> Unit
) {
    throw UnsupportedOperationException()
}

fun <T : TestScopeBase> runTest(
    rootScopeFactory: ApplicationTestBuilder.() -> T,
    setupTestApplication: TestApplicationBuilder.() -> Unit = {},
    block: suspend T.() -> Unit
) =
    testApplication {
        setupTestApplication()
        runApplication(
            rootScopeFactory = { rootScopeFactory() },
            block = {
                this@testApplication.application {
                    KtorServerModule.initializeKtorServerApplication(
                        this,
                        Development,
                        this,
                        ktorServerApplicationConfigurers()
                    )
                }
                block()
            }
        )
    }

interface TestScopeBase : ContainerScope {

    @ComponentQuery
    fun ktorServerApplicationConfigurers(): List<KtorServerApplicationConfigurer>
}
