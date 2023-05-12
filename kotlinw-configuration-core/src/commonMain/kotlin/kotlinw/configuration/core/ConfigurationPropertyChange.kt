package kotlinw.configuration.core

import kotlinw.eventbus.local.LocalEventBus
import kotlinw.eventbus.local.on
import kotlinw.util.stdlib.collection.filterNotNullValues
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlin.time.Duration

suspend fun ConfigurationPropertyLookup.pollEnumerableConfigurationProperties(
    pollingDelay: Duration,
    configurationPropertyNamePredicate: (key: String) -> Boolean
) =
    pollConfigurationPropertiesImpl(pollingDelay) {
        resolveEnumerableConfigurationProperties(configurationPropertyNamePredicate)
    }

private fun ConfigurationPropertyLookup.resolveEnumerableConfigurationProperties(configurationPropertyNamePredicate: (key: String) -> Boolean) =
    filterEnumerableConfigurationProperties(configurationPropertyNamePredicate)

suspend fun ConfigurationPropertyLookup.pollConfigurationProperties(
    pollingDelay: Duration,
    propertyNames: Set<String>
) =
    pollConfigurationPropertiesImpl(pollingDelay) {
        resolveConfigurationPropertiesByName(propertyNames)
    }

private fun ConfigurationPropertyLookup.resolveConfigurationPropertiesByName(propertyNames: Set<String>) =
    propertyNames
        .associateWith { getConfigurationPropertyValueOrNull(it) }
        .filterNotNullValues()

private suspend fun pollConfigurationPropertiesImpl(
    pollingDelay: Duration,
    resolveConfigurationProperties: suspend () -> Map<String, String>
) =
    flow {
        val initialValues = resolveConfigurationProperties()
        emit(initialValues)

        var previousValues = initialValues
        while (true) {
            delay(pollingDelay)

            val currentValues = resolveConfigurationProperties()
            if (currentValues != previousValues) {
                emit(currentValues)
                previousValues = currentValues
            }
        }
    }

data class ConfigurationPropertyChangeEvent(
    val name: String,
    val newValue: String
)

suspend fun ConfigurationPropertyLookup.watchEnumerableConfigurationProperties(
    eventBus: LocalEventBus,
    pollingDelay: Duration,
    configurationPropertyNamePredicate: (key: String) -> Boolean
) =
    watchConfigurationPropertiesImpl(
        eventBus,
        pollingDelay,
        configurationPropertyNamePredicate
    ) {
        resolveEnumerableConfigurationProperties(configurationPropertyNamePredicate)
    }

suspend fun ConfigurationPropertyLookup.watchConfigurationProperties(
    eventBus: LocalEventBus,
    pollingDelay: Duration,
    propertyNames: Set<String>
) =
    watchConfigurationPropertiesImpl(
        eventBus,
        pollingDelay,
        configurationPropertyNamePredicate = { propertyNames.contains(it) },
        resolveConfigurationProperties = { resolveConfigurationPropertiesByName(propertyNames) }
    )

private suspend fun watchConfigurationPropertiesImpl(
    eventBus: LocalEventBus,
    pollingDelay: Duration,
    configurationPropertyNamePredicate: (key: String) -> Boolean,
    resolveConfigurationProperties: suspend () -> Map<String, String>
): Flow<Map<String, String>> {
    val pollingFlow = pollConfigurationPropertiesImpl(pollingDelay, resolveConfigurationProperties)

    val watcherFlow = channelFlow {
        send(emptyMap())

        eventBus.on<ConfigurationPropertyChangeEvent> {
            if (configurationPropertyNamePredicate(it.name)) {
                send(mapOf(it.name to it.newValue))
            }
        }
    }

    return pollingFlow.combine(watcherFlow) { pollerMap, watcherMap ->
        pollerMap.toMutableMap().apply { putAll(watcherMap) }
    }
}
