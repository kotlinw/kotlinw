package kotlinw.configuration.core

import kotlinw.util.stdlib.Priority

typealias ConfigurationChangeNotifier = () -> Unit

interface ConfigurationPropertyResolver {

    suspend fun initialize(
        configurationChangeNotifier: ConfigurationChangeNotifier,
        snapshotConfigurationPropertyLookup: SnapshotConfigurationPropertyLookup
    ) {
    }

    suspend fun reload()

    fun getPropertyValueOrNull(key: ConfigurationPropertyKey): EncodedConfigurationPropertyValue?
}

interface EnumerableConfigurationPropertyResolver : ConfigurationPropertyResolver {

    fun getPropertyKeys(): Set<ConfigurationPropertyKey>
}

fun <T : ConfigurationPropertyResolver> T.asConfigurationPropertySource(priority: Priority = Priority.Normal) =
    when (this) {
        is EnumerableConfigurationPropertyResolver -> EnumerableConfigurationPropertyLookupSourceImpl(this, priority)
        else -> ConfigurationPropertyLookupSourceImpl(this, priority)
    }
