package kotlinw.configuration.core

import kotlinw.util.stdlib.Priority

class ConstantConfigurationPropertySource(
    private val properties: Map<ConfigurationPropertyKey, EncodedConfigurationPropertyValue>,
    override val priority: Priority = Priority.Normal
) : EnumerableConfigurationPropertySource {

    companion object {

        fun of(properties: Map<String, EncodedConfigurationPropertyValue>, priority: Priority = Priority.Normal) =
            ConstantConfigurationPropertySource(properties.mapKeys { ConfigurationPropertyKey(it.key) }, priority)
    }

    override fun getPropertyValueOrNull(key: ConfigurationPropertyKey): EncodedConfigurationPropertyValue? = properties[key]

    override fun getPropertyKeys(): Set<ConfigurationPropertyKey> = properties.keys
}
