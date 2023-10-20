package kotlinw.koin.core.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import kotlin.time.Duration.Companion.seconds
import kotlinw.configuration.core.ConfigurationObjectLookup
import kotlinw.configuration.core.ConfigurationObjectLookupImpl
import kotlinw.configuration.core.ConfigurationPropertyLookup
import kotlinw.configuration.core.ConfigurationPropertyLookupImpl
import kotlinw.eventbus.local.LocalEventBus
import kotlinw.eventbus.local.LocalEventBusImpl
import kotlinw.koin.core.internal.ApplicationCoroutineServiceImpl
import kotlinw.koin.core.internal.defaultLoggingIntegrator
import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import kotlinw.logging.spi.LoggingConfigurationProvider
import kotlinw.logging.spi.LoggingContextManager
import kotlinw.logging.spi.LoggingDelegator
import kotlinw.logging.spi.LoggingIntegrator
import kotlinw.module.api.ApplicationInitializerService
import kotlinw.remoting.client.ktor.KtorHttpRemotingClientImplementor
import kotlinw.remoting.core.codec.JsonMessageCodec
import kotlinw.remoting.core.common.BidirectionalCommunicationImplementor
import kotlinw.remoting.core.common.MutableRemotePeerRegistry
import kotlinw.remoting.core.common.RemotePeerRegistry
import kotlinw.remoting.core.common.RemotePeerRegistryImpl
import kotlinw.remoting.core.common.SynchronousCallSupport
import kotlinw.serialization.core.SerializerService
import kotlinw.serialization.core.SerializerServiceImpl
import kotlinw.util.stdlib.Priority
import kotlinw.util.stdlib.Priority.Companion.higherBy
import kotlinw.util.stdlib.Priority.Companion.lowerBy
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import org.koin.core.KoinApplication
import org.koin.core.module.KoinApplicationDslMarker
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.withOptions
import org.koin.core.qualifier.named
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.bind
import org.koin.dsl.module
import org.koin.dsl.onClose
import xyz.kotlinw.koin.container.ContainerShutdownCoordinator
import xyz.kotlinw.koin.container.ContainerShutdownCoordinatorImpl
import xyz.kotlinw.koin.container.ContainerStartupCoordinator
import xyz.kotlinw.koin.container.ContainerStartupCoordinatorImpl
import xyz.kotlinw.koin.container.getAllSortedByPriority
import xyz.kotlinw.koin.container.registerShutdownTask

@PublishedApi
internal val coreModuleLogger by lazy { PlatformLogging.getLogger() }

val coreModule by lazy {
    module {
        single<ContainerStartupCoordinator> { ContainerStartupCoordinatorImpl() }
        single<ApplicationInitializerService>(named("coreModule.ContainerStartupCoordinatorApplicationInitializerService")) {
            ApplicationInitializerService(Priority.Normal.higherBy(90)) {
                get<ContainerStartupCoordinator>().runStartupTasks()
            }
        }

        single<ContainerShutdownCoordinator> { ContainerShutdownCoordinatorImpl() }.onClose { it?.close() }

        single<LoggingIntegrator> { defaultLoggingIntegrator } withOptions {
            bind<LoggingConfigurationProvider>()
            bind<LoggerFactory>()
            bind<LoggingDelegator>()
            bind<LoggingContextManager>()
        }
        single<ApplicationCoroutineService>(createdAtStart = true) {
            ApplicationCoroutineServiceImpl()
                .registerShutdownTask(this) {
                    it.close()
                }
        }

        single<ConfigurationPropertyLookup>(createdAtStart = true) {
            ConfigurationPropertyLookupImpl(getAll())
        }
        single<ApplicationInitializerService>(named("coreModule.ConfigurationPropertyLookupInitializer")) {
            ApplicationInitializerService(Priority.Highest.lowerBy(100)) {
                get<ConfigurationPropertyLookup>().initialize()
            }
        }

        single<ConfigurationObjectLookup> { ConfigurationObjectLookupImpl(get()) }

        single<LocalEventBus> {
            LocalEventBusImpl(1000) // TODO config
        }

        single<SerializerService> { SerializerServiceImpl() }

        // TODO az alábbiakat külön modulba
        single<HttpClient> {
            HttpClient {
                install(HttpTimeout) {
                    connectTimeoutMillis = 3.seconds.inWholeMilliseconds // TODO config
                    requestTimeoutMillis = 10.seconds.inWholeMilliseconds // TODO config
                }
            }
        } // TODO close()-zal le kell zárni
        single { KtorHttpRemotingClientImplementor(get<HttpClient>()) } withOptions {
            bind<SynchronousCallSupport>()
            bind<BidirectionalCommunicationImplementor>()
        }
        single { RemotingClientFactoryImpl(JsonMessageCodec.Default, get()) }.bind<RemotingClientFactory>()
        single { RemotePeerRegistryImpl() }.withOptions {
            bind<RemotePeerRegistry>()
            bind<MutableRemotePeerRegistry>()
        }
    }
}

@KoinApplicationDslMarker
suspend fun startContainer(
    appDeclaration: KoinAppDeclaration,
    onUninitializedKoinApplicationInstanceCreated: (KoinApplication) -> Unit = {}
): KoinApplication {
    val koinApplication = coroutineScope {
        org.koin.core.context.startKoin {
            allowOverride(false)
            onUninitializedKoinApplicationInstanceCreated(this)

            modules(coreModule)
            appDeclaration()

            launch {
                koin.getAllSortedByPriority<ApplicationInitializerService>().forEach {
                    try {
                        it.performInitialization()
                    } catch (e: Exception) {
                        // TODO log
                        e.printStackTrace()
                        throw e
                    }
                }
            }
        }
    }

    return koinApplication
}
