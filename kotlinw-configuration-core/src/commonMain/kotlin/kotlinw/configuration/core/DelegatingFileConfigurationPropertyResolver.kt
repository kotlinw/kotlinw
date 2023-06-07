package kotlinw.configuration.core

import arrow.core.continuations.AtomicRef
import kotlinw.eventbus.local.LocalEventBus
import kotlinw.util.stdlib.concurrent.value
import kotlinw.util.stdlib.io.FileLocation
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration

class DelegatingFileConfigurationPropertyResolver private constructor(
    private val fileLocation: FileLocation,
    private val delegateFactory: (String) -> EnumerableConfigurationPropertyResolver,
    private val watcherCoroutineScope: CoroutineScope?,
    private val eventBus: LocalEventBus?,
    private val watchDelay: Duration?,
    @Suppress("UNUSED_PARAMETER") primaryConstructorMarker: Unit
) : EnumerableConfigurationPropertyResolver {

    constructor(
        fileLocation: FileLocation,
        delegateFactory: (String) -> EnumerableConfigurationPropertyResolver
    ) :
            this(fileLocation, delegateFactory, null, null, null, Unit)

    constructor(
        fileLocation: FileLocation,
        delegateFactory: (String) -> EnumerableConfigurationPropertyResolver,
        watcherCoroutineScope: CoroutineScope,
        eventBus: LocalEventBus,
        watchDelay: Duration = Duration.INFINITE
    ) :
            this(fileLocation, delegateFactory, watcherCoroutineScope, eventBus, watchDelay, Unit)

    private val delegateHolder = AtomicRef<EnumerableConfigurationPropertyResolver?>(null)

    private val delegate get() = delegateHolder.value

    override suspend fun initialize() {
        require(watchDelay == null || watchDelay > Duration.ZERO)

        val initialProperties = tryReadConfiguration()
        if (initialProperties != null) {
            delegateHolder.value = delegateFactory(initialProperties)
        }

        if (watcherCoroutineScope != null && eventBus != null && watchDelay != null) {
            watcherCoroutineScope.launch {
                var previousPropertiesDefinition = initialProperties

                while (true) {
                    delay(watchDelay)

                    val currentPropertiesDefinition = tryReadConfiguration()
                    if (currentPropertiesDefinition != null && currentPropertiesDefinition != previousPropertiesDefinition) {
                        previousPropertiesDefinition = currentPropertiesDefinition
                        delegateHolder.value = delegateFactory(currentPropertiesDefinition)

                        eventBus.dispatch(ConfigurationPropertySourceChangeEvent)
                    }
                }
            }
        }
    }

    private suspend fun tryReadConfiguration(): String? =
        try {
            fileLocation.readString()
        } catch (e: Exception) {
            // TODO log
            null
        }

    override fun getPropertyKeys() = delegate?.getPropertyKeys() ?: emptySet()

    override fun getPropertyValueOrNull(key: ConfigurationPropertyKey) = delegate?.getPropertyValueOrNull(key)
}
