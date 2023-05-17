package kotlinw.configuration.core

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.properties.Properties

sealed interface ConfigurationObjectLookup {

    fun <T : Any> getConfigurationObjectOrNull(
        deserializer: DeserializationStrategy<T>,
        prefix: ConfigurationPropertyKey? = null
    ): T?
}

fun <T : Any> ConfigurationObjectLookup.getConfigurationObjectOrNull(
    deserializer: DeserializationStrategy<T>,
    prefix: String? = null
): T? =
    getConfigurationObjectOrNull(deserializer, prefix?.let { ConfigurationPropertyKey(it) })

fun <T : Any> ConfigurationObjectLookup.getConfigurationObject(
    configurationType: DeserializationStrategy<T>,
    prefix: ConfigurationPropertyKey? = null
): T =
    getConfigurationObjectOrNull(configurationType, prefix)
        ?: throw ConfigurationException("Required configuration object of type '$configurationType' not found (corresponding configuration key prefix: $prefix).")

fun <T : Any> ConfigurationObjectLookup.getConfigurationObject(
    configurationType: DeserializationStrategy<T>,
    prefix: String? = null
): T =
    getConfigurationObject(configurationType, prefix?.let { ConfigurationPropertyKey(it) })

class ConfigurationObjectLookupImpl(
    private val configurationPropertyLookup: ConfigurationPropertyLookup,
    private val serialFormat: Properties = Properties.Default
) : ConfigurationObjectLookup {

    override fun <T : Any> getConfigurationObjectOrNull(
        deserializer: DeserializationStrategy<T>,
        prefix: ConfigurationPropertyKey?
    ): T? =
        if (prefix == null) {
            configurationPropertyLookup
                .filterEnumerableConfigurationProperties { true }
        } else {
            configurationPropertyLookup
                .filterEnumerableConfigurationProperties { it.startsWith(prefix) }
                .mapKeys {
                    it.key.subKeyAfterPrefix(prefix)
                }
        }
            .let {
                val propertiesMap =
                    it
                        .mapKeys { it.key.name }
                        .mapValues { it.value.toString() }

                if (it.isNotEmpty()) {
                    serialFormat.decodeFromStringMap(deserializer, propertiesMap)
                } else {
                    null
                }
            }
}
