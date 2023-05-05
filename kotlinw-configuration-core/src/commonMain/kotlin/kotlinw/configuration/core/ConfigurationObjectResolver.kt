package kotlinw.configuration.core

import kotlinw.util.stdlib.HasPriority
import kotlinw.util.stdlib.Priority
import kotlin.reflect.KClass

interface ConfigurationObjectResolver<T> : HasPriority {

    interface ResolutionContext {

        val configurationPropertyLookup: ConfigurationPropertyLookup

        val configurationPropertyValueConverter: ConfigurationPropertyValueConverter
    }

    fun supports(configurationType: KClass<*>): Boolean

    // TODO context(ResolutionContext)
    fun resolveConfigurationInstances(
        context: ResolutionContext,
        configurationType: KClass<*>
    ): List<T>
}

abstract class AbstractSimpleConfigurationObjectResolver<T : Any>(
    private val supportedConfigurationType: KClass<T>,
    override val priority: Priority = Priority.Normal
) : ConfigurationObjectResolver<T> {

    override fun supports(configurationType: KClass<*>): Boolean =
        configurationType == supportedConfigurationType
}
