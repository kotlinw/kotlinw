package kotlinw.module.hibernate.schemaupgrade.simple

import kotlinw.hibernate.core.schemaupgrade.DatabaseUpgraderProvider
import kotlinw.hibernate.core.schemaupgrade.simple.SimpleDatabaseUpgradeManager
import org.hibernate.SessionFactory
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module

@Module
class SimpleDatabaseUpgraderManagerServicesModule {

    @Component(onConstruction = "upgradeSchema")
    fun databaseUpgradeManager(
        sessionFactory: SessionFactory,
        databaseUpgraderProviders: List<DatabaseUpgraderProvider>
    ) = SimpleDatabaseUpgradeManager(sessionFactory, databaseUpgraderProviders)

}

