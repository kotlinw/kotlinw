package kotlinw.configuration.core

import kotlinw.util.stdlib.HasPriority
import kotlinw.util.stdlib.Priority

interface ConfigurationPropertySource : HasPriority {

    fun getPropertyValue(key: String): String?
}

interface EnumerableConfigurationPropertySource : ConfigurationPropertySource {

    fun getPropertyKeys(): Set<String>
}

abstract class AbstractConfigurationPropertySource(
    override val priority: Priority = Priority.Normal
) : ConfigurationPropertySource
