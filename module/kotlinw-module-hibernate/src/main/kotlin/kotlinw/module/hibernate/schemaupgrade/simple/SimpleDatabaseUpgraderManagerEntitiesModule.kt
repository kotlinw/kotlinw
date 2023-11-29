package kotlinw.module.hibernate.schemaupgrade.simple

import kotlinw.hibernate.api.configuration.PersistentClassProvider
import kotlinw.hibernate.core.schemaupgrade.simple.DatabaseSchemaVersionInfoEntity
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module

@Module
class SimpleDatabaseUpgraderManagerEntitiesModule {

    @Component
    fun entities() = PersistentClassProvider { listOf(DatabaseSchemaVersionInfoEntity::class) }
}