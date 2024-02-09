package xyz.kotlinw.module.appbase.api

import kotlinw.configuration.core.DeploymentMode
import kotlinw.configuration.core.EnumerableConfigurationPropertyLookupSource
import kotlinw.configuration.core.EnumerableConfigurationPropertyLookupSourceImpl
import kotlinw.configuration.core.StandardJvmConfigurationPropertyResolver
import kotlinw.logging.api.LoggerFactory
import kotlinw.util.stdlib.Priority
import kotlinw.util.stdlib.Priority.Companion
import kotlinw.util.stdlib.Priority.Companion.higherBy
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.io.ClasspathScanner
import xyz.kotlinw.io.ClasspathScannerImpl

@Module(includeModules = [AppbaseModule::class])
class AppbaseJvmModule {

    @Component
    fun standardJvmConfigurationPropertyResolver(
        loggerFactory: LoggerFactory,
        classpathScanner: ClasspathScanner,
        deploymentMode: DeploymentMode
    ): EnumerableConfigurationPropertyLookupSource =
        EnumerableConfigurationPropertyLookupSourceImpl(
            StandardJvmConfigurationPropertyResolver(
                loggerFactory,
                classpathScanner,
                deploymentMode,
                Thread.currentThread().contextClassLoader
            ),
            Priority.Normal.higherBy(10_000)
        )

    @Component
    fun classpathScanner(): ClasspathScanner = ClasspathScannerImpl()
}
