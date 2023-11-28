package kotlinw.module.hibernate.core

import kotlin.reflect.KClass
import kotlinw.configuration.core.ConfigurationPropertyLookup
import kotlinw.configuration.core.startsWith
import kotlinw.hibernate.api.configuration.PersistentClassProvider
import kotlinw.hibernate.core.schemaexport.HibernateSqlSchemaExporter
import kotlinw.hibernate.core.schemaexport.HibernateSqlSchemaExporterImpl
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import org.hibernate.SessionFactory
import org.hibernate.boot.Metadata
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.BootstrapServiceRegistry
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder
import org.hibernate.boot.registry.StandardServiceRegistry
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.di.api.Module

@Module
class HibernateModuleNew {

    @Component
    fun bootstrapServiceRegistry(customizers: List<BootstrapServiceRegistryCustomizer>): BootstrapServiceRegistry =
        BootstrapServiceRegistryBuilder()
            .apply {
                customizers.forEach { it.customize() }
            }
            .build()

    @Component
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
    fun persistentClassRegistrator(persistentClassProviders: List<PersistentClassProvider>) =
        MetadataSourcesCustomizer {
            persistentClassProviders.forEach {
                it.getPersistentClasses().forEach {
                    addAnnotatedClass(it.java)
                }
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

    @Component
    fun sessionFactory(metadata: Metadata, customizers: List<SessionFactoryCustomizer>): SessionFactory =
        metadata
            .sessionFactoryBuilder
            .apply {
                customizers.forEach { it.customize() }
            }
            .build()

    @Component
    fun hibernateSqlSchemaExporter(
        standardServiceRegistry: StandardServiceRegistry,
        metadata: Metadata
    ): HibernateSqlSchemaExporter = HibernateSqlSchemaExporterImpl(standardServiceRegistry, metadata)
}
