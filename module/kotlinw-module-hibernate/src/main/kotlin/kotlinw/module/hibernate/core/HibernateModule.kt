package kotlinw.module.hibernate.core

import kotlinw.configuration.core.ConfigurationPropertyLookup
import kotlinw.configuration.core.startsWith
import kotlinw.hibernate.api.configuration.PersistentClassProvider
import kotlinw.hibernate.core.schemaexport.HibernateSqlSchemaExporter
import kotlinw.hibernate.core.schemaexport.HibernateSqlSchemaExporterImpl
import kotlinw.hibernate.core.schemaupgrade.DatabaseUpgradeManager
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.logging.platform.PlatformLogging
import kotlinw.module.api.ApplicationInitializerService
import kotlinw.module.core.api.coreJvmModule
import kotlinw.util.stdlib.Priority
import kotlinw.util.stdlib.Priority.Companion.higherBy
import xyz.kotlinw.koin.container.registerShutdownTask
import org.hibernate.SessionFactory
import org.hibernate.boot.Metadata
import org.hibernate.boot.MetadataBuilder
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.SessionFactoryBuilder
import org.hibernate.boot.registry.BootstrapServiceRegistry
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder
import org.hibernate.boot.registry.StandardServiceRegistry
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.koin.core.qualifier.named
import org.koin.dsl.bind
import org.koin.dsl.module
import xyz.kotlinw.koin.container.registerStartupTask

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

val hibernateModule by lazy {
    module {
        single<ApplicationInitializerService>(named("HibernateInitializerService")) {
            ApplicationInitializerService(Priority.Normal.higherBy(200)) {
                get<BootstrapServiceRegistryProxy>().initialize(
                    BootstrapServiceRegistryBuilder()
                        .apply {
                            getAll<BootstrapServiceRegistryCustomizer>().forEach {
                                it.customize()
                            }
                        }
                        .build()
                )
                get<StandardServiceRegistryProxy>().initialize(
                    StandardServiceRegistryBuilder(get())
                        .apply {
                            get<ConfigurationPropertyLookup>()
                                .filterEnumerableConfigurationProperties { it.startsWith("hibernate") }
                                .also {
                                    PlatformLogging.getLogger().info { "Hibernate configuration properties: " / it }
                                }
                                .forEach {
                                    applySetting(it.key.name, it.value)
                                }

                            getAll<StandardServiceRegistryCustomizer>().forEach {
                                it.customize()
                            }
                        }
                        .build()
                )
                get<MetadataSources>().apply {
                    getAll<PersistentClassProvider>().forEach {
                        it.getPersistentClasses().forEach {
                            addAnnotatedClass(it.java)
                        }
                    }

                    getAll<MetadataSourcesCustomizer>().forEach {
                        it.customize()
                    }
                }
                get<MetadataProxy>().initialize(
                    get<MetadataSources>()
                        .metadataBuilder
                        .apply {
                            getAll<MetadataCustomizer>().forEach {
                                it.customize()
                            }
                        }
                        .build()
                )
                get<SessionFactoryProxy>().initialize(
                    get<Metadata>()
                        .sessionFactoryBuilder
                        .apply {
                            getAll<SessionFactoryCustomizer>().forEach {
                                it.customize()
                            }
                        }
                        .build()
                )
            }
        }

        single<BootstrapServiceRegistryProxy> {
            BootstrapServiceRegistryProxy()
                .registerShutdownTask(this) {
                    it.close()
                }
        }.bind(BootstrapServiceRegistry::class)

        single<StandardServiceRegistryProxy> {
            StandardServiceRegistryProxy()
                .registerShutdownTask(this) {
                    it.close()
                }
        }.bind(StandardServiceRegistry::class)

        single<MetadataSources> {
            MetadataSources(get<StandardServiceRegistry>())
        }

        single<MetadataProxy> {
            MetadataProxy()
        }.bind(Metadata::class)

        single<SessionFactoryProxy> {
            SessionFactoryProxy()
                .registerShutdownTask(this) {
                    it.close()
                }
        }.bind(SessionFactory::class)

        single<HibernateSqlSchemaExporter> { HibernateSqlSchemaExporterImpl(get(), get()) }
    }
}
