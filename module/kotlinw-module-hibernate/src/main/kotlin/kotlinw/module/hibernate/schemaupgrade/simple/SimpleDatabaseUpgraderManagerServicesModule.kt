package kotlinw.module.hibernate.schemaupgrade.simple

import kotlinw.hibernate.core.schemaupgrade.DatabaseUpgraderProvider
import kotlinw.hibernate.core.schemaupgrade.simple.SimpleDatabaseUpgradeManager
import kotlinw.logging.api.LoggerFactory
import kotlinw.util.stdlib.Priority
import kotlinw.util.stdlib.Priority.Companion.lowerBy
import org.hibernate.SessionFactory
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.di.impl.ContainerLifecycleCoordinator

@Module
class SimpleDatabaseUpgraderManagerServicesModule {

    @Component
    fun databaseUpgradeManager(
        loggerFactory: LoggerFactory,
        sessionFactory: SessionFactory,
        databaseUpgraderProviders: List<DatabaseUpgraderProvider>,
        lifecycleCoordinator: ContainerLifecycleCoordinator
    ) =
        SimpleDatabaseUpgradeManager(loggerFactory, sessionFactory, databaseUpgraderProviders)
            .apply {
                lifecycleCoordinator.registerListener({ upgradeSchema() }, {}, Priority.Highest.lowerBy(1000))
            }
}
