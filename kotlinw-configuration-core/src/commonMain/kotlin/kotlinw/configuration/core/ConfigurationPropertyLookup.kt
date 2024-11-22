package kotlinw.configuration.core

import kotlinw.logging.api.LoggerFactory
import kotlinw.logging.api.LoggerFactory.Companion.getLogger
import kotlinw.util.stdlib.HasPriority
import kotlinw.util.stdlib.Url
import kotlinw.util.stdlib.sortedByPriority
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import kotlinx.collections.immutable.persistentHashSetOf

typealias EncodedConfigurationPropertyValue = String

interface SnapshotConfigurationPropertyLookup {

    fun getConfigurationPropertyValueOrNull(key: ConfigurationPropertyKey): EncodedConfigurationPropertyValue?
}

interface ConfigurationPropertyLookup : SnapshotConfigurationPropertyLookup {

    suspend fun initialize()

    suspend fun reload()

    fun filterEnumerableConfigurationProperties(predicate: (key: ConfigurationPropertyKey) -> Boolean): Map<ConfigurationPropertyKey, EncodedConfigurationPropertyValue>
}

fun SnapshotConfigurationPropertyLookup.getConfigurationPropertyValueOrNull(key: String): EncodedConfigurationPropertyValue? =
    getConfigurationPropertyValueOrNull(ConfigurationPropertyKey(key))

fun SnapshotConfigurationPropertyLookup.getConfigurationPropertyValue(key: ConfigurationPropertyKey): EncodedConfigurationPropertyValue =
    getConfigurationPropertyValueOrNull(key)
        ?: throw ConfigurationException("Required configuration property not found: $key")

fun SnapshotConfigurationPropertyLookup.getConfigurationPropertyValue(key: String): EncodedConfigurationPropertyValue =
    getConfigurationPropertyValue(ConfigurationPropertyKey(key))

inline fun <reified T> SnapshotConfigurationPropertyLookup.getConfigurationPropertyTypedValueOrNull(key: ConfigurationPropertyKey): T? =
    getConfigurationPropertyValueOrNull(key)?.decode()

inline fun <reified T> SnapshotConfigurationPropertyLookup.getConfigurationPropertyTypedValueOrNull(key: String): T? =
    getConfigurationPropertyTypedValueOrNull(ConfigurationPropertyKey(key))

inline fun <reified T> SnapshotConfigurationPropertyLookup.getConfigurationPropertyTypedValue(key: ConfigurationPropertyKey): T =
    getConfigurationPropertyTypedValueOrNull<T>(key)
        ?: throw ConfigurationException("Required configuration property not found: $key")

inline fun <reified T> SnapshotConfigurationPropertyLookup.getConfigurationPropertyTypedValue(key: String): T =
    getConfigurationPropertyTypedValueOrNull(key)
        ?: throw ConfigurationException("Required configuration property not found: $key")

@PublishedApi
internal inline fun <reified T : Any> String.decode(): T? =
    if (isBlank()) {
        null
    } else {
        try {
            @Suppress("IMPLICIT_CAST_TO_ANY")
            when (T::class) {
                String::class -> this
                Boolean::class -> toBooleanStrict()
                Byte::class -> toByte()
                Short::class -> toShort()
                Int::class -> toInt()
                Long::class -> toLong()
                Float::class -> toFloat()
                Double::class -> toDouble()
                Char::class -> if (length == 1) first() else throw IllegalArgumentException("Single character expected but got: '$this'")
                Url::class -> Url(this)
                else -> throw IllegalArgumentException("Unsupported type: ${T::class}")
            } as T
        } catch (e: Exception) {
            throw IllegalArgumentException("Failed to decode value '$this' as target type ${T::class}.", e)
        }
    }

fun ConfigurationPropertyLookup.getMatchingEnumerableConfigurationProperties(keyRegex: Regex): Map<ConfigurationPropertyKey, EncodedConfigurationPropertyValue> =
    filterEnumerableConfigurationProperties { it.name.matches(keyRegex) }

fun defaultConfigurationPropertyLoggingFilter(configurationPropertyKey: ConfigurationPropertyKey): Boolean =
    configurationPropertyKey.name.let { key ->
        setOf(
            "pass",
            "password",
            "secret",
            "passkey",
            "passcode",
            "pin",
            "access",
            "keycode",
            "passphrase"
        ).any {
            key.lowercase().contains(it)
        }
    }

private class SnapshotConfigurationPropertyLookupImpl(
    private val sources: List<ConfigurationPropertyLookupSource>
) : SnapshotConfigurationPropertyLookup {

    private val _unsafeAccessedProperties = atomic(persistentHashSetOf<ConfigurationPropertyKey>())

    val nonAvailableAccessedProperties by _unsafeAccessedProperties

    // TODO pont ugyanez van kicsit lejjebb
    override fun getConfigurationPropertyValueOrNull(key: ConfigurationPropertyKey): EncodedConfigurationPropertyValue? {
        sources.forEach {
            val value = it.getPropertyValueOrNull(key)
            if (value != null) {
                return value
            }
        }

        _unsafeAccessedProperties.update {
            it.add(key)
        }

        return null
    }
}

class ConfigurationPropertyLookupImpl(
    loggerFactory: LoggerFactory,
    configurationPropertyLookupSources: List<ConfigurationPropertyLookupSource>,
    private val loggingFilter: (ConfigurationPropertyKey) -> Boolean = ::defaultConfigurationPropertyLoggingFilter
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
        logger.info { "Initializing configuration..." }
        doReload(true)
    }

    override suspend fun reload() {
        logger.info { "Reloading configuration..." }
        doReload(false)
    }

    private suspend fun doReload(isInitialization: Boolean) {
        logger.info { "Configuration property sources: " / sources.joinToString() }

        sources.sortedByPriority().forEachIndexed { index, source ->
            val snapshotConfigurationPropertyLookup = SnapshotConfigurationPropertyLookupImpl(sources.subList(0, index))

            if (isInitialization) {
                source.initialize(
                    {
                        println(">>> TODO possible configuration change") // TODO
                    },
                    snapshotConfigurationPropertyLookup
                )

                val nonAvailableAccessedProperties = snapshotConfigurationPropertyLookup.nonAvailableAccessedProperties
                if (nonAvailableAccessedProperties.isNotEmpty()) {
                    if (sources.subList(index + 1, sources.lastIndex)
                            .any { it !is EnumerableConfigurationPropertyLookupSource }
                    ) {
                        logger.warning { "Unsafe configuration: property source " / source / " tried to read the non-available configuration properties $nonAvailableAccessedProperties during initialization and a lower priority non-enumerable property source may provide these properties." }
                    }
                }
            }

            source.reload()
        }

        logger.info {
            "Enumerable configuration properties: " /
                    filterEnumerableConfigurationProperties { true }
                        .mapValues {
                            if (loggingFilter(it.key)) "***" else it.value
                        }
        }
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
