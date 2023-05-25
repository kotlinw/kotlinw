package kotlinw.module.hibernate.schemaupgrade.simple

import kotlinw.hibernate.api.configuration.PersistentClassProvider
import kotlinw.hibernate.core.schemaupgrade.DatabaseUpgradeManager
import kotlinw.hibernate.core.schemaupgrade.simple.DatabaseSchemaVersionInfoEntity
import kotlinw.hibernate.core.schemaupgrade.simple.SimpleDatabaseUpgradeManager
import org.koin.dsl.bind
import org.koin.dsl.module

fun simpleDatabaseUpgraderManagerServicesModule() = module {
    single { SimpleDatabaseUpgradeManager(get(), getAll()) }.bind<DatabaseUpgradeManager>()
}

fun simpleDatabaseUpgraderManagerEntitiesModule() = module {
    single { PersistentClassProvider { listOf(DatabaseSchemaVersionInfoEntity::class) } }.bind<PersistentClassProvider>()
}
