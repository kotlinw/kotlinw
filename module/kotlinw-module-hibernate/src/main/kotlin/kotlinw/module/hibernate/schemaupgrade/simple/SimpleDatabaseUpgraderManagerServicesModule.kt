package kotlinw.module.hibernate.schemaupgrade.simple

import kotlin.reflect.jvm.internal.impl.builtins.functions.FunctionTypeKind.KFunction
import kotlinw.configuration.core.ConfigurationPropertyLookup
import kotlinw.hibernate.core.schemaupgrade.DatabaseUpgradeManager
import kotlinw.hibernate.core.schemaupgrade.DatabaseUpgraderProvider
import kotlinw.hibernate.core.schemaupgrade.simple.SimpleDatabaseUpgradeManager
import kotlinw.hibernate.core.service.JpaPersistenceService
import kotlinw.logging.api.LoggerFactory
import kotlinw.util.stdlib.Priority
import kotlinw.util.stdlib.Priority.Companion.lowerBy
import org.hibernate.SessionFactory
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.di.impl.ContainerLifecycleCoordinator
import xyz.kotlinw.di.impl.registerListener

@Module
class SimpleDatabaseUpgraderManagerServicesModule {

    @Component
    fun databaseUpgradeManager(
        loggerFactory: LoggerFactory,
        jpaPersistenceService: JpaPersistenceService,
        databaseUpgraderProviders: List<DatabaseUpgraderProvider>,
        lifecycleCoordinator: ContainerLifecycleCoordinator,
        configurationPropertyLookup: ConfigurationPropertyLookup
    ): DatabaseUpgradeManager =
        SimpleDatabaseUpgradeManager(loggerFactory, jpaPersistenceService, databaseUpgraderProviders)
            .apply {
                lifecycleCoordinator.registerListener(
                    SimpleDatabaseUpgraderManagerServicesModule::databaseUpgradeManager,
                    {
                        upgradeSchema()
                        configurationPropertyLookup.reload()
                    },
                    {},
                    Priority.Highest.lowerBy(1000)
                )
            }
}
