package xyz.kotlinw.hibernate.configuration.entity

import kotlinw.hibernate.api.configuration.PersistentClassProvider
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.ComponentScan
import xyz.kotlinw.di.api.Module

@Module
@ComponentScan
class ApplicationConfigurationEntitiesModule {

    @Component
    fun entities() = PersistentClassProvider { listOf(ApplicationConfigurationEntity::class) }
}
