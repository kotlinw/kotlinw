package xyz.kotlinw.module.appbase.api

import kotlinw.configuration.core.DeploymentMode
import kotlinw.configuration.core.DeploymentMode.Development
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module

@Module
actual class AppbasePlatformModule {

    @Component
    actual fun deploymentMode(): DeploymentMode = Development // TODO
}
