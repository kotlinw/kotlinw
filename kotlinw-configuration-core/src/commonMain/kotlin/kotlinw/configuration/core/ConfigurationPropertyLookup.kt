package kotlinw.configuration.core

import kotlinw.util.stdlib.HasPriority

typealias EncodedConfigurationPropertyValue = String

interface ConfigurationPropertyLookup {

    fun getConfigurationPropertyValueOrNull(key: ConfigurationPropertyKey): EncodedConfigurationPropertyValue?

    fun filterEnumerableConfigurationProperties(predicate: (key: ConfigurationPropertyKey) -> Boolean): Map<ConfigurationPropertyKey, EncodedConfigurationPropertyValue>
}

fun ConfigurationPropertyLookup.getConfigurationPropertyValue(key: ConfigurationPropertyKey): EncodedConfigurationPropertyValue =
    getConfigurationPropertyValueOrNull(key)
        ?: throw ConfigurationException("Configuration property not found: $key")

fun ConfigurationPropertyLookup.getMatchingEnumerableConfigurationProperties(keyRegex: Regex): Map<ConfigurationPropertyKey, EncodedConfigurationPropertyValue> =
    filterEnumerableConfigurationProperties { it.name.matches(keyRegex) }

class ConfigurationPropertyLookupImpl(
    configurationPropertySources: Iterable<ConfigurationPropertySource>
) : ConfigurationPropertyLookup {

    constructor(vararg configurationPropertySources: ConfigurationPropertySource) : this(configurationPropertySources.toList())

    private val sources: List<ConfigurationPropertySource> =
        configurationPropertySources.sortedWith(HasPriority.comparator)

    override fun getConfigurationPropertyValueOrNull(key: ConfigurationPropertyKey): EncodedConfigurationPropertyValue? {
        sources.forEach {
            val value = it.getPropertyValueOrNull(key)
            if (value != null) {
                return value
            }
        }

        return null
    }

    override fun filterEnumerableConfigurationProperties(predicate: (key: ConfigurationPropertyKey) -> Boolean): Map<ConfigurationPropertyKey, EncodedConfigurationPropertyValue> =
        sources
            .filterIsInstance<EnumerableConfigurationPropertySource>()
            .flatMap { it.getPropertyKeys() }
            .filter(predicate)
            .associateWith { getConfigurationPropertyValueOrNull(it)!! }
}
