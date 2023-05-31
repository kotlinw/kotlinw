package kotlinw.module.core.api

import kotlinw.configuration.core.ConfigurationPropertyLookupSource
import kotlinw.configuration.core.ConfigurationPropertyLookupSourceImpl
import kotlinw.configuration.core.ConfigurationPropertyResolver
import kotlinw.configuration.core.EmptyConfigurationPropertyResolver
import kotlinw.configuration.core.EnumerableConfigurationPropertyLookupSourceImpl
import kotlinw.configuration.core.EnumerableConfigurationPropertyResolver
import kotlinw.configuration.core.JavaPropertiesConfigurationPropertyResolver
import kotlinw.configuration.core.JavaPropertiesFileConfigurationPropertyResolver
import kotlinw.koin.core.api.startKoin
import org.koin.core.KoinApplication
import org.koin.core.module.KoinApplicationDslMarker
import org.koin.core.module.Module
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.withOptions
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import java.util.Properties

@KoinApplicationDslMarker
inline fun <reified T> runApplication(vararg modules: Module) {
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
        }
    )

    // TODO
    runCatching {
        Thread.sleep(Long.MAX_VALUE)
    }
}
