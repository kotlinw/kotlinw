package xyz.kotlinw.module.serializer

import kotlinw.configuration.core.DeploymentMode
import kotlinw.serialization.core.SerializerService
import kotlinw.serialization.core.SerializerServiceImpl
import kotlinw.serialization.core.SerializersModuleContributor
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module

@Module
class SerializerModule {

    @Component
    fun serializerService(
        serializersModuleContributors: List<SerializersModuleContributor>,
        deploymentMode: DeploymentMode
    ): SerializerService = SerializerServiceImpl(
        serializersModuleContributors,
        deploymentMode == DeploymentMode.Development
    )
}
