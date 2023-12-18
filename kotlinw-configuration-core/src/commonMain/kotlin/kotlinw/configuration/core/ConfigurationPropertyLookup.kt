package kotlinw.configuration.core

import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.util.stdlib.HasPriority

typealias EncodedConfigurationPropertyValue = String

interface ConfigurationPropertyLookup {

    suspend fun initialize() // TODO internal API

    fun getConfigurationPropertyValueOrNull(key: ConfigurationPropertyKey): EncodedConfigurationPropertyValue?

    fun filterEnumerableConfigurationProperties(predicate: (key: ConfigurationPropertyKey) -> Boolean): Map<ConfigurationPropertyKey, EncodedConfigurationPropertyValue>
}

fun ConfigurationPropertyLookup.getConfigurationPropertyValueOrNull(key: String): EncodedConfigurationPropertyValue? =
    getConfigurationPropertyValueOrNull(ConfigurationPropertyKey(key))

fun ConfigurationPropertyLookup.getConfigurationPropertyValue(key: ConfigurationPropertyKey): EncodedConfigurationPropertyValue =
    getConfigurationPropertyValueOrNull(key)
        ?: throw ConfigurationException("Required configuration property not found: $key")

fun ConfigurationPropertyLookup.getConfigurationPropertyValue(key: String): EncodedConfigurationPropertyValue =
    getConfigurationPropertyValue(ConfigurationPropertyKey(key))

inline fun <reified T> ConfigurationPropertyLookup.getConfigurationPropertyTypedValueOrNull(key: ConfigurationPropertyKey): T? =
    getConfigurationPropertyValueOrNull(key)?.decode()

inline fun <reified T> ConfigurationPropertyLookup.getConfigurationPropertyTypedValueOrNull(key: String): T? =
    getConfigurationPropertyTypedValueOrNull(ConfigurationPropertyKey(key))

inline fun <reified T> ConfigurationPropertyLookup.getConfigurationPropertyTypedValue(key: ConfigurationPropertyKey): T =
    getConfigurationPropertyTypedValueOrNull<T>(key)
        ?: throw ConfigurationException("Required configuration property not found: $key")

inline fun <reified T> ConfigurationPropertyLookup.getConfigurationPropertyTypedValue(key: String): T =
    getConfigurationPropertyTypedValueOrNull(key)
        ?: throw ConfigurationException("Required configuration property not found: $key")

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
    loggerFactory: LoggerFactory,
    configurationPropertyLookupSources: List<ConfigurationPropertyLookupSource>
) : ConfigurationPropertyLookup {

    private val logger = loggerFactory.getLogger()

    constructor(
        loggerFactory: LoggerFactory,
        vararg configurationPropertyLookupSources: ConfigurationPropertyLookupSource
    ) :
            this(loggerFactory, configurationPropertyLookupSources.toList())

    private val sources: List<ConfigurationPropertyLookupSource> =
        configurationPropertyLookupSources.sortedWith(HasPriority.comparator)

    override suspend fun initialize() {
        logger.info { "Configuration property sources: " / sources.joinToString() }
        sources.forEach {
            it.initialize()
        }
        logger.info { "Enumerable configuration properties: " / filterEnumerableConfigurationProperties { true } } // FIXME a jelszavak ne legyenek logolva
    }

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
