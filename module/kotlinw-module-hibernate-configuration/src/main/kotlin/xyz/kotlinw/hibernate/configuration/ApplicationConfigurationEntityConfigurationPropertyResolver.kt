package xyz.kotlinw.hibernate.configuration

import kotlinw.configuration.core.ConfigurationChangeNotifier
import kotlinw.configuration.core.ConfigurationPropertyKey
import kotlinw.configuration.core.EncodedConfigurationPropertyValue
import kotlinw.configuration.core.EnumerableConfigurationPropertyResolver
import kotlinw.configuration.core.SnapshotConfigurationPropertyLookup
import kotlinw.configuration.core.getConfigurationPropertyTypedValue
import kotlinw.hibernate.core.api.jdbcTask
import kotlinw.jdbc.util.executeQuery
import kotlinw.jdbc.util.executeSingleResultQuery
import kotlinw.util.stdlib.collection.emptyImmutableHashMap
import kotlinw.util.stdlib.collection.emptyImmutableOrderedMap
import kotlinw.util.stdlib.collection.toImmutableMap
import kotlinw.util.stdlib.executeDirectPostgresqlOperation
import kotlinx.atomicfu.atomic
import org.hibernate.SessionFactory
import xyz.kotlinw.di.api.Component
import xyz.kotlinw.hibernate.configuration.entity.ApplicationConfigurationEntity
import xyz.kotlinw.hibernate.configuration.entity.ApplicationConfigurationEntityRepository
import java.sql.Connection

sealed interface ApplicationConfigurationEntityConfigurationPropertyResolver : EnumerableConfigurationPropertyResolver

internal interface ApplicationConfigurationEntityConfigurationPropertyResolverImplementor :
    ApplicationConfigurationEntityConfigurationPropertyResolver {

    fun setSessionFactory(sessionFactory: SessionFactory)
}

@Component(type = ApplicationConfigurationEntityConfigurationPropertyResolver::class)
class ApplicationConfigurationEntityConfigurationPropertyResolverImpl(
    private val applicationConfigurationEntityRepository: ApplicationConfigurationEntityRepository,
    private val notifier: ApplicationConfigurationEntityChangeNotifier
) : ApplicationConfigurationEntityConfigurationPropertyResolverImplementor {

    private lateinit var configurationChangeNotifier: ConfigurationChangeNotifier

    private lateinit var hibernateConnectionUrl: String

    private lateinit var hibernateConnectionUsername: String

    private lateinit var hibernateConnectionPassword: String

    private val _sessionFactory = atomic<SessionFactory?>(null)

    private var properties by atomic(emptyImmutableOrderedMap<ConfigurationPropertyKey, EncodedConfigurationPropertyValue>())

    override fun setSessionFactory(sessionFactory: SessionFactory) {
        _sessionFactory.value = sessionFactory
        configurationChangeNotifier()
    }

    override suspend fun initialize(
        configurationChangeNotifier: ConfigurationChangeNotifier,
        snapshotConfigurationPropertyLookup: SnapshotConfigurationPropertyLookup
    ) {
        this.configurationChangeNotifier = configurationChangeNotifier

        this.hibernateConnectionUrl =
            snapshotConfigurationPropertyLookup.getConfigurationPropertyTypedValue<String>("hibernate.connection.url")
        this.hibernateConnectionUsername =
            snapshotConfigurationPropertyLookup.getConfigurationPropertyTypedValue<String>("hibernate.connection.username")
        this.hibernateConnectionPassword =
            snapshotConfigurationPropertyLookup.getConfigurationPropertyTypedValue<String>("hibernate.connection.password")

        check(notifier is ApplicationConfigurationEntityChangeNotifierImplementor)
        notifier.addListener {
            configurationChangeNotifier()
        }
    }

    override fun getPropertyKeys(): Set<ConfigurationPropertyKey> = properties.keys

    override suspend fun reload() {
        val sessionFactory = _sessionFactory.value
        properties =
            if (sessionFactory != null) {
                TODO("a jelek szerint ez mindig null lesz")
//                if (
//                    sessionFactory.runJpaTask {
//                        jdbcTask { checkTableExists() }
//                    }
//                ) {
//                    sessionFactory
//                        .runJpaTask { applicationConfigurationEntityRepository.findAll() }
//                        .associate {
//                            ConfigurationPropertyKey(
//                                it.name,
//                                "Database entity: ${ApplicationConfigurationEntity::class.simpleName}"
//                            ) to it.value
//                        }
//                        .toImmutableMap()
//                } else {
//                    emptyImmutableHashMap()
//                }
            } else {
                executeDirectPostgresqlOperation(
                    hibernateConnectionUrl,
                    hibernateConnectionUsername,
                    hibernateConnectionPassword
                ) {
                    if (checkTableExists()) {
                        executeQuery<Pair<String, String>>("SELECT name, value FROM ${ApplicationConfigurationEntity.TableName}") {
                            it.getString(1) to it.getString(2)
                        }
                            .associate {
                                ConfigurationPropertyKey(
                                    it.first,
                                    "Database entity: ${ApplicationConfigurationEntity::class.simpleName}"
                                ) to it.second
                            }
                            .toImmutableMap()
                    } else {
                        emptyImmutableHashMap()
                    }
                }
            }
    }

    private fun Connection.checkTableExists() =
        executeSingleResultQuery("SELECT EXISTS (SELECT 1 FROM information_schema.tables WHERE LOWER(table_name)=LOWER('${ApplicationConfigurationEntity.TableName}'))") {
            getBoolean(1)
        } == true

    override fun getPropertyValueOrNull(key: ConfigurationPropertyKey): EncodedConfigurationPropertyValue? =
        properties[key]
}
