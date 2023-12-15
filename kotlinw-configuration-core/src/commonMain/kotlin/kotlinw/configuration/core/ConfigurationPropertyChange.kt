package kotlinw.configuration.core

import arrow.core.continuations.AtomicRef
import xyz.kotlinw.eventbus.inprocess.InProcessEventBus
import xyz.kotlinw.eventbus.inprocess.asyncOn
import kotlinw.util.stdlib.collection.filterNotNullValues
import kotlinw.util.stdlib.concurrent.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import xyz.kotlinw.eventbus.inprocess.on

suspend fun ConfigurationPropertyLookup.pollEnumerableConfigurationProperties(
    pollingDelay: Duration,
    configurationPropertyNamePredicate: (key: ConfigurationPropertyKey) -> Boolean
) =
    pollConfigurationPropertiesImpl(pollingDelay) {
        resolveEnumerableConfigurationProperties(configurationPropertyNamePredicate)
    }

private fun ConfigurationPropertyLookup.resolveEnumerableConfigurationProperties(configurationPropertyNamePredicate: (key: ConfigurationPropertyKey) -> Boolean) =
    filterEnumerableConfigurationProperties(configurationPropertyNamePredicate)

suspend fun ConfigurationPropertyLookup.pollConfigurationProperties(
    pollingDelay: Duration,
    propertyNames: Set<ConfigurationPropertyKey>
) =
    pollConfigurationPropertiesImpl(pollingDelay) {
        resolveConfigurationPropertiesByName(propertyNames)
    }

private fun ConfigurationPropertyLookup.resolveConfigurationPropertiesByName(propertyNames: Set<ConfigurationPropertyKey>) =
    propertyNames
        .associateWith { getConfigurationPropertyValueOrNull(it) }
        .filterNotNullValues()

private suspend fun pollConfigurationPropertiesImpl(
    pollingDelay: Duration,
    eventListenerCoroutineScope: CoroutineScope? = null,
    eventBus: InProcessEventBus? = null,
    resolveConfigurationProperties: suspend () -> Map<ConfigurationPropertyKey, EncodedConfigurationPropertyValue>,
) =
    flow {
        val initialValues = resolveConfigurationProperties()
        emit(initialValues)

        val previousValues = AtomicRef(initialValues)
        val updateLock = Mutex()

        suspend fun refresh() {
            updateLock.withLock {
                val currentValues = resolveConfigurationProperties()
                if (currentValues != previousValues.value) {
                    emit(currentValues)
                    previousValues.value = currentValues
                }
            }
        }

        suspend fun pollingLoop() {
            while (true) {
                delay(pollingDelay)
                refresh()
            }
        }

        if (eventListenerCoroutineScope != null && eventBus != null) {
            eventListenerCoroutineScope.launch {
                eventBus.on<ConfigurationPropertySourceChangeEvent> {
                    refresh()
                }
            }
        }

        pollingLoop()
    }
        .distinctUntilChanged()

object ConfigurationPropertySourceChangeEvent

data class ConfigurationPropertyChangeEvent(
    val name: ConfigurationPropertyKey,
    val newValue: EncodedConfigurationPropertyValue
)

suspend fun ConfigurationPropertyLookup.watchEnumerableConfigurationProperties(
    eventListenerCoroutineScope: CoroutineScope,
    eventBus: InProcessEventBus,
    pollingDelay: Duration,
    configurationPropertyNamePredicate: (key: ConfigurationPropertyKey) -> Boolean
) =
    watchConfigurationPropertiesImpl(
        eventListenerCoroutineScope,
        eventBus,
        pollingDelay,
        configurationPropertyNamePredicate
    ) {
        resolveEnumerableConfigurationProperties(configurationPropertyNamePredicate)
    }

suspend fun ConfigurationPropertyLookup.watchConfigurationProperties(
    eventListenerCoroutineScope: CoroutineScope,
    eventBus: InProcessEventBus,
    pollingDelay: Duration,
    propertyNames: Set<ConfigurationPropertyKey>
) =
    watchConfigurationPropertiesImpl(
        eventListenerCoroutineScope,
        eventBus,
        pollingDelay,
        configurationPropertyNamePredicate = { propertyNames.contains(it) },
        resolveConfigurationProperties = { resolveConfigurationPropertiesByName(propertyNames) }
    )

private suspend fun watchConfigurationPropertiesImpl(
    eventListenerCoroutineScope: CoroutineScope,
    eventBus: InProcessEventBus,
    pollingDelay: Duration,
    configurationPropertyNamePredicate: (key: ConfigurationPropertyKey) -> Boolean,
    resolveConfigurationProperties: suspend () -> Map<ConfigurationPropertyKey, EncodedConfigurationPropertyValue>
): Flow<Map<ConfigurationPropertyKey, EncodedConfigurationPropertyValue>> {
    val pollingFlow =
        pollConfigurationPropertiesImpl(
            pollingDelay,
            eventListenerCoroutineScope,
            eventBus,
            resolveConfigurationProperties
        )

    val watcherFlow = channelFlow {
        send(emptyMap())

        eventBus.asyncOn<ConfigurationPropertyChangeEvent>(eventListenerCoroutineScope) {
            if (configurationPropertyNamePredicate(it.name)) {
                send(mapOf(it.name to it.newValue))
            }
        }
            .join()
    }
        .distinctUntilChanged()

    return pollingFlow.combine(watcherFlow) { pollerMap, watcherMap ->
        pollerMap.toMutableMap().apply { putAll(watcherMap) }
    }
}
