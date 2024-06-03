package kotlinw.module.hibernate.core

import kotlinw.configuration.core.ConfigurationPropertyLookup
import kotlinw.configuration.core.startsWith
import kotlinw.hibernate.api.configuration.PersistentClassProvider
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import org.hibernate.SessionFactory
import org.hibernate.boot.Metadata
import org.hibernate.boot.MetadataBuilder
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.SessionFactoryBuilder
import org.hibernate.boot.registry.BootstrapServiceRegistry
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder
import org.hibernate.boot.registry.StandardServiceRegistry
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.ComponentScan
import xyz.kotlinw.di.api.Module
import xyz.kotlinw.module.core.CoreModule

fun interface BootstrapServiceRegistryCustomizer {

    context(BootstrapServiceRegistryBuilder)
    fun customize()
}

fun interface StandardServiceRegistryCustomizer {

    context(StandardServiceRegistryBuilder)
    fun customize()
}

fun interface MetadataSourcesCustomizer {

    context(MetadataSources)
    fun customize()
}

fun interface MetadataCustomizer {

    context(MetadataBuilder)
    fun customize()
}

fun interface SessionFactoryCustomizer {

    context(SessionFactoryBuilder)
    fun customize()
}

@Module(includeModules = [CoreModule::class])
@ComponentScan
class HibernateModule {

    @Component(onTerminate = "close")
    fun bootstrapServiceRegistry(customizers: List<BootstrapServiceRegistryCustomizer>): BootstrapServiceRegistry =
        BootstrapServiceRegistryBuilder()
            .apply {
                customizers.forEach { it.customize() }
            }
            .build()

    @Component(onTerminate = "close")
    fun standardServiceRegistry(
        bootstrapServiceRegistry: BootstrapServiceRegistry,
        configurationPropertyLookup: ConfigurationPropertyLookup,
        customizers: List<StandardServiceRegistryCustomizer>
    ): StandardServiceRegistry =
        StandardServiceRegistryBuilder(bootstrapServiceRegistry)
            .apply {
                configurationPropertyLookup
                    .filterEnumerableConfigurationProperties { it.startsWith("hibernate") }
                    .also {
                        PlatformLogging.getLogger()
                            .info { "Hibernate configuration properties: " / it } // TODO egységes modul szintű logger
                    }
                    .forEach {
                        applySetting(it.key.name, it.value)
                    }
                    .also {
                        applySetting("hibernate.id.optimizer.pooled.preferred", "pooled-lo") // TODO make configurable
                        applySetting("hibernate.jdbc.batch_size", "10") // TODO make configurable
                        applySetting("hibernate.order_inserts", "true") // TODO make configurable
                        applySetting("hibernate.order_updates", "true") // TODO make configurable
                        applySetting("hibernate.jdbc.fetch_size", "100") // TODO make configurable
                    }

                customizers.forEach { it.customize() }
            }
            .build()

    @Component
    fun metadata(metadataSources: MetadataSources, customizers: List<MetadataCustomizer>): Metadata =
        metadataSources
            .metadataBuilder
            .apply {
                customizers.forEach { it.customize() }
            }
            .build()

    @Component
    fun persistentClassProviderApplier(persistentClassProviders: List<PersistentClassProvider>) =
        MetadataSourcesCustomizer {
            val packages = mutableSetOf<String>()

            persistentClassProviders.forEach {
                it.getPersistentClasses().forEach {
                    addAnnotatedClass(it.java)
                    packages.add(it.java.packageName)
                }
            }

            packages.forEach {
                addPackage(it)
            }
        }

    @Component
    fun metadataSources(
        standardServiceRegistry: StandardServiceRegistry,
        customizers: List<MetadataSourcesCustomizer>
    ): MetadataSources =
        MetadataSources(standardServiceRegistry)
            .apply {
                customizers.forEach { it.customize() }
            }

    @Component(onTerminate = "close")
    fun sessionFactory(metadata: Metadata, customizers: List<SessionFactoryCustomizer>): SessionFactory =
        SessionFactoryImpl(
            metadata
                .sessionFactoryBuilder
                .apply {
                    customizers.forEach { it.customize() }
                }
                .build()
        )
}
