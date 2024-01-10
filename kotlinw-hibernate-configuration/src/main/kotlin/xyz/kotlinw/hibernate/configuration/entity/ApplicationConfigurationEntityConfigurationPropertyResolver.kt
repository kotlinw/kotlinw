package xyz.kotlinw.hibernate.configuration.entity

import kotlinw.configuration.core.ConfigurationPropertyKey
import kotlinw.configuration.core.EncodedConfigurationPropertyValue
import kotlinw.configuration.core.EnumerableConfigurationPropertyResolver
import kotlinw.hibernate.core.api.runReadOnlyJpaTask
import kotlinw.util.stdlib.collection.emptyImmutableOrderedMap
import kotlinw.util.stdlib.collection.toImmutableMap
import kotlinx.atomicfu.atomic
import org.hibernate.SessionFactory

class ApplicationConfigurationEntityConfigurationPropertyResolver(
    private val sessionFactory: SessionFactory,
    private val applicationConfigurationEntityRepository: ApplicationConfigurationEntityRepository
) : EnumerableConfigurationPropertyResolver {

    private var properties by atomic(emptyImmutableOrderedMap<ConfigurationPropertyKey, EncodedConfigurationPropertyValue>())

    override fun getPropertyKeys(): Set<ConfigurationPropertyKey> = properties.keys

    override suspend fun reload() {
        properties =
            sessionFactory
                .runReadOnlyJpaTask { applicationConfigurationEntityRepository.findAll() }
                .associate {
                    ConfigurationPropertyKey(
                        it.name,
                        "Database entity: ${ApplicationConfigurationEntity::class.simpleName}"
                    ) to it.value
                }
                .toImmutableMap()
    }

    override fun getPropertyValueOrNull(key: ConfigurationPropertyKey): EncodedConfigurationPropertyValue? =
        properties[key]
}
