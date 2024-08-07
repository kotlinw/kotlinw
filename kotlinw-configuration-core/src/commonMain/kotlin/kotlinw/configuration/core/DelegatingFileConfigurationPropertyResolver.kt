package kotlinw.configuration.core

import arrow.atomic.Atomic
import arrow.atomic.value
import arrow.core.continuations.AtomicRef
import xyz.kotlinw.eventbus.inprocess.InProcessEventBus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration
import xyz.kotlinw.io.Resource
import xyz.kotlinw.io.readUtf8String

class DelegatingFileConfigurationPropertyResolver private constructor(
    private val resource: Resource,
    private val delegateFactory: (String) -> EnumerableConfigurationPropertyResolver,
    private val watcherCoroutineScope: CoroutineScope?,
    private val eventBus: InProcessEventBus<in ConfigurationEvent>?,
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
        eventBus: InProcessEventBus<in ConfigurationEvent>,
        watchDelay: Duration = Duration.INFINITE
    ) :
            this(resource, delegateFactory, watcherCoroutineScope, eventBus, watchDelay, Unit)

    private val delegateHolder = Atomic<EnumerableConfigurationPropertyResolver?>(null)

    private val delegate get() = delegateHolder.value

    override suspend fun reload() {
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

                        eventBus.publish(ConfigurationPropertySourceChangeEvent)
                    }
                }
            }
        }
    }

    private suspend fun tryReadConfiguration(): String? =
        try {
            resource.readUtf8String()
        } catch (e: Exception) {
            // FIXME log
            null
        }

    override fun getPropertyKeys() = delegate?.getPropertyKeys() ?: emptySet()

    override fun getPropertyValueOrNull(key: ConfigurationPropertyKey) = delegate?.getPropertyValueOrNull(key)

    override fun toString(): String {
        return "DelegatingFileConfigurationPropertyResolver(resource=$resource, watchDelay=$watchDelay)"
    }
}
