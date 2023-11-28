package xyz.kotlinw.module.appbase.api

import kotlinw.configuration.core.ConfigurationPropertyLookupSource
import kotlinw.configuration.core.DeploymentMode
import kotlinw.configuration.core.EnumerableConfigurationPropertyLookupSource
import kotlinw.configuration.core.EnumerableConfigurationPropertyLookupSourceImpl
import kotlinw.configuration.core.StandardJvmConfigurationPropertyResolver
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.io.ClasspathScanner
import xyz.kotlinw.io.ClasspathScannerImpl
import kotlin.reflect.KClass
import kotlinw.koin.core.internal.createPidFile
import kotlinw.koin.core.internal.deletePidFile
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import xyz.kotlinw.di.api.ContainerScope

@Module(includeModules = [AppbaseModule::class])
class AppbaseJvmModule {

    @Component
    fun standardJvmConfigurationPropertyResolver(
        classpathScanner: ClasspathScanner,
        deploymentMode: DeploymentMode
    ): EnumerableConfigurationPropertyLookupSource =
        EnumerableConfigurationPropertyLookupSourceImpl(
            StandardJvmConfigurationPropertyResolver(
                classpathScanner,
                deploymentMode,
                Thread.currentThread().contextClassLoader
            )
        )

    @Component
    fun classpathScanner(): ClasspathScanner = ClasspathScannerImpl()
}

suspend fun <T: ContainerScope> runJvmApplication(
    rootScopeFactory: () -> T,
    args: Array<out String> = emptyArray(),
    block: suspend T.() -> Unit = { delay(Long.MAX_VALUE) }
) {
    createPidFile()

    val rootScope = rootScopeFactory()

    Runtime.getRuntime().addShutdownHook(
        Thread {
            try {
                runBlocking {
                    rootScope.close()
                }
            } finally {
                deletePidFile()
            }
        }
    )

    with(rootScope) {
        try {
            start()
            block()
        } finally {
            close()
        }
    }
}
