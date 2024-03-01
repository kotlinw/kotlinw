package kotlinw.configuration.core

import kotlin.reflect.KClass
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerializationException
import kotlinx.serialization.properties.Properties
import kotlinx.serialization.serializer

sealed interface ConfigurationObjectLookup {

    fun <T : Any> getConfigurationObjectOrNull(
        configurationObjectType: KClass<out Any>,
        deserializer: DeserializationStrategy<T>,
        prefix: ConfigurationPropertyKey? = null
    ): T?
}

fun <T : Any> ConfigurationObjectLookup.getConfigurationObjectOrNull(
    configurationObjectType: KClass<out Any>,
    deserializer: DeserializationStrategy<T>,
    prefix: String? = null
): T? =
    getConfigurationObjectOrNull(configurationObjectType, deserializer, prefix?.let { ConfigurationPropertyKey(it) })

fun <T : Any> ConfigurationObjectLookup.getConfigurationObject(
    configurationObjectType: KClass<out Any>,
    configurationType: DeserializationStrategy<T>,
    prefix: ConfigurationPropertyKey? = null
): T =
    getConfigurationObjectOrNull(configurationObjectType, configurationType, prefix)
        ?: throw ConfigurationException("Required configuration object of type '$configurationType' not found (corresponding configuration key prefix: $prefix).")

fun <T : Any> ConfigurationObjectLookup.getConfigurationObject(
    configurationObjectType: KClass<out Any>,
    configurationType: DeserializationStrategy<T>,
    prefix: String? = null
): T =
    getConfigurationObject(configurationObjectType, configurationType, prefix?.let { ConfigurationPropertyKey(it) })

inline fun <reified T : Any> ConfigurationObjectLookup.getConfigurationObject(prefix: String? = null) =
    getConfigurationObject(T::class, serializer<T>(), prefix?.let { ConfigurationPropertyKey(it) })

inline fun <reified T : Any> ConfigurationObjectLookup.getConfigurationObjectOrNull(prefix: String? = null) =
    getConfigurationObjectOrNull(T::class, serializer<T>(), prefix?.let { ConfigurationPropertyKey(it) })

class ConfigurationObjectLookupImpl(
    private val configurationPropertyLookup: ConfigurationPropertyLookup,
    private val explicitConfigurationObjects: List<ExplicitConfigurationObject> = emptyList(),
    private val serialFormat: Properties = Properties.Default
) : ConfigurationObjectLookup {

    init {
        val conflicts = explicitConfigurationObjects.map { it::class }.toSet()
            .associateWith { kClass -> explicitConfigurationObjects.count { it::class == kClass } }
            .filter { it.value > 1 }
        if (conflicts.isNotEmpty()) {
            throw RuntimeException("Conflicting explicit configuration objects: $conflicts")
        }
    }

    override fun <T : Any> getConfigurationObjectOrNull(
        configurationObjectType: KClass<out Any>,
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

                if (it.isNotEmpty()) {
                    explicitConfigurationObjects.firstOrNull { it::class == configurationObjectType }?.also {
                        println("Explicit configuration object $it has been overridden by configuration properties: $propertiesMap") // TODO log
                    }

                    try {
                        serialFormat.decodeFromStringMap(deserializer, propertiesMap)
                    } catch (e: Exception) {
                        throw SerializationException(
                            "Failed to decode object of type $configurationObjectType from properties: $propertiesMap", e
                        )
                    }
                } else {
                    explicitConfigurationObjects.firstOrNull { it::class == configurationObjectType } as T?
                }
            }
}
