package xyz.kotlinw.koin.core

import kotlinw.configuration.core.DeploymentMode.Development
import kotlinw.koin.core.api.coreModuleLogger
import kotlinw.koin.core.api.startContainer
import kotlinx.coroutines.delay
import org.koin.core.module.KoinApplicationDslMarker
import org.koin.core.module.Module
import org.koin.core.scope.Scope
import org.koin.dsl.module
import xyz.kotlinw.koin.container.KOIN_ROOT_SCOPE_ID

private val coreJsModule by lazy {
    module {
        single {
            val deploymentMode = Development
            coreModuleLogger.info { "Deployment mode: " / it }
            deploymentMode
        }
    }
}

@KoinApplicationDslMarker
suspend fun runJsApplication(
    vararg modules: Module,
    block: suspend Scope.() -> Unit = { delay(Long.MAX_VALUE) }
) {
    val koinApplication = startContainer({
        this.modules(coreJsModule, *modules)
    })

    // TODO koinApplication.close()

    try {
        koinApplication.koin.getScope(KOIN_ROOT_SCOPE_ID).block()
    } finally {
        koinApplication.close()
    }
}
