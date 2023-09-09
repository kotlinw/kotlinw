package kotlinw.module.hibernate.core

import jakarta.persistence.EntityGraph
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceUnitUtil
import jakarta.persistence.Query
import jakarta.persistence.SynchronizationType
import jakarta.persistence.metamodel.Metamodel
import javax.naming.Reference
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
import kotlinx.atomicfu.atomic
import org.hibernate.Cache
import org.hibernate.Session
import org.hibernate.SessionBuilder
import org.hibernate.StatelessSession
import org.hibernate.StatelessSessionBuilder
import org.hibernate.boot.spi.SessionFactoryOptions
import org.hibernate.engine.spi.FilterDefinition
import org.hibernate.graph.RootGraph
import org.hibernate.query.criteria.HibernateCriteriaBuilder
import org.hibernate.relational.SchemaManager
import org.hibernate.stat.Statistics
import java.sql.Connection

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
        single<BootstrapServiceRegistry> {
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
        }

        single<StandardServiceRegistry> {
            StandardServiceRegistryBuilder(get())
                .apply {
                    get<ConfigurationPropertyLookup>()
                        .filterEnumerableConfigurationProperties { it.startsWith("hibernate") }
                        .forEach {
                            applySetting(it.key.name, it.value)
                        }

                    getAll<StandardServiceRegistryCustomizer>().forEach {
                        it.customize()
                    }
                }
                .build()
                .registerShutdownTask(this) {
                    it?.close()
                }
        }

        single<MetadataSources> {
            MetadataSources(get<StandardServiceRegistry>()).apply {
                getAll<PersistentClassProvider>().forEach {
                    it.getPersistentClasses().forEach {
                        addAnnotatedClass(it.java)
                    }
                }

                getAll<MetadataSourcesCustomizer>().forEach {
                    it.customize()
                }
            }
        }

        single<Metadata> {
            get<MetadataSources>()
                .metadataBuilder
                .apply {
                    getAll<MetadataCustomizer>().forEach {
                        it.customize()
                    }
                }
                .build()
        }

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
        }

        single<HibernateSqlSchemaExporter> { HibernateSqlSchemaExporterImpl(get(), get()) }
    }
}
