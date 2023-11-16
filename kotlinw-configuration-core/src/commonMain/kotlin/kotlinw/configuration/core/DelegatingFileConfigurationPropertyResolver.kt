package kotlinw.configuration.core

import arrow.core.continuations.AtomicRef
import kotlinw.eventbus.local.LocalEventBus
import kotlinw.util.stdlib.concurrent.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlinx.io.buffered
import kotlinx.io.readString
import xyz.kotlinw.io.Resource
import xyz.kotlinw.io.readUtf8String

class DelegatingFileConfigurationPropertyResolver private constructor(
    private val resource: Resource,
    private val delegateFactory: (String) -> EnumerableConfigurationPropertyResolver,
    private val watcherCoroutineScope: CoroutineScope?,
    private val eventBus: LocalEventBus?,
    private val watchDelay: Duration?,
    @Suppress("UNUSED_PARAMETER") primaryConstructorMarker: Unit
) : EnumerableConfigurationPropertyResolver {

    constructor(
        resource: Resource,
        delegateFactory: (String) -> EnumerableConfigurationPropertyResolver
    ) :
            this(resource, delegateFactory, null, null, null, Unit)

    constructor(
        resource: Resource,
        delegateFactory: (String) -> EnumerableConfigurationPropertyResolver,
        watcherCoroutineScope: CoroutineScope,
        eventBus: LocalEventBus,
        watchDelay: Duration = Duration.INFINITE
    ) :
            this(resource, delegateFactory, watcherCoroutineScope, eventBus, watchDelay, Unit)

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

    @OptIn(ExperimentalStdlibApi::class)
    private suspend fun tryReadConfiguration(): String? =
        try {
            resource.readUtf8String()
        } catch (e: Exception) {
            // TODO log
            null
        }

    override fun getPropertyKeys() = delegate?.getPropertyKeys() ?: emptySet()

    override fun getPropertyValueOrNull(key: ConfigurationPropertyKey) = delegate?.getPropertyValueOrNull(key)
}
