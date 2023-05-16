package kotlinw.configuration.core

import kotlinw.util.stdlib.HasPriority

typealias ConfigurationPropertyValue = Any

interface ConfigurationPropertyLookup {

    fun getConfigurationPropertyValueOrNull(key: ConfigurationPropertyKey): ConfigurationPropertyValue?

    fun filterEnumerableConfigurationProperties(predicate: (key: ConfigurationPropertyKey) -> Boolean): Map<ConfigurationPropertyKey, ConfigurationPropertyValue>
}

fun ConfigurationPropertyLookup.getConfigurationPropertyValue(key: ConfigurationPropertyKey): ConfigurationPropertyValue =
    getConfigurationPropertyValueOrNull(key)
        ?: throw ConfigurationException("Configuration property not found: $key")

fun ConfigurationPropertyLookup.getMatchingEnumerableConfigurationProperties(keyRegex: Regex): Map<ConfigurationPropertyKey, ConfigurationPropertyValue> =
    filterEnumerableConfigurationProperties { it.name.matches(keyRegex) }

class ConfigurationPropertyLookupImpl(
    configurationPropertySources: Iterable<ConfigurationPropertySource>
) : ConfigurationPropertyLookup {

    private val sources: List<ConfigurationPropertySource> =
        configurationPropertySources.sortedWith(HasPriority.comparator)

    override fun getConfigurationPropertyValueOrNull(key: ConfigurationPropertyKey): ConfigurationPropertyValue? {
        sources.forEach {
            val value = it.getPropertyValue(key)
            if (value != null) {
                return value
            }
        }

        return null
    }

    override fun filterEnumerableConfigurationProperties(predicate: (key: ConfigurationPropertyKey) -> Boolean): Map<ConfigurationPropertyKey, ConfigurationPropertyValue> =
        sources
            .filterIsInstance<EnumerableConfigurationPropertySource>()
            .flatMap { it.getPropertyKeys() }
            .filter(predicate)
            .associateWith { getConfigurationPropertyValueOrNull(it)!! }
}
