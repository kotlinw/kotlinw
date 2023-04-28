package kotlinw.configuration.core

import kotlinw.configuration.core.ConfigurationObjectResolver.ResolutionContext
import kotlinw.util.stdlib.HasPriority
import kotlin.reflect.KClass

interface ConfigurationObjectService {

    fun <T : Any> getConfigurationObjectInstances(configurationType: KClass<T>): List<T>
}

inline fun <reified T : Any> ConfigurationObjectService.getConfigurationObjectInstances(): List<T> =
    getConfigurationObjectInstances(T::class)

fun <T : Any> ConfigurationObjectService.getOptionalSingletonConfigurationObject(configurationType: KClass<T>): T? {
    val configurationInstances = getConfigurationObjectInstances(configurationType)
    return when {
        configurationInstances.size == 1 -> configurationInstances.first()
        configurationInstances.isEmpty() -> null
        else -> throw ConfigurationException("Configuration of type '$configurationType' expected to be a singleton but multiple instances are found: $configurationInstances")
    }
}

inline fun <reified T : Any> ConfigurationObjectService.getOptionalSingletonConfigurationObject(): T? =
    getOptionalSingletonConfigurationObject(T::class)

fun <T : Any> ConfigurationObjectService.getSingletonConfigurationObject(configurationType: KClass<T>): T =
    getOptionalSingletonConfigurationObject(configurationType)
        ?: throw ConfigurationException("Required configuration of type '$configurationType' not found.")

inline fun <reified T : Any> ConfigurationObjectService.getSingletonConfigurationObject(): T =
    getSingletonConfigurationObject(T::class)

class ConfigurationObjectServiceImpl(
    val configurationPropertyLookup: ConfigurationPropertyLookup,
    val configurationPropertyValueResolver: ConfigurationPropertyValueResolver,
    configurationProviders: List<ConfigurationObjectResolver<*>>
) : ConfigurationObjectService {

    private inner class ResolutionContextImpl : ResolutionContext {

        override val configurationPropertyLookup: ConfigurationPropertyLookup
            get() = this@ConfigurationObjectServiceImpl.configurationPropertyLookup

        override val configurationPropertyValueResolver: ConfigurationPropertyValueResolver
            get() = this@ConfigurationObjectServiceImpl.configurationPropertyValueResolver
    }

    private val sortedConfigurationProviders = configurationProviders.sortedWith(HasPriority.comparator)

    override fun <T : Any> getConfigurationObjectInstances(configurationType: KClass<T>): List<T> =
        sortedConfigurationProviders
            .filter { it.supports(configurationType) }
            .map { it.resolveConfigurationInstances(ResolutionContextImpl(), configurationType) }
            .flatMap { it as List<T> }
}
