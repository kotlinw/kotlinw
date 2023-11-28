package xyz.kotlinw.module.appbase.api

import kotlinw.configuration.core.DeploymentMode
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.koin.core.api.CoreModule

@Module(includeModules = [CoreModule::class, AppbasePlatformModule::class])
class AppbaseModule

@Module
expect class AppbasePlatformModule {

    @Component
    fun deploymentMode(): DeploymentMode
}
