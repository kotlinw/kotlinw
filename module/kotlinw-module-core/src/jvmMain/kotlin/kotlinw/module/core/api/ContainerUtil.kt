package kotlinw.module.core.api

import kotlinw.configuration.core.ConfigurationPropertyLookupSource
import kotlinw.configuration.core.EmptyConfigurationPropertyResolver
import kotlinw.configuration.core.EnumerableConfigurationPropertyLookupSourceImpl
import kotlinw.configuration.core.JavaPropertiesConfigurationPropertyResolver
import kotlinw.koin.core.api.startKoin
import org.koin.core.module.KoinApplicationDslMarker
import org.koin.core.module.Module
import org.koin.dsl.module
import java.io.File
import java.util.Properties
import kotlin.concurrent.thread

const val applicationPidFileName = "pid"

fun pidFile() = System.getenv("KOTLINW_APPLICATION_BASE_DIRECTORY")?.let { File(it, applicationPidFileName) }

// TODO move to a better project/place
fun createPidFile() {
    pidFile()?.also {
        if (it.exists()) {
            it.delete()
        }

        it.writeText(ProcessHandle.current().pid().toString())
    }
}

// TODO move to a better project/place
fun deletePidFile() {
    pidFile()?.delete()
}

@KoinApplicationDslMarker
inline fun <reified T> runApplication(vararg modules: Module) {
    createPidFile()

    val koinApplication = startKoin {
        this.modules(
            *modules,
            module {
                single<ConfigurationPropertyLookupSource> {
                    val properties =
                        T::class.java.getResourceAsStream("/kotlinw.properties")?.use {
                            Properties().apply { load(it) }
                        }

                    EnumerableConfigurationPropertyLookupSourceImpl(
                        if (properties != null) {
                            JavaPropertiesConfigurationPropertyResolver(properties)
                        } else {
                            EmptyConfigurationPropertyResolver
                        }
                    )
                }
            }
        )
    }

    Runtime.getRuntime().addShutdownHook(
        Thread {
            koinApplication.close()
            deletePidFile()
        }
    )

    // TODO
    runCatching {
        Thread.sleep(Long.MAX_VALUE)
    }
}
