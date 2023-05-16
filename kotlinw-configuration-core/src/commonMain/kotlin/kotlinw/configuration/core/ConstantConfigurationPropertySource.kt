package kotlinw.configuration.core

import kotlinw.util.stdlib.Priority

class ConstantConfigurationPropertySource(
    private val properties: Map<ConfigurationPropertyKey, ConfigurationPropertyValue>,
    override val priority: Priority = Priority.Normal
) : EnumerableConfigurationPropertySource {

    companion object {

        fun of(properties: Map<String, ConfigurationPropertyValue>, priority: Priority = Priority.Normal) =
            ConstantConfigurationPropertySource(properties.mapKeys { ConfigurationPropertyKey(it.key) }, priority)
    }

    override fun getPropertyValue(key: ConfigurationPropertyKey): ConfigurationPropertyValue? = properties[key]

    override fun getPropertyKeys(): Set<ConfigurationPropertyKey> = properties.keys
}
