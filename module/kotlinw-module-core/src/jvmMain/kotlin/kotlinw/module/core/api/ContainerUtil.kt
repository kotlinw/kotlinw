package kotlinw.module.core.api

import kotlinw.koin.core.api.startKoin
import org.koin.core.KoinApplication
import org.koin.core.module.KoinApplicationDslMarker
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.koinApplication

@KoinApplicationDslMarker
fun runApplication(vararg modules: Module) {
    val koinApplication = startKoin {
        this.modules(*modules)
    }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            koinApplication.close()
        }
    )

    // TODO
    runCatching {
        Thread.sleep(Long.MAX_VALUE)
    }
}
