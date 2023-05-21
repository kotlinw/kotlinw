package kotlinw.configuration.core

import kotlinw.util.stdlib.HasPriority

typealias EncodedConfigurationPropertyValue = String

interface ConfigurationPropertyLookup {

    fun getConfigurationPropertyValueOrNull(key: ConfigurationPropertyKey): EncodedConfigurationPropertyValue?

    fun filterEnumerableConfigurationProperties(predicate: (key: ConfigurationPropertyKey) -> Boolean): Map<ConfigurationPropertyKey, EncodedConfigurationPropertyValue>
}

fun ConfigurationPropertyLookup.getConfigurationPropertyValue(key: ConfigurationPropertyKey): EncodedConfigurationPropertyValue =
    getConfigurationPropertyValueOrNull(key)
        ?: throw ConfigurationException("Configuration property not found: $key")

inline fun <reified T> ConfigurationPropertyLookup.getConfigurationPropertyTypedValue(key: ConfigurationPropertyKey): T? =
    getConfigurationPropertyValueOrNull(key)?.decode()

@PublishedApi
internal inline fun <reified T> String.decode(): T? =
    if (isBlank()) {
        null
    } else {
        try {
            when (T::class) {
                Boolean::class -> toBooleanStrict()
                Byte::class -> toByte()
                Short::class -> toShort()
                Int::class -> toInt()
                Long::class -> toLong()
                Float::class -> toFloat()
                Double::class -> toDouble()
                Char::class -> if (length == 1) first() else throw IllegalArgumentException("Single character expected but got: '$this'")
                else -> throw IllegalArgumentException("Unsupported type: ${T::class}")
            } as T
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to decode value '$this' as target type ${T::class}.")
        }
    }

fun ConfigurationPropertyLookup.getMatchingEnumerableConfigurationProperties(keyRegex: Regex): Map<ConfigurationPropertyKey, EncodedConfigurationPropertyValue> =
    filterEnumerableConfigurationProperties { it.name.matches(keyRegex) }

class ConfigurationPropertyLookupImpl(
    configurationPropertyLookupSources: Iterable<ConfigurationPropertyLookupSource>
) : ConfigurationPropertyLookup {

    constructor(vararg configurationPropertyLookupSources: ConfigurationPropertyLookupSource) : this(configurationPropertyLookupSources.toList())

    private val sources: List<ConfigurationPropertyLookupSource> =
        configurationPropertyLookupSources.sortedWith(HasPriority.comparator)

    override fun getConfigurationPropertyValueOrNull(key: ConfigurationPropertyKey): EncodedConfigurationPropertyValue? {
        sources.forEach {
            val value = it.getPropertyValueOrNull(key)
            if (value != null) {
                return value
            }
        }

        return null
    }

    override fun filterEnumerableConfigurationProperties(predicate: (key: ConfigurationPropertyKey) -> Boolean): Map<ConfigurationPropertyKey, EncodedConfigurationPropertyValue> =
        sources
            .filterIsInstance<EnumerableConfigurationPropertyLookupSource>()
            .flatMap { it.getPropertyKeys() }
            .filter(predicate)
            .associateWith { getConfigurationPropertyValueOrNull(it)!! }
}
