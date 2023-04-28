package kotlinw.configuration.core

import kotlinw.util.stdlib.HasPriority

interface ConfigurationPropertyLookup {

    fun getConfigurationPropertyValueOrNull(key: String): String?

    fun filterEnumerableConfigurationProperties(predicate: (key: String) -> Boolean): Map<String, String>
}

fun ConfigurationPropertyLookup.getMatchingEnumerableConfigurationProperties(keyRegex: Regex): Map<String, String> =
    filterEnumerableConfigurationProperties { it.matches(keyRegex) }

class ConfigurationPropertyLookupImpl(
    configurationPropertySources: Iterable<ConfigurationPropertySource>
) : ConfigurationPropertyLookup {

    private val sortedConfigurationPropertyValueSources =
        configurationPropertySources.sortedWith(HasPriority.comparator)

    override fun getConfigurationPropertyValueOrNull(key: String): String? {
        sortedConfigurationPropertyValueSources.forEach {
            val value = it.getPropertyValue(key)
            if (value != null) {
                return value
            }
        }

        return null
    }

    override fun filterEnumerableConfigurationProperties(predicate: (key: String) -> Boolean): Map<String, String> =
        sortedConfigurationPropertyValueSources
            .filterIsInstance<EnumerableConfigurationPropertySource>()
            .flatMap { it.getPropertyKeys() }
            .filter(predicate)
            .associateWith { getConfigurationPropertyValueOrNull(it)!! }
}
