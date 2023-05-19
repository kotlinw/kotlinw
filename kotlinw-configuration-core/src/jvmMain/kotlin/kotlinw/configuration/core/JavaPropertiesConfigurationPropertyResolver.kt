package kotlinw.configuration.core

import java.util.Properties

class JavaPropertiesConfigurationPropertyResolver(
    private val properties: Properties,
    sourceInfo: String? = null
) : EnumerableConfigurationPropertyResolver {

    override fun getPropertyKeys(): Set<ConfigurationPropertyKey> =
        properties.keys.map { ConfigurationPropertyKey(it.toString()) }.toSet()

    override fun getPropertyValueOrNull(key: ConfigurationPropertyKey): String? =
        properties.getProperty(key.name)
}
