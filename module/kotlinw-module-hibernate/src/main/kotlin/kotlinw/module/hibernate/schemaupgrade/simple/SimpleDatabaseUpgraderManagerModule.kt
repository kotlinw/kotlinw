package kotlinw.module.hibernate.schemaupgrade.simple

import kotlinw.hibernate.api.configuration.PersistentClassProvider
import kotlinw.hibernate.core.schemaupgrade.DatabaseUpgradeManager
import kotlinw.hibernate.core.schemaupgrade.simple.DatabaseSchemaVersionInfoEntity
import kotlinw.hibernate.core.schemaupgrade.simple.SimpleDatabaseUpgradeManager
import kotlinw.koin.core.internal.ContainerStartupCoordinator
import kotlinw.module.api.ApplicationInitializerService
import kotlinw.util.stdlib.Priority
import kotlinw.util.stdlib.Priority.Companion.higherBy
import kotlinw.util.stdlib.Priority.Companion.lowerBy
import org.koin.dsl.bind
import org.koin.dsl.module

val simpleDatabaseUpgraderManagerServicesModule by lazy {
    module {
        single<DatabaseUpgradeManager> { SimpleDatabaseUpgradeManager(get(), getAll()) }

        single {
            ApplicationInitializerService(Priority.Normal.higherBy(100)) {
                get<DatabaseUpgradeManager>().upgradeSchema()
            }
        }.bind<ApplicationInitializerService>()
    }
}

val simpleDatabaseUpgraderManagerEntitiesModule by lazy {
    module {
        single { PersistentClassProvider { listOf(DatabaseSchemaVersionInfoEntity::class) } }.bind<PersistentClassProvider>()
    }
}
