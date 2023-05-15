package kotlinw.configuration.core

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.properties.Properties

sealed interface ConfigurationObjectLookup {

    fun <T : Any> getConfigurationObjectOrNull(
        deserializer: DeserializationStrategy<T>,
        prefix: String? = null
    ): T?
}

fun <T : Any> ConfigurationObjectLookup.getConfigurationObject(
    configurationType: DeserializationStrategy<T>,
    prefix: String
): T =
    getConfigurationObjectOrNull(configurationType, prefix)
        ?: throw ConfigurationException("Required configuration of type '$configurationType' not found.")

class ConfigurationObjectLookupImpl(
    private val configurationPropertyLookup: ConfigurationPropertyLookup,
    private val serialFormat: Properties = Properties.Default
) : ConfigurationObjectLookup {

    override fun <T : Any> getConfigurationObjectOrNull(deserializer: DeserializationStrategy<T>, prefix: String?): T? =
        if (prefix == null) {
            configurationPropertyLookup
                .filterEnumerableConfigurationProperties { true }
        } else {
            val finalPrefix = "$prefix."
            val nestedPropertyNameStartIndex = finalPrefix.length
            configurationPropertyLookup
                .filterEnumerableConfigurationProperties { it.startsWith(finalPrefix) }
                .mapKeys {
                    it.key.substring(nestedPropertyNameStartIndex)
                }
        }
            .let {
                if (it.isNotEmpty()) {
                    serialFormat.decodeFromStringMap(deserializer, it)
                } else {
                    null
                }
            }
}
