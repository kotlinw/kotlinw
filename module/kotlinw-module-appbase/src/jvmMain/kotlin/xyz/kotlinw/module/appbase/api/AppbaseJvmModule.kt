package xyz.kotlinw.module.appbase.api

import kotlinw.configuration.core.DeploymentMode
import kotlinw.configuration.core.EnumerableConfigurationPropertyLookupSource
import kotlinw.configuration.core.EnumerableConfigurationPropertyLookupSourceImpl
import kotlinw.configuration.core.StandardJvmConfigurationPropertyResolver
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.io.ClasspathScanner
import xyz.kotlinw.io.ClasspathScannerImpl

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
