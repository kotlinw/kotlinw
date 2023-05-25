package kotlinw.module.hibernate.core

import kotlinw.configuration.core.ConfigurationPropertyLookup
import kotlinw.configuration.core.startsWith
import kotlinw.hibernate.api.configuration.PersistentClassProvider
import kotlinw.hibernate.core.schemaexport.HibernateSqlSchemaExporter
import kotlinw.hibernate.core.schemaexport.HibernateSqlSchemaExporterImpl
import kotlinw.koin.core.api.registerShutdownTask
import org.hibernate.SessionFactory
import org.hibernate.boot.Metadata
import org.hibernate.boot.MetadataBuilder
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.SessionFactoryBuilder
import org.hibernate.boot.registry.BootstrapServiceRegistry
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder
import org.hibernate.boot.registry.StandardServiceRegistry
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.AvailableSettings
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.reflect.KClass

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

fun hibernateModule(vararg persistentClasses: KClass<*>) = hibernateModule(persistentClasses.toList())

fun hibernateModule(persistentClasses: List<KClass<*>> = emptyList()) = module {
    single {
        BootstrapServiceRegistryBuilder()
            .apply {
                getAll<BootstrapServiceRegistryCustomizer>().forEach {
                    it.customize()
                }
            }
            .build()
            .registerShutdownTask(this) {
                it?.close()
            }
    }.bind<BootstrapServiceRegistry>()

    single<StandardServiceRegistry> {
        StandardServiceRegistryBuilder(get())
            .apply {
                get<ConfigurationPropertyLookup>()
                    .filterEnumerableConfigurationProperties { it.startsWith("hibernate") }
                    .forEach {
                        applySetting(it.key.name, it.value)
                    }

                // TODO applySetting("hibernate.connection.url", "jdbc:h2:mem:")
                applySetting(AvailableSettings.URL, "jdbc:postgresql://localhost:5432/whocos-gateway2")
                applySetting(AvailableSettings.USER, "whocos")
                applySetting(AvailableSettings.PASS, "whocos")

                getAll<StandardServiceRegistryCustomizer>().forEach {
                    it.customize()
                }
            }
            .build()
            .registerShutdownTask(this) {
                it?.close()
            }
    }.bind<StandardServiceRegistry>()

    single<MetadataSources> {
        MetadataSources(get<StandardServiceRegistry>()).apply {
            persistentClasses.forEach {
                addAnnotatedClass(it.java)
            }

            getAll<PersistentClassProvider>().forEach {
                it.getPersistentClasses().forEach {
                    addAnnotatedClass(it.java)
                }
            }

            getAll<MetadataSourcesCustomizer>().forEach {
                it.customize()
            }
        }
    }.bind<MetadataSources>()

    single<Metadata> {
        get<MetadataSources>()
            .metadataBuilder
            .apply {
                getAll<MetadataCustomizer>().forEach {
                    it.customize()
                }
            }
            .build()
    }.bind<Metadata>()

    single<SessionFactory> {
        get<Metadata>()
            .sessionFactoryBuilder
            .apply {
                getAll<SessionFactoryCustomizer>().forEach {
                    it.customize()
                }
            }
            .build()
            .registerShutdownTask(this) {
                it?.close()
            }
    }.bind<SessionFactory>()

    single { HibernateSqlSchemaExporterImpl(get(), get()) }.bind<HibernateSqlSchemaExporter>()
}
