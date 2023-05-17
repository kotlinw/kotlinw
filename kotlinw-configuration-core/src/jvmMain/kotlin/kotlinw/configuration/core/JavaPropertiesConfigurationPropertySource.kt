package kotlinw.configuration.core

import kotlinw.util.stdlib.Priority
import java.util.Properties

class JavaPropertiesConfigurationPropertySource(
    private val properties: Properties,
    override val priority: Priority = Priority.Normal
) : EnumerableConfigurationPropertySource {

    override fun getPropertyKeys(): Set<ConfigurationPropertyKey> =
        properties.keys.map { ConfigurationPropertyKey(it.toString()) }.toSet()

    override fun getPropertyValueOrNull(key: ConfigurationPropertyKey): String? =
        properties.getProperty(key.name)
}
