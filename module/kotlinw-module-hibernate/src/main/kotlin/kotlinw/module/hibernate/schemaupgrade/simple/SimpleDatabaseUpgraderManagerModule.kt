package kotlinw.module.hibernate.schemaupgrade.simple

import kotlinw.hibernate.api.configuration.PersistentClassProvider
import kotlinw.hibernate.core.schemaupgrade.DatabaseUpgradeManager
import kotlinw.hibernate.core.schemaupgrade.simple.DatabaseSchemaVersionInfoEntity
import kotlinw.hibernate.core.schemaupgrade.simple.SimpleDatabaseUpgradeManager
import kotlinw.module.api.ApplicationInitializerService
import kotlinw.util.stdlib.Priority
import kotlinw.util.stdlib.Priority.Companion.higherBy
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module

val simpleDatabaseUpgraderManagerServicesModule by lazy {
    module {
        single<DatabaseUpgradeManager> { SimpleDatabaseUpgradeManager(get(), getAll()) }

        single<ApplicationInitializerService>(named("DatabaseUpgradeManagerApplicationInitializerService")) {
            ApplicationInitializerService(Priority.Normal.higherBy(100)) {
                get<DatabaseUpgradeManager>().upgradeSchema()
            }
        }
    }
}

val simpleDatabaseUpgraderManagerEntitiesModule by lazy {
    module {
        single(named("kotlinw.module.hibernate.schemaupgrade.simple.PersistentClassProvider")) { PersistentClassProvider { listOf(DatabaseSchemaVersionInfoEntity::class) } }.bind<PersistentClassProvider>()
    }
}
